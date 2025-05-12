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

package org.qubership.atp.dataset.ei;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.qubership.atp.dataset.model.AttributeType.ENCRYPTED;
import static org.qubership.atp.dataset.model.AttributeType.FILE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.qubership.atp.dataset.antlr4.TextParameterParser;
import org.qubership.atp.dataset.db.GridFsRepository;
import org.qubership.atp.dataset.db.jpa.entities.AbstractAttributeEntity;
import org.qubership.atp.dataset.db.jpa.entities.AttributeEntity;
import org.qubership.atp.dataset.db.jpa.entities.AttributeKeyEntity;
import org.qubership.atp.dataset.db.jpa.entities.DataSetEntity;
import org.qubership.atp.dataset.db.jpa.entities.DataSetListEntity;
import org.qubership.atp.dataset.db.jpa.entities.ListValueEntity;
import org.qubership.atp.dataset.db.jpa.entities.ParameterEntity;
import org.qubership.atp.dataset.db.jpa.repositories.JpaDataSetListRepository;
import org.qubership.atp.dataset.db.jpa.repositories.JpaDataSetRepository;
import org.qubership.atp.dataset.ei.model.DataSet;
import org.qubership.atp.dataset.ei.model.DataSetAttribute;
import org.qubership.atp.dataset.ei.model.DataSetAttributeKey;
import org.qubership.atp.dataset.ei.model.DataSetList;
import org.qubership.atp.dataset.ei.model.DataSetListValue;
import org.qubership.atp.dataset.ei.model.DataSetParameter;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.impl.ei.postman.ExportPostmanDataset;
import org.qubership.atp.dataset.model.impl.ei.postman.ExportPostmanParameter;
import org.qubership.atp.dataset.model.impl.file.FileData;
import org.qubership.atp.dataset.service.jpa.JpaDataSetListService;
import org.qubership.atp.dataset.service.jpa.delegates.AttributeKey;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;
import org.qubership.atp.dataset.service.jpa.impl.DataSetListContextService;
import org.qubership.atp.dataset.service.jpa.impl.DataSetParameterProvider;
import org.qubership.atp.dataset.service.jpa.impl.JpaDataSetServiceImpl;
import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.service.jpa.model.MacroContextService;
import org.qubership.atp.dataset.service.jpa.model.dscontext.DataSetContext;
import org.qubership.atp.dataset.service.jpa.model.dscontext.DataSetListContext;
import org.qubership.atp.dataset.service.jpa.model.dscontext.GroupContext;
import org.qubership.atp.dataset.service.jpa.model.dscontext.ParameterContext;
import org.qubership.atp.dataset.service.jpa.model.tree.params.AbstractParameter;
import org.qubership.atp.dataset.service.jpa.model.tree.params.AbstractTextParameter;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.ParameterPositionContext;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.RefDslMacro;
import org.qubership.atp.ei.node.ExportExecutor;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ExportScope;
import org.qubership.atp.ei.node.exceptions.ExportException;
import org.qubership.atp.ei.node.services.FileService;
import org.qubership.atp.ei.node.services.ObjectSaverToDiskService;
import org.qubership.atp.ei.ntt.impl.NttProjectConverter;
import org.qubership.atp.macros.core.calculator.MacrosCalculator;
import org.qubership.atp.macros.core.client.MacrosFeignClient;
import org.qubership.atp.macros.core.clients.api.dto.macros.MacrosDto;
import org.qubership.atp.macros.core.converter.MacrosDtoConvertService;
import org.qubership.atp.macros.core.model.Macros;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import joptsimple.internal.Strings;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional(readOnly = true)
public class DataSetExportExecutor implements ExportExecutor {

    @Value("${spring.application.name}")
    private String implementationName;

    private static final String DATASET = "dataset";
    private final ObjectSaverToDiskService objectSaverToDiskService;
    private final ObjectWriter objectWriter;
    private final JpaDataSetRepository dataSetRepository;
    private final JpaDataSetListRepository dataSetListRepository;
    private final GridFsRepository gridFsRepository;
    private final MacroContextService macroContextService;
    private final DataSetListContextService dataSetListContextService;
    private final FileService fileService;
    private final JpaDataSetListService dslService;
    private final JpaDataSetServiceImpl dsService;
    private final DataSetParameterProvider dataSetParameterProvider;
    private final MacrosFeignClient macrosFeignClient;
    private final MacrosCalculator macrosCalculator;

