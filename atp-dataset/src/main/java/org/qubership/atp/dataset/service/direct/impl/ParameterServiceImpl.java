/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 */

package org.qubership.atp.dataset.service.direct.impl;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.qubership.atp.crypt.exception.AtpEncryptException;
import org.qubership.atp.dataset.constants.CacheEnum;
import org.qubership.atp.dataset.db.ParameterRepository;
import org.qubership.atp.dataset.db.dto.ParameterDataDto;
import org.qubership.atp.dataset.db.utils.Proxies;
import org.qubership.atp.dataset.exception.attribute.AttributeNotFoundException;
import org.qubership.atp.dataset.exception.attribute.AttributeTypeException;
import org.qubership.atp.dataset.exception.file.FileDsUploadException;
import org.qubership.atp.dataset.exception.parameter.ParameterOverlapLevelDownException;
import org.qubership.atp.dataset.exception.parameter.ParameterValueEncryptException;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributePath;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.ParameterOverlap;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.model.impl.TableResponse;
import org.qubership.atp.dataset.model.impl.file.FileData;
import org.qubership.atp.dataset.service.direct.AliasWrapperService;
import org.qubership.atp.dataset.service.direct.AttributeService;
import org.qubership.atp.dataset.service.direct.DataSetService;
import org.qubership.atp.dataset.service.direct.DateAuditorService;
import org.qubership.atp.dataset.service.direct.EncryptionService;
import org.qubership.atp.dataset.service.direct.GridFsService;
import org.qubership.atp.dataset.service.direct.ParameterService;
import org.qubership.atp.dataset.versioning.service.DataSetListSnapshotService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@Service
public class ParameterServiceImpl implements ParameterService {

    private static final String EMPTY_STRING = "";
    private static final int OVERLAP_MAX_LEVELS = 15;

    private final javax.inject.Provider<DataSetService> dsServiceProvider;
    private final DateAuditorService dateAuditorService;
    private final ParameterRepository repo;
    private final AliasWrapperService aliasWrapperService;
    private final AttributeService attributeService;
    private final GridFsService gridFsService;
    private final DataSetListSnapshotService dataSetListSnapshotService;
    private final EncryptionService encryptionService;
    private final ClearCacheServiceImpl clearCacheService;

    @Nonnull
    private ParameterDataDto extractData(@Nonnull Parameter parameter, UUID newParentId) {
        UUID listValueId = parameter.getListValue() == null ? null : extractListValueData(parameter, newParentId);
        UUID dsRef = parameter.getDataSetReference() == null ? null : parameter.getDataSetReference().getId();
        return prepareData(parameter.getAttribute().getId(), parameter.getDataSet().getId(),
                parameter.getText(), listValueId, dsRef, parameter.getAttribute().getType() == AttributeType.FILE);
    }

    private UUID extractListValueData(@Nonnull Parameter parameter, UUID newParentId) {
        if (newParentId == null) {
            return parameter.getListValue().getId();
        } else {
            List<ListValue> newListValues = attributeService.get(newParentId).getListValues();
            String oldValue = parameter.getListValue().getName();
            ListValue newValue = newListValues.stream()
                    .filter(value -> value.getName().equals(oldValue)).findFirst().get();
            return newValue.getId();
        }
    }