    /**
     * Instantiates a new Data set export executor.
     *
     * @param objectSaverToDiskService the object saver to disk service
     * @param dataSetRepository the data set repository
     * @param dataSetListRepository the data set list repository
     * @param gridFsRepository the grid fs repository
     * @param fileService the fileService
     * @param dataSetParameterProvider the dataSetParameterProvider
     * @param macrosFeignClient the macrosFeignClient
     * @param macrosCalculator the macrosCalculator
     */
    public DataSetExportExecutor(ObjectSaverToDiskService objectSaverToDiskService,
                                 ObjectMapper exportObjectMapper,
                                 JpaDataSetRepository dataSetRepository,
                                 JpaDataSetListRepository dataSetListRepository,
                                 GridFsRepository gridFsRepository,
                                 MacroContextService macroContextService,
                                 DataSetListContextService dataSetListContextService,
                                 FileService fileService,
                                 JpaDataSetListService dslService,
                                 JpaDataSetServiceImpl dsService,
                                 DataSetParameterProvider dataSetParameterProvider,
                                 MacrosFeignClient macrosFeignClient,
                                 MacrosCalculator macrosCalculator) {
        this.objectSaverToDiskService = objectSaverToDiskService;
        this.objectWriter = exportObjectMapper.writer();
        this.dataSetRepository = dataSetRepository;
        this.dataSetListRepository = dataSetListRepository;
        this.gridFsRepository = gridFsRepository;
        this.macroContextService = macroContextService;
        this.dataSetListContextService = dataSetListContextService;
        this.fileService = fileService;
        this.dslService = dslService;
        this.dsService = dsService;
        this.dataSetParameterProvider = dataSetParameterProvider;
        this.macrosFeignClient = macrosFeignClient;
        this.macrosCalculator = macrosCalculator;
    }

    @Override
    public void exportToFolder(ExportImportData exportData, Path workDir) throws Exception {
        log.info("Start export by request {}", exportData);
        switch (exportData.getFormat()) {
            case ATP:
                atpExport(exportData, workDir);
                break;
            case NTT:
                nttExport(exportData, workDir);
                break;
            case POSTMAN:
                postmanExport(exportData, workDir);
                break;
            default:
                log.error("Export data unformatted: {}", exportData.getFormat());
        }
        log.info("Finish export by request {}", exportData);
    }

    private void atpExport(ExportImportData exportData, Path workDir) {
        log.info("Start ATP export by request {}", exportData);
        expandExportScope(exportData);
        exportProjectByScope(exportData, workDir);
        log.info("Finish ATP export by request {}", exportData);
    }

    private void nttExport(ExportImportData exportData, Path workDir) {
        atpExport(exportData, workDir);
        UUID vaId = exportData.getProjectId();
        log.info("Converting to ntt project. Project id {}", vaId);
        convertToNttFormat(workDir);
    }

    protected void expandExportScope(ExportImportData exportData) {
        Set<String> allDslByVa = dataSetListRepository.getDslIdsByVa(exportData.getProjectId())
                .stream()
                .map(UUID::toString)
                .collect(Collectors.toSet());
        Set<String> exportScopeDatasetStorage = exportData.getExportScope().getEntities()
                .getOrDefault(Constants.ENTITY_DATASET_STORAGE, new HashSet<>());
        if (allDslByVa.equals(exportScopeDatasetStorage)) {
            // all dsls are exporting - skip expand scope
            log.info("Exporting all dsls for project, no expand export scope needed");
            return;
        }

        Set<String> exportScopeDatasets =
                exportData.getExportScope().getEntities().getOrDefault(Constants.ENTITY_DATASETS, new HashSet<>());
        Set<String> collectedDs = new HashSet<>(exportScopeDatasets);
        Set<String> collectedDsl = new HashSet<>();
        Map<UUID, LinkedList<UUID>> context = new HashMap<>();

        for (String dataSetId : exportScopeDatasets) {
            collectReferencesOnDsAndCorrespondingDsl(dataSetId, collectedDs, collectedDsl, null,
                    null,
                    context,
                    false,
                    true,
                    null);
        }
        exportData.getExportScope().getEntities().put(Constants.ENTITY_DATASETS, collectedDs);
        exportScopeDatasetStorage.addAll(collectedDsl);

        collectedDsl = new HashSet<>(exportScopeDatasetStorage);
        for (String dataSetListId : exportScopeDatasetStorage) {
            collectReferencesOnDslFromAttribute(dataSetListId, collectedDsl);
        }
        log.debug("collectedDsl {}", collectedDsl);
        exportScopeDatasetStorage.addAll(collectedDsl);
        exportData.getExportScope().getEntities().put(Constants.ENTITY_DATASET_STORAGE, exportScopeDatasetStorage);
    }

    private void collectReferencesOnDslFromAttribute(String dataSetListId, Set<String> collectedDsl) {
        log.debug("start collectReferencesOnDslFromAttribute(dataSetListId: {})", dataSetListId);
        collectedDsl.add(dataSetListId);
        Optional<DataSetListEntity> dataSetListEntity = dataSetListRepository.findById(UUID.fromString(dataSetListId));
        if (!dataSetListEntity.isPresent()) {
            log.info("Data Set List not found by id {}", dataSetListId);
            return;
        }
        Set<UUID> dataSetLists = new HashSet<>();
        List<AttributeEntity> attributes = dataSetListEntity.get().getAttributes();
        for (AttributeEntity attribute : attributes) {
            if (attribute.getAttributeTypeId().equals((long) AttributeType.DSL.getId())) {
                dataSetLists.add(attribute.getTypeDataSetListId());
            }
        }
        for (UUID dslId : dataSetLists) {
            if (!collectedDsl.contains(dslId.toString())) {
                collectReferencesOnDslFromAttribute(dslId.toString(), collectedDsl);
            }
        }
    }

    /**
     * For given DS we recursively collect all referenced from params DS and from param Values via RefDslMacros,
     * also all DLS for each DS is collected and all DSL referenced by RefDslMacros.
     */
    private void collectReferencesOnDsAndCorrespondingDsl(String dataSetId,
                                                          Set<String> alreadyCollectedDs,
                                                          Set<String> alreadyCollectedDsl,
                                                          MacroContext macroContext,
                                                          List<UUID> attrPath,
                                                          Map<UUID, LinkedList<UUID>> context,
                                                          boolean notFromReference,
                                                          boolean firstEnter,
                                                          UUID lastRefDsBeforeMacrosId
    ) {
        log.debug("start collectReferencesOnDsAndCorrespondingDsl(dataSetId: {}, macroContext: {}, attrPath: {})",
                dataSetId, macroContext, attrPath);
        alreadyCollectedDs.add(dataSetId);
        Optional<DataSetEntity> dataSet = dataSetRepository.findById(UUID.fromString(dataSetId));
        if (!dataSet.isPresent()) {
            log.info("Data Set not found by id {}", dataSetId);
            return;
        }
        UUID dataSetListId = dataSet.get().getDataSetList().getId();
        alreadyCollectedDsl.add(dataSetListId.toString()); //collect DSL for all DS

        if (macroContext == null) {
            macroContext = new MacroContext();
            macroContext.setMacroContextService(macroContextService);
            DataSetListContext dataSetListContext = dataSetListContextService.getDataSetListContext(
                    dataSetListId,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    null
            );
            macroContext.setDataSetListContext(dataSetListContext);
        }

        if (attrPath == null) {
            attrPath = new ArrayList<>();
        }

        if (firstEnter) {
            lastRefDsBeforeMacrosId = UUID.fromString(dataSetId);
        }

        List<ParameterEntity> params = dataSet.get().getParameters();
        for (ParameterEntity param : params) {
            log.debug("Parameter Entity id {}", param.getId());
            List<UUID> attrPathInner = new ArrayList<>();
            if (param.getAttribute().isAttributeKey()) {
                AttributeKey key = new Parameter(param).getAttributeKey();
                if (key != null) {
                    attrPathInner.addAll(key.getPath());
                }
            } else {
                attrPathInner.addAll(attrPath);
            }
            UUID dataSetReferenceId = param.getDataSetReferenceId(); //collect referenced from param DS
            log.debug("dataSetReferenceId {}", dataSetReferenceId);

            Set<UUID> dataSetsFromReference = new HashSet<>();
            Set<UUID> dataSetsFromMacros = new HashSet<>();
            Set<UUID> dataSetList = new HashSet<>();

            if (dataSetReferenceId != null) {
                dataSetsFromReference.add(dataSetReferenceId);
                attrPathInner.add(param.getAttribute().getOriginAttributeId());
            } else {
                collectReferencesFromParamValue(
                        param.getStringValue(),
                        dataSetsFromMacros,
                        dataSetList,
                        macroContext,
                        attrPathInner,
                        UUID.fromString(dataSetId),
                        context,
                        notFromReference,
                        lastRefDsBeforeMacrosId); //collect references by parsing param value like #REF_DSL(DSL.DS.ATTR)
            }
            for (UUID dsId : dataSetsFromReference) {
                if (!alreadyCollectedDs.contains(dsId.toString())) {
                    collectReferencesOnDsAndCorrespondingDsl(dsId.toString(), alreadyCollectedDs, alreadyCollectedDsl,
                            macroContext, attrPathInner, context, false, false,
                            lastRefDsBeforeMacrosId);
                }
            }
            for (UUID dsId : dataSetsFromMacros) {
                if (!alreadyCollectedDs.contains(dsId.toString())) {
                    collectReferencesOnDsAndCorrespondingDsl(dsId.toString(), alreadyCollectedDs, alreadyCollectedDsl,
                            null, null, context,
                            true,  // call from Macros
                            false,
                            lastRefDsBeforeMacrosId);
                }
            }
            alreadyCollectedDsl.addAll(dataSetList.stream().map(UUID::toString).collect(Collectors.toSet()));
        }
    }