    /**
     * Create parameter.
     */
    @Transactional
    @Nonnull
    public Parameter create(@Nonnull UUID dsId, @Nonnull UUID attrId,
                            @Nullable String text, @Nullable UUID listValueReference, @Nullable UUID referenceDataSet) {
        Parameter parameter = repo.create(dsId, attrId, prepareData(attrId, dsId, text, listValueReference,
                referenceDataSet, false));
        DataSetList dataSetList = parameter.getDataSet().getDataSetList();
        dateAuditorService.updateModifiedFields(dataSetList.getId());
        dataSetListSnapshotService.commitEntity(dataSetList.getId());
        return parameter;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Transactional
    @CacheEvict(value = CacheEnum.Constants.DATASET_LIST_CONTEXT_CACHE, key = "#dsId")
    public Parameter setParamSelectJavers(@NotNull UUID dsId, @NotNull UUID targetAttrId,
                                          @Nullable List<UUID> attrPathIds,
                                          @Nullable String stringValue,
                                          @Nullable UUID dsRef,
                                          @Nullable UUID listValueRef,
                                          boolean isJavers) {
        return setParameter(dsId, targetAttrId, attrPathIds, stringValue, dsRef, listValueRef, isJavers);
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Transactional
    public Parameter set(@Nonnull UUID dsId,
                         @Nonnull UUID targetAttrId,
                         @Nullable List<UUID> attrPathIds,
                         @Nullable String stringValue,
                         @Nullable UUID dsRef,
                         @Nullable UUID listValueRef) {
        clearCacheService.evictDatasetListContextCache(dsId);
        return setParameter(dsId, targetAttrId, attrPathIds, stringValue, dsRef, listValueRef, true);
    }

    @Nonnull
    @Override
    public Parameter set(@Nonnull UUID dsId, @Nonnull UUID targetAttrId,
                         @Nonnull String value, List<UUID> attrPathIds) {
        String stringValue = null;
        UUID dsRef = null;
        UUID listValueRef = null;

        Attribute attr = attributeService.get(targetAttrId);
        if (attr == null) {
            log.error("No such attribute");
            throw new AttributeNotFoundException();
        }

        AttributeType type = attr.getType();
        switch (type) {
            case CHANGE:
            case ENCRYPTED:
            case TEXT:
                stringValue = value;
                break;
            case LIST:
                listValueRef = UUID.fromString(value);
                break;
            case DSL:
                dsRef = UUID.fromString(value);
                break;
            default:
                String error = "Unknown attribute type " + type;
                log.error(error);
                throw new AttributeTypeException(attr.getName(), type.toString());
        }
        return set(dsId, targetAttrId, attrPathIds, stringValue, dsRef, listValueRef);
    }

    @NotNull
    private Parameter setParameter(@NotNull UUID dsId, @NotNull UUID targetAttrId,
                                   @Nullable List<UUID> attrPathIds,
                                   @Nullable String stringValue,
                                   @Nullable UUID dsRef,
                                   @Nullable UUID listValueRef,
                                   @NotNull Boolean isJavers) {
        checkDsLocked(targetAttrId, dsId);
        ParameterDataDto data = prepareData(targetAttrId, dsId, stringValue, listValueRef, dsRef, false);
        Parameter target;
        if (!ObjectUtils.anyNotNull(stringValue, dsRef, listValueRef)) {
            isJavers = false;
        }

        if (attrPathIds == null || attrPathIds.isEmpty()) {
            target = repo.getByDataSetIdAttributeId(dsId, targetAttrId);
            if (target != null) {
                log.debug("Updating parameter {} in ds {}", target.getId(), dsId);
                Parameter parameter = repo.update(target, data);
                dateAuditorService.updateModifiedFields(parameter.getDataSet().getDataSetList().getId());
                if (isJavers) {
                    log.debug("Javers activated");
                    dataSetListSnapshotService.commitEntity(parameter.getDataSet().getDataSetList().getId());
                }
                return parameter;
            }
            log.debug("Creating parameter of attribute {} in ds {}", targetAttrId, dsId);
            Parameter parameter = repo.create(dsId, targetAttrId, data);
            dateAuditorService.updateModifiedFields(parameter.getDataSet().getDataSetList().getId());
            if (isJavers) {
                log.debug("Javers activated");
                dataSetListSnapshotService.commitEntity(parameter.getDataSet().getDataSetList().getId());
            }
            return parameter;
        }
        if (attrPathIds.size() > OVERLAP_MAX_LEVELS) {
            String message = String.format(
                    "You cannot overlap parameters deeper than %s levels down", OVERLAP_MAX_LEVELS);
            log.error(message);
            throw new ParameterOverlapLevelDownException(message);
        }
        //is overlap
        DataSet ds = dsServiceProvider.get().get(dsId);
        Preconditions.checkArgument(ds != null, "No data set found by id: %s", dsId);
        UUID dslId = ds.getDataSetList().getId();
        target = repo.getOverlap(dslId, dsId, targetAttrId, attrPathIds);
        if (target != null) {
            log.debug("Updating overlap parameter {} with path {}, in ds {}", target.getId(), attrPathIds, dsId);
            Parameter parameter = repo.update(target, data);
            dateAuditorService.updateModifiedFields(parameter.getDataSet().getDataSetList().getId());
            if (isJavers) {
                log.debug("Javers activated");
                dataSetListSnapshotService.commitEntity(parameter.getDataSet().getDataSetList().getId());
            }
            return parameter;
        }
        log.debug("Creating overlap parameter with path {}, in ds {}", attrPathIds, dsId);
        ParameterOverlap parameterOverlap = repo.overlap(dslId, dsId, targetAttrId, data, attrPathIds);
        dateAuditorService.updateModifiedFields(parameterOverlap.getDataSet().getDataSetList().getId());
        if (isJavers) {
            log.debug("Javers activated");
            dataSetListSnapshotService.commitEntity(parameterOverlap.getDataSet().getDataSetList().getId());
        }
        return parameterOverlap;
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    public boolean bulkUpdateValue(@Nullable String stringValue,
                                   @Nullable UUID dsRef,
                                   @Nullable UUID listValueRef,
                                   @Nonnull List<UUID> parameterIds) {
        ParameterDataDto data = new ParameterDataDto(stringValue, dsRef, listValueRef);
        boolean isUpdated = repo.update(parameterIds, data);
        if (isUpdated) {
            parameterIds
                    .stream()
                    .map(parameterId -> repo.getById(parameterId))
                    .filter(Objects::nonNull)
                    .map(Parameter::getDataSet)
                    .collect(Collectors.groupingBy(DataSet::getDataSetList))
                    .keySet()
                    .forEach(dataSetList -> {
                        dateAuditorService.updateModifiedFields(dataSetList.getId());
                        dataSetListSnapshotService.commitEntity(dataSetList.getId());
                    });
        }
        return isUpdated;
    }

    @Override
    public FileData upload(UUID datasetId, UUID attributeId, List<UUID> attrPathIds, String contentType,
                           String fileName, InputStream file) {
        Parameter parameter;
        if (isEmpty(attrPathIds)) {
            parameter = getOrCreateByDataSetIdAttributeId(datasetId, attributeId);
        } else {
            parameter = getOrCreateOverlap(datasetId, attributeId, attrPathIds);
        }
        Preconditions.checkNotNull(parameter.getId(), "Could not get parameters id");
        return upload(parameter.getId(), contentType, fileName, file);
    }

    @Override
    public FileData upload(UUID parameterUuid, String contentType, String fileName, InputStream file) {
        FileData fileData = new FileData(fileName, parameterUuid, contentType);
        gridFsService.save(fileData, file, false);
        return fileData;
    }

    @Override
    public List<FileData> bulkUploadAttachment(UUID dataSetListId, List<UUID> dataSetsIds, UUID attributeId,
                                               MultipartFile file, List<UUID> attrPathIds) {

        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();

        List<FileData> parametersToUpdate =
                getParametersToUpdate(attributeId, dataSetsIds, attrPathIds, fileName, contentType);

        log.info("bulkUpload of file parameters for attribute {} and datasets {}", attributeId, dataSetsIds);
        try {
            gridFsService.saveAll(parametersToUpdate, file);
        } catch (IOException e) {
            log.error("Cannot upload file {}", fileName, e);
            throw new FileDsUploadException(fileName);
        }
        dateAuditorService.updateModifiedFields(dataSetListId);
        return parametersToUpdate;
    }

    private List<FileData> getParametersToUpdate(UUID attributeId, List<UUID> dataSetsIds,
                                                 List<UUID> attrPathIds, String fileName, String contentType) {
        List<FileData> parametersToUpdate = new ArrayList<>();

        Function<UUID, Function<UUID, Function<ParameterService, Parameter>>> getOrCreate;
        if (isEmpty(attrPathIds)) {
            getOrCreate = ds -> attr -> service -> service.getOrCreateByDataSetIdAttributeId(ds, attr);
        } else {
            getOrCreate = ds -> attr -> service -> service.getOrCreateOverlap(ds, attr, attrPathIds);
        }

        dataSetsIds.forEach(
                dsId -> parametersToUpdate.add(prepareParams(dsId, attributeId, fileName, contentType, getOrCreate)));

        return parametersToUpdate;
    }

    private FileData prepareParams(UUID dsId,
                                   UUID attributeId,
                                   String fileName,
                                   String contentType,
                                   Function<UUID, Function<UUID, Function<ParameterService, Parameter>>> getOrCreate) {
        Parameter parameter = getOrCreate.apply(dsId).apply(attributeId).apply(this);
        Preconditions.checkNotNull(parameter, "Could not get parameter");
        return new FileData(fileName, parameter.getId(), contentType);
    }

    @Override
    public List<Object> bulkUpdate(UUID dataSetListId, List<UUID> attrPathIds, List<UUID> dataSetsIds, UUID attributeId,
                                   String value, MultipartFile file) {
        if (isEmpty(dataSetsIds)) {
            dataSetsIds = getListOfDataSetByDataSetListId(dataSetListId);
        }
        removeForSkipIdsDsLocked(dataSetListId, dataSetsIds);
        log.info("bulkUpdate of parameters for attribute {} and datasets {}", attributeId, dataSetsIds);

        ArrayList<Object> updatedObjects = new ArrayList<>();
        if (isFile(attributeId)) {
            if (file == null) {
                updatedObjects.addAll(bulkClearAttachment(dataSetsIds, attributeId, attrPathIds, dataSetListId));
            } else {
                updatedObjects.addAll(bulkUploadAttachment(dataSetListId, dataSetsIds, attributeId, file, attrPathIds));
            }
        } else {
            value = StringUtils.trimToNull(value);
            if (value == null) {
                updatedObjects.addAll(bulkClearParameter(dataSetsIds, attributeId, dataSetListId, attrPathIds));
            } else {
                updatedObjects.addAll(bulkSetParameter(dataSetsIds, attributeId, value, attrPathIds));
            }
        }
        return updatedObjects;
    }

    private void removeForSkipIdsDsLocked(UUID dataSetListId, List<UUID> dataSetsIds) {
        List<UUID> filteredDsLocked = dataSetsIds.stream()
                .filter(dsId -> {
                    DataSet dataSet = dsServiceProvider.get().get(dsId);
                    return Objects.isNull(dataSet) || dataSet.isLocked();
                }).collect(Collectors.toList());
        dataSetsIds.removeAll(filteredDsLocked);
        log.info("Remove for skip locked datasets id:{} from datasetList id {}", filteredDsLocked, dataSetListId);
    }

    private boolean isFile(UUID attributeId) {
        Attribute attr = attributeService.get(attributeId);
        return attr != null && AttributeType.FILE == attr.getType();
    }

    private List<Parameter> bulkSetParameter(List<UUID> dataSetsIds, UUID attributeId, String value,
                                             List<UUID> attrPathIds) {
        List<Parameter> updatedObjects = new ArrayList<>();
        dataSetsIds.forEach(dsId -> updatedObjects.add(set(dsId, attributeId, value, attrPathIds)));
        return updatedObjects;
    }

    private List<Parameter> bulkClearParameter(List<UUID> dataSetsIds, UUID attributeId, UUID datasetListId,
                                               List<UUID> attrPathIds) {
        List<Parameter> updatedObjects = new ArrayList<>();
        dataSetsIds.forEach(dsId -> delete(attributeId, dsId, datasetListId, attrPathIds));
        return updatedObjects;
    }

    private List<FileData> bulkClearAttachment(List<UUID> dataSetsIds, UUID attributeId,
                                               List<UUID> attrPathIds, UUID dataSetListId) {
        List<FileData> parametersToUpdate =
                getParametersToUpdate(attributeId, dataSetsIds, attrPathIds, null, null);
        parametersToUpdate.forEach(fileData -> clearAttachment(fileData.getParameterUuid()));
        dateAuditorService.updateModifiedFields(dataSetListId);
        return parametersToUpdate;
    }

    @Override
    public void clearAttachment(UUID parameterId) {
        gridFsService.delete(parameterId);
    }

    private List<UUID> getListOfDataSetByDataSetListId(UUID dataSetListId) {
        return dsServiceProvider.get().getByParentId(dataSetListId, false, null)
                .map(DataSet::getId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean update(@Nonnull UUID parameterId, String text) {
        Parameter parameter = repo.getById(parameterId);

        if (parameter == null) {
            return false;
        }
        UUID attributeId = parameter.getAttribute().getId();
        UUID dsId = parameter.getDataSet().getId();
        return repo.update(parameterId, prepareData(attributeId, dsId, text, null, null,
                parameter.getAttribute().getType() == AttributeType.FILE));
    }

    @Nullable
    public Parameter get(@Nonnull UUID id) {
        return repo.getById(id);
    }

    @Override
    public boolean existsById(@NotNull UUID id) {
        return repo.existsById(id);
    }

    @Nonnull
    public List<Parameter> getAll() {
        return repo.getAll();
    }

    @Transactional
    public boolean delete(@Nonnull Parameter parameter) {
        return repo.delete(parameter);
    }

    @Transactional
    @Override
    public boolean delete(@Nonnull UUID attributeId, @Nonnull UUID dataSetId, UUID datasetListId,
                          List<UUID> attrPathIds) {
        clearCacheService.evictDatasetListContextCache(dataSetId);
        return deleteParameter(attributeId, dataSetId, datasetListId, attrPathIds, true);
    }

    private boolean deleteParameter(@Nonnull UUID attributeId, @Nonnull UUID dataSetId, UUID datasetListId,
                                    List<UUID> attrPathIds, boolean isJavers) {
        checkDsLocked(attributeId, dataSetId);
        Parameter parameter = repo.getByDataSetIdAttributeId(dataSetId, attributeId);
        if (parameter == null) {
            parameter = repo.getOverlap(datasetListId, dataSetId, attributeId, attrPathIds);
        }
        if (parameter == null) {
            return false;
        } else {
            DataSetList dataSetList = parameter.getDataSet().getDataSetList();
            repo.delete(parameter);
            dateAuditorService.updateModifiedFields(dataSetList.getId());
            if (isJavers) {
                log.debug("Javers activated");
                dataSetListSnapshotService.commitEntity(dataSetList.getId());
            }
            return true;
        }
    }

    /**
     * Delete parameter without snapshot.
     */
    @Transactional
    @Override
    public void deleteParamSelectJavers(@Nonnull UUID attributeId, @Nonnull UUID dataSetId, UUID datasetListId,
                                        List<UUID> attrPathIds, boolean isJavers) {
        deleteParameter(attributeId, dataSetId, datasetListId, attrPathIds, isJavers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Parameter copy(@Nonnull DataSet newParentDs, @Nonnull Parameter parameter,
                          @Nullable Map<UUID, UUID> attributes) {
        UUID parentId = parameter.getAttribute().getId();
        UUID newParentId = attributes == null ? null : attributes.get(parentId);
        if (newParentId == null) {
            newParentId = parentId;
        }
        UUID dsId = newParentDs.getId();
        ParameterDataDto data = extractData(parameter, newParentId);
        Parameter result = null;
        if (!parameter.isOverlap()) {
            result = repo.create(dsId, newParentId, data);
        } else {
            List<UUID> attrPathIds = parameter.asOverlap().getAttributePath().getPath().stream()
                    .map((attribute) -> {
                        UUID oldId = attribute.getId();
                        UUID newId = null;
                        if (attributes != null) {
                            newId = attributes.get(oldId);
                        }
                        return newId == null ? oldId : newId;
                    })
                    .collect(Collectors.toList());
            UUID dslId = newParentDs.getDataSetList().getId();
            result = repo.overlap(dslId, dsId, newParentId, data, attrPathIds);
        }
        if (data.isFile()) {
            copyFile(parameter, result);
        }
        if (result != null) {
            newParentDs.getParameters().add(result);
        }
        return result;
    }

    private void copyFile(Parameter parameter, Parameter result) {
        if (gridFsService.get(parameter.getId()).isPresent()) {
            gridFsService.copy(parameter.getId(), result.getId(), false);
        }
    }

    private ParameterDataDto prepareData(@Nonnull UUID attrId,
                                         @Nonnull UUID dsId,
                                         @Nullable String text,
                                         @Nullable UUID listValueReference,
                                         @Nullable UUID referenceDataSet, boolean isFile) {
        if (text != null) {
            Attribute attribute = attributeService.get(attrId);
            if (attribute == null) {
                log.error(String.format("Attribute %s not found", attrId));
                throw new AttributeNotFoundException();
            }
            if (AttributeType.ENCRYPTED.equals(attribute.getType())
                    && !Strings.isNullOrEmpty(text.replace("\u0000", ""))
                    && !encryptionService.isEncrypted(text)) {
                try {
                    text = encryptionService.decodeBase64(text);
                    text = encryptionService.encrypt(text);
                } catch (AtpEncryptException e) {
                    log.error("Failed to encrypt parameter {}", attrId);
                    throw new ParameterValueEncryptException();
                }
            }
            text = wrapToAlias(text, attrId);
            text = convertToEmptyIfNeed(text);
        }
        return new ParameterDataDto(text, referenceDataSet, listValueReference, isFile);
    }

    private String wrapToAlias(String text, UUID attributeId) {
        DataSetList dsl = Proxies.base(DataSetList.class, () -> {
            Attribute attribute = attributeService.get(attributeId);
            Preconditions.checkNotNull(attribute, "Attribute not found by Id: %s", attributeId);
            return attribute.getDataSetList();
        });
        VisibilityArea va = Proxies.base(VisibilityArea.class, dsl::getVisibilityArea);
        return aliasWrapperService.wrapToAlias(text, va, dsl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public Parameter getByDataSetIdAttributeId(UUID dataSetId, UUID attrId) {
        return repo.getByDataSetIdAttributeId(dataSetId, attrId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public List<Parameter> getByAttributeIdAndDatasetIds(@Nonnull UUID attributeId, @Nonnull Set<UUID> datasetIds) {
        return repo.getByAttributeIdAndDatasetIds(attributeId, datasetIds);
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public Parameter getOrCreateByDataSetIdAttributeId(UUID dataSetId, UUID attrId) {
        Parameter parameter = getByDataSetIdAttributeId(dataSetId, attrId);
        if (parameter == null) {
            parameter = create(dataSetId, attrId, null, null, null);
        }
        return parameter;
    }

    @Nonnull
    @Override
    public Parameter getOrCreateOverlap(UUID dsId, UUID targetAttrId, List<UUID> attrPathIds) {
        DataSet ds = dsServiceProvider.get().get(dsId);
        Preconditions.checkArgument(ds != null, "No data set found by id: %s", dsId);
        UUID dslId = ds.getDataSetList().getId();
        Parameter target = repo.getOverlap(dslId, dsId, targetAttrId, attrPathIds);
        if (target == null) {
            target = repo.overlap(dslId, dsId, targetAttrId,
                    new ParameterDataDto(null, null, null, true), attrPathIds);
        }
        return target;
    }

    private String convertToEmptyIfNeed(String text) {
        if (!Strings.isNullOrEmpty(text) && text.charAt(0) == 0) {
            return EMPTY_STRING;
        }
        return text;
    }

    @Nullable
    @Override
    public ParameterOverlap getOverlap(@Nonnull UUID dsId, @Nonnull UUID attributeId,
                                       @Nonnull List<UUID> attributePathIds) {
        DataSet ds = dsServiceProvider.get().get(dsId);
        Preconditions.checkArgument(ds != null, "No data set found by id: %s", dsId);
        UUID dslId = ds.getDataSetList().getId();
        return repo.getOverlap(dslId, dsId, attributeId, attributePathIds);
    }

    @Override
    public Stream<ParameterOverlap> getOverlaps(@Nonnull UUID targetAttributeId,
                                                @Nonnull Predicate<AttributePath> filter) {
        return repo.getOverlaps(targetAttributeId, filter);
    }

    @Override
    public List<?> getParametersAffectedByListValue(UUID listValueId, boolean withDsl) {
        if (withDsl) {
            return repo.findAllByListValueIdWithDataSetList(listValueId);
        } else {
            return repo.findAllByListValueId(listValueId);
        }
    }

    @Override
    public List<TableResponse> getParametersAffectedByListValues(List<UUID> listValueIds) {
        return repo.findAllByListValueIdsWithDataSetList(listValueIds);
    }

    private void checkDsLocked(@NotNull UUID attributeId, @NotNull UUID dataSetId) {
        DataSet dataSet = dsServiceProvider.get().get(dataSetId);
        Preconditions.checkArgument(!(Objects.isNull(dataSet) || dataSet.isLocked()),
                "Can not change parameter with attribute id: %s because dataset id locked: %s",
                attributeId, dataSetId);
    }
}