    /**
     * Collect references by parsing value of param like #REF_DSL(DSL.DS.ATTR).
     */
    private void collectReferencesFromParamValue(String parameterString, Set<UUID> dataSets, Set<UUID> dataSetLists,
                                                 MacroContext macroContext, List<UUID> parameterPath,
                                                 UUID dataSetId,
                                                 Map<UUID, LinkedList<UUID>> context,
                                                 boolean notFromReference,
                                                 UUID lastRefDsBeforeMacrosId
    ) {
        log.debug("start collectReferencesFromParamValue(parameterString: {}, macroContext: {}, parameterPath: {})",
                parameterString, macroContext, parameterPath);
        if (parameterString == null) {
            return;
        }

        int dataSetPosition;
        if (notFromReference) {
            dataSetPosition = getDataSetPositionFromContextOrDb(dataSetId, context);
        } else {
            dataSetPosition = getDataSetPositionFromContextOrDb(lastRefDsBeforeMacrosId, context);
        }
        ParameterPositionContext parameterPositionContext = new ParameterPositionContext(parameterPath,
                dataSetPosition, null, 0L, null);
        TextParameterParser parser = new TextParameterParser(macroContext, parameterPositionContext);
        List<AbstractTextParameter> parseResult = parser.parse(parameterString, true);
        for (AbstractTextParameter abstractTextParameter : parseResult) {
            abstractTextParameter.getValue();
            walkThroughResultTree(abstractTextParameter, dataSets, dataSetLists);
        }
    }

    /**
     * Calculate dataset position index in DS.
     */
    private int getDataSetPositionFromContextOrDb(UUID dataSetId, Map<UUID, LinkedList<UUID>> context) {
        UUID dataSetListId = dsService.getDataSetsListIdByDataSetId(dataSetId);
        if (!context.containsKey(dataSetListId)) {
            LinkedList<UUID> dataSetsOrders = dslService.getDataSetsIdsByDataSetListId(dataSetListId);
            context.put(dataSetListId, dataSetsOrders);
        }
        LinkedList<UUID> datasetIds = context.get(dataSetListId);
        log.error("MacrosContext DataSet Id: " + dataSetId + ", position: " + datasetIds.indexOf(dataSetId)
                + ", datasetIds: " + datasetIds);
        return datasetIds.indexOf(dataSetId);
    }

    private void walkThroughResultTree(AbstractTextParameter parseResult, Set<UUID> dataSets, Set<UUID> dataSetLists) {
        List<AbstractTextParameter> children = parseResult.getChildTextParameters();
        if (!isEmpty(children)) {
            for (AbstractTextParameter parameter : children) {
                walkThroughResultTree(parameter, dataSets, dataSetLists);
            }
        }
        if (parseResult instanceof RefDslMacro) {
            dataSets.addAll(((RefDslMacro) parseResult).getDataSets());
            dataSetLists.addAll(((RefDslMacro) parseResult).getDataSetLists());
        }
    }

    private void convertToNttFormat(Path workDir) throws ExportException {
        log.info("Starting to convert to ntt using export source dir {}", workDir);
        try {
            new NttProjectConverter(workDir).convertDataSet().saveToFolder(workDir);
        } catch (Exception e) {
            log.error("NttConverter exception {}", workDir, e);
            ExportException.throwException("Cannot convert export to NTT format. Work dir {}", workDir, e);
        }
    }

    private void exportProjectByScope(ExportImportData atpScopeData, Path workDir) throws ExportException {
        ExportScope atpScopeExport = atpScopeData.getExportScope();

        Map<UUID, Short> attributeTypes = new HashMap<>();
        Set<String> exportScopeDatasetStorage = atpScopeExport.getEntities()
                .getOrDefault(Constants.ENTITY_DATASET_STORAGE, new HashSet<>());
        for (String dataSetListId : exportScopeDatasetStorage) {
            exportDataSetStorage(UUID.fromString(dataSetListId), workDir, attributeTypes, atpScopeExport);
        }

        Set<String> exportScopeDatasets = atpScopeExport.getEntities()
                .getOrDefault(Constants.ENTITY_DATASETS, new HashSet<>());
        for (String dataSetId : exportScopeDatasets) {
            exportDataSet(UUID.fromString(dataSetId), workDir, attributeTypes, atpScopeExport);
        }
    }

    private void exportDataSet(UUID dataSetId, Path workDir, Map<UUID, Short> attributeTypes,
                               ExportScope atpScopeExport) {
        Optional<DataSetEntity> dataSet = dataSetRepository.findById(dataSetId);
        if (!dataSet.isPresent()) {
            log.info("Data Set not found by id {}", dataSetId);
            return;
        }
        DataSet eds = new DataSet(dataSet.get().getId(), dataSet.get().getName());
        // todo replace this with mixin and jsonserializer from objectmapper to replace
        // complex properties of java model to simple value of json required for export.
        // in case if data set will not have got jpa entity simpler (without complex fields)
        UUID parentId = dataSet.get().getDataSetList().getId();
        eds.setDataSetList(parentId);
        eds.setOrdering(dataSet.get().getOrdering());
        eds.setIsLocked(dataSet.get().isLocked());
        objectSaverToDiskService.exportAtpEntity(dataSetId, eds, parentId, workDir);

        List<UUID> idsOfFileParameters = new ArrayList<>();
        List<ParameterEntity> params = dataSet.get().getParameters();
        for (ParameterEntity param : params) {

            AbstractAttributeEntity attribute = param.getAttribute();
            if (attribute != null) {
                UUID dataSetListId = attribute.getDataSetList().getId();
                if (!atpScopeExport.getEntities().getOrDefault(Constants.ENTITY_DATASET_STORAGE, new HashSet<>())
                        .contains(dataSetListId.toString())) {
                    // skip parameter with attribute that refers to data set list which is non including in export
                    continue;
                }
            }

            exportParameter(param, dataSetId, workDir, attributeTypes, idsOfFileParameters);
        }

        if (!idsOfFileParameters.isEmpty()) {
            exportFiles(idsOfFileParameters, dataSet.get(), workDir);
        }
    }

    private void exportFiles(List<UUID> idsOfFileParameters,
                             DataSetEntity dataSet, Path workDir) {
        Map<UUID, Optional<InputStream>> files = gridFsRepository.getAll(idsOfFileParameters);
        for (Map.Entry<UUID, Optional<InputStream>> entry : files.entrySet()) {
            UUID parameterId = entry.getKey();
            Optional<InputStream> file = entry.getValue();
            if (file.isPresent()) {
                Optional<FileData> fileInfo = gridFsRepository.getFileInfo(parameterId);
                if (!fileInfo.isPresent()) {
                    log.info("File not found for parameter with id {}", parameterId);
                    continue;
                }
                FileData fileData = fileInfo.get();
                UUID datasetListIdentifier = dataSet.getDataSetList().getId();
                UUID datasetIdentifier = dataSet.getId();
                saveDataSetFile(fileData, datasetListIdentifier, datasetIdentifier, workDir, file);
            }
        }
    }

    private void checkListValueParameter(ParameterEntity param, ListValueEntity listValue,
                                         DataSetParameter exportParameter, Path workDir) {
        AbstractAttributeEntity listValueAttr = listValue.getAttribute();
        AbstractAttributeEntity paramAttr = param.getAttribute();
        if (paramAttr instanceof AttributeEntity) {
            UUID attrParamId = paramAttr.getId();
            UUID attrListValueId = listValueAttr.getId();
            if (!attrParamId.equals(attrListValueId)) {
                Optional<ListValueEntity> optionalListValue = ((AttributeEntity) paramAttr).getListValues().stream()
                        .filter(value -> value.getText().equals(listValue.getText())).findFirst();
                if (optionalListValue.isPresent()) {
                    exportParameter.setListValue(optionalListValue.get().getId());
                } else {
                    UUID newId = UUID.randomUUID();
                    DataSetListValue listValueExport = new DataSetListValue();
                    listValueExport.setAttribute(attrParamId);
                    listValueExport.setId(newId);
                    listValueExport.setText(listValue.getText());
                    objectSaverToDiskService
                            .exportAtpEntity(listValueExport.getId(), listValueExport,
                                    listValueExport.getAttribute(), workDir);
                    exportParameter.setListValue(newId);
                }
            } else {
                exportParameter.setListValue(listValue.getId());
            }
        } else {
            exportParameter.setListValue(listValue.getId());
        }
    }

    private void exportParameter(ParameterEntity param, UUID dataSetId, Path workDir,
                                 Map<UUID, Short> attributeTypes, List<UUID> idsOfFileParameters) {
        DataSetParameter exportParameter = new DataSetParameter(param.getId());
        exportParameter.setDataSet(dataSetId);

        ListValueEntity listValue = param.getListValue();
        if (listValue != null) {
            checkListValueParameter(param, listValue, exportParameter, workDir);
        }

        exportParameter.setAttribute(param.getAttribute().getId());
        Short getAttributeType = attributeTypes.get(param.getAttribute().getId());

        if (Objects.equals(ENCRYPTED.getId(), getAttributeType)) {
            exportParameter.setStringValue(null); // skip encrypted value
        } else {
            exportParameter.setStringValue(param.getStringValue());
        }
        exportParameter.setDataSetReferenceValue(param.getDataSetReferenceId());
        exportParameter.setFileValueId(param.getFileValueId());
        objectSaverToDiskService.exportAtpEntity(param.getId(), exportParameter, param.getDataSet().getId(), workDir);

        if (Objects.equals(FILE.getId(), getAttributeType)) {
            idsOfFileParameters.add(param.getId());
        }
    }

    private void exportDataSetStorage(UUID dataSetListId, Path workDir,
                                      Map<UUID, Short> attributeTypes,
                                      ExportScope atpScopeExport) {
        Optional<DataSetListEntity> dataSetListEntity = dataSetListRepository.findById(dataSetListId);
        if (!dataSetListEntity.isPresent()) {
            log.info("Data Set List not found by id {}", dataSetListId);
            return;
        }
        UUID vaId = dataSetListEntity.get().getVisibilityArea().getId();
        DataSetList dataSetList = new DataSetList(dataSetListEntity.get().getId(), dataSetListEntity.get().getName());
        dataSetList.setTestPlan(
                dataSetListEntity.get().getTestPlan() == null ? null : dataSetListEntity.get().getTestPlan().getId());
        dataSetList.setVisibilityArea(vaId);
        dataSetList.setCreatedBy(dataSetListEntity.get().getCreatedBy());
        dataSetList.setCreatedWhen(dataSetListEntity.get().getCreatedWhen());
        dataSetList.setModifiedBy(dataSetListEntity.get().getModifiedBy());
        dataSetList.setModifiedWhen(dataSetListEntity.get().getModifiedWhen());
        objectSaverToDiskService.exportAtpEntity(dataSetListId, dataSetList, vaId, workDir);

        List<AttributeEntity> attributes = dataSetListEntity.get().getAttributes();
        for (AttributeEntity attribute : attributes) {
            exportAttribute(attribute, workDir);
            attributeTypes.put(attribute.getId(), attribute.getAttributeTypeId().shortValue());
        }

        Set<AttributeKeyEntity> attKeys = dataSetListEntity.get().getAttributeKeys();
        for (AttributeKeyEntity attrKey : attKeys) {
            if (atpScopeExport.getEntities().getOrDefault(Constants.ENTITY_DATASETS, new HashSet<>())
                    .contains(attrKey.getDataSet().getId().toString())) {
                exportAttributeKey(attrKey, workDir);
                AttributeEntity attribute = attrKey.getAttribute();
                exportAttribute(attribute, workDir);
                attributeTypes.put(attrKey.getId(), attribute.getAttributeTypeId().shortValue());
            }
        }
    }

    private void exportAttributeKey(AttributeKeyEntity attribute, Path workDir) throws ExportException {
        DataSetAttributeKey exportAttribute = new DataSetAttributeKey(attribute.getId(), attribute.getKey());
        exportAttribute.setAttribute(attribute.getAttribute().getId());
        exportAttribute.setDataSetList(attribute.getDataSetList().getId());
        exportAttribute.setDataSet(attribute.getDataSet().getId());
        objectSaverToDiskService.exportAtpEntity(attribute.getId(), exportAttribute,
                attribute.getDataSetList().getId(), workDir);
    }

    private void exportAttribute(AttributeEntity attribute, Path workDir) throws ExportException {
        DataSetAttribute exportAttribute = new DataSetAttribute(attribute.getId(), attribute.getName());
        exportAttribute.setAttributeType(attribute.getAttributeTypeId());
        exportAttribute.setDataSetList(attribute.getDataSetList().getId());
        exportAttribute.setOrdering(attribute.getOrdering());
        exportAttribute.setTypeDataSetList(attribute.getTypeDataSetListId());
        objectSaverToDiskService.exportAtpEntity(attribute.getId(), exportAttribute,
                attribute.getDataSetList().getId(), workDir);
        List<ListValueEntity> listValues = attribute.getListValues();
        if (listValues != null) {
            for (ListValueEntity lv : listValues) {
                DataSetListValue listValue = new DataSetListValue();
                listValue.setAttribute(attribute.getId());
                listValue.setId(lv.getId());
                listValue.setText(lv.getText());
                objectSaverToDiskService
                        .exportAtpEntity(listValue.getId(), listValue, listValue.getAttribute(), workDir);
            }
        }
    }

    private void saveDataSetFile(FileData fileData, UUID datasetListIdentifier, UUID datasetIdentifier, Path workDir,
                                 Optional<InputStream> file) throws ExportException {
        Path dataSetFilesPath = workDir.resolve("files")
                .resolve(datasetListIdentifier.toString())
                .resolve(datasetIdentifier.toString());
        fileService.createDirectory(dataSetFilesPath);
        String fileName = fileData.getParameterUuid().toString();
        try (InputStream in = file.get()) {
            Path filePath = dataSetFilesPath.resolve(fileName);
            Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            ExportException.throwException("Cannot save file {} on disk {}", fileName, dataSetFilesPath);
        }

        Path filePath = fileService.createFile(fileName + ".json", dataSetFilesPath);
        try {
            objectWriter.writeValue(filePath.toFile(), fileData);
        } catch (IOException e) {
            log.error("Cannot write object {} in file {}", fileData, filePath, e);
            ExportException
                    .throwException("Cannot write object {} in file ", fileData, filePath.toString(), e);
        }
    }

    @Override
    public String getExportImplementationName() {
        return implementationName;
    }

    private void postmanExport(ExportImportData exportData, Path workDir) {
        log.info("Start Postman export by request {}", exportData);
        Set<String> exportScopeDatasetStorage = exportData.getExportScope().getEntities()
                .getOrDefault(Constants.ENTITY_DATASET_STORAGE, new HashSet<>());
        for (String dataSetListId : exportScopeDatasetStorage) {
            exportDatasetList(UUID.fromString(dataSetListId), workDir);
        }
        log.info("Finish Postman export by request {}", exportData);
    }

    private void exportDatasetList(UUID dataSetListId, Path workDir) {
        DataSetListContext dataSetListContext = createDataSetListContext(dataSetListId);
        MacroContext macroContext = createMacrosContext(dataSetListContext);
        for (DataSetContext dataset : dataSetListContext.getDataSets()) {
            exportDataset(dataset, macroContext, dataSetListContext, workDir);
        }
    }

    private DataSetListContext createDataSetListContext(UUID dataSetListId) {
        AttributeTypeName[] attributesToLoad = {
                AttributeTypeName.TEXT,
                AttributeTypeName.ENCRYPTED,
                AttributeTypeName.LIST,
                AttributeTypeName.FILE,
                AttributeTypeName.CHANGE,
                AttributeTypeName.DSL};

        List<Integer> columnsToLoad = dslService.getById(dataSetListId).getDataSetsColumns(
                dataSetListRepository.getDataSetsIdsByDataSetListId(dataSetListId));

        return dataSetListContextService.getDataSetListContext(dataSetListId,
                columnsToLoad,
                Arrays.asList(attributesToLoad),
                null);
    }

    private MacroContext createMacrosContext(DataSetListContext dataSetListContext) {
        MacroContext macroContext = new MacroContext();
        macroContext.setMacroContextService(macroContextService);
        macroContext.setMacrosCalculator(macrosCalculator);
        List<MacrosDto> macrosDtoList = macrosFeignClient
                .findAllByProject(dataSetListContext.getVisibilityAreaId()).getBody();
        List<Macros> macros = new MacrosDtoConvertService().convertList(macrosDtoList, Macros.class);
        macroContext.setMacros(macros);
        macroContext.setDataSetListContext(dataSetListContext);
        return macroContext;
    }

    private void exportDataset(DataSetContext dataset, MacroContext macroContext,
                               DataSetListContext dataSetListContext, Path workDir) {
        List<ExportPostmanParameter> parameters = new ArrayList<>();
        for (ParameterContext param : dataset.getParameters()) {
            processParameter(param, dataset, macroContext, dataSetListContext.getGroups(), parameters, null);
        }
        String dsName = String.format("%s.%s", dataSetListContext.getDataSetListName(), dataset.getName());
        String fileName = String.format("%s.%s", dsName, DATASET);
        ExportPostmanDataset exportDataset = new ExportPostmanDataset(dataset.getId(), dsName, parameters);
        objectSaverToDiskService.writeAtpEntityToFile(fileName, exportDataset, DATASET, workDir, true);
    }

    private void processParameter(ParameterContext param, DataSetContext dataset,
                                  MacroContext macroContext, List<GroupContext> groups,
                                  List<ExportPostmanParameter> parameters, @Nullable String attrPath) {

        String path = getPath(attrPath, param.getName());
        if (param.getType().equals(AttributeTypeName.DSL)) {
            if (param.getValue() == null) {
                parameters.add(new ExportPostmanParameter(path, Strings.EMPTY));
            } else {
                parameters.add(new ExportPostmanParameter(path, param.getValue()));
                GroupContext groupContext = groups.stream().filter(group -> param.getOrder() == group.getOrder())
                        .findFirst().get();
                processGroup(param, macroContext, groupContext, parameters, path);
            }
        } else {
            if (!param.getType().equals(AttributeTypeName.ENCRYPTED)
                    && !param.getType().equals(AttributeTypeName.FILE)
                    && !param.getType().equals(AttributeTypeName.CHANGE)) {
                AbstractParameter resolvedParameter = dataSetParameterProvider.getDataSetParameterResolved(
                        macroContext.getDataSetListContext().getDataSetListId(),
                        param.getParameterId(),
                        param.getType(),
                        true,
                        macroContext,
                        new ParameterPositionContext(
                                Collections.emptyList(),
                                dataset.getColumnNumber(),
                                dataset.getId(),
                                param.getOrder(),
                                macroContext.getDataSetListContext().getDataSetListId()
                        )
                );
                parameters.add(new ExportPostmanParameter(path, resolvedParameter.getValue()));
            } else {
                parameters.add(new ExportPostmanParameter(path, Strings.EMPTY));
            }
        }
    }

    private String getPath(String attrPath, String paramName) {
        if (Strings.isNullOrEmpty(attrPath)) {
            return paramName;
        } else {
            return String.format("%s.%s", attrPath, paramName);
        }
    }

    private void processGroup(ParameterContext param, MacroContext macroContext, GroupContext groupContext,
                              List<ExportPostmanParameter> parameters, @Nullable String attrPath) {
        DataSetContext refDs = groupContext.getDataSets().stream()
                .filter(ds -> ds.getName().equals(param.getValue()))
                .findFirst().get();
        for (ParameterContext parameter : refDs.getParameters()) {
            processParameter(parameter, refDs, macroContext, groupContext.getGroups(), parameters, attrPath);
        }
    }
}
