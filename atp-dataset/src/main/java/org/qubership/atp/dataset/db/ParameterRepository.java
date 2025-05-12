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

package org.qubership.atp.dataset.db;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.constants.CacheEnum;
import org.qubership.atp.dataset.db.dto.AttributePathDto;
import org.qubership.atp.dataset.db.dto.ParameterDataDto;
import org.qubership.atp.dataset.db.dto.ParameterDto;
import org.qubership.atp.dataset.db.utils.Proxies;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributePath;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.ParameterOverlap;
import org.qubership.atp.dataset.model.impl.AttributePathImpl;
import org.qubership.atp.dataset.model.impl.ParameterImpl;
import org.qubership.atp.dataset.model.impl.ParameterOverlapImpl;
import org.qubership.atp.dataset.model.impl.TableResponse;
import org.qubership.atp.dataset.model.impl.file.FileData;
import org.qubership.atp.dataset.service.direct.ClearCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Repository;

import com.google.common.base.Preconditions;
import com.querydsl.core.Tuple;
import com.querydsl.core.dml.StoreClause;
import com.querydsl.core.types.MappingProjection;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.dml.SQLInsertClause;
import com.querydsl.sql.dml.SQLUpdateClause;

@Repository
public class ParameterRepository extends AbstractRepository {

    private final SQLQueryFactory queryFactory;
    private final ParameterDtoProjection projection;
    private final DataSetRepository dsRepo;
    private final ListValueRepository lvRepo;
    private final AttributeRepository attrRepo;
    private final AttributePathRepository attrPathRepo;
    private final GridFsRepository gridFsRepoProvider;
    private final CacheRepository cacheRepo;
    private final ClearCacheService clearCacheService;

    @Autowired
    protected ParameterRepository(SQLQueryFactory queryFactory,
                                  DataSetRepository dsRepo,
                                  ListValueRepository lvRepo,
                                  AttributeRepository attrRepo,
                                  AttributePathRepository attrPathRepo,
                                  GridFsRepository gridFsRepoProvider,
                                  CacheRepository cacheRepo,
                                  ClearCacheService clearCacheService) {
        this.queryFactory = queryFactory;
        this.dsRepo = dsRepo;
        this.lvRepo = lvRepo;
        this.attrRepo = attrRepo;
        this.attrPathRepo = attrPathRepo;
        this.cacheRepo = cacheRepo;
        this.clearCacheService = clearCacheService;
        this.projection = new ParameterDtoProjection(this);
        this.gridFsRepoProvider = gridFsRepoProvider;
    }

    @Nonnull
    public Parameter create(@Nonnull UUID dsId, @Nonnull UUID attributeId,
                            @Nonnull ParameterDataDto data) {
        return projection.convert(createParameter(dsId, attributeId, data), null);
    }

    /**
     * Creates and returns overlapped parameter.
     *
     * @param dslId - parent {@link DataSetList}
     * @param dsId - target {@link DataSet}
     * @param attributeId - target attribute
     * @param data - parameter value
     * @param attributePathIds - path to original value.
     * @return - overlapped parameter.
     */
    @Nonnull
    public ParameterOverlap overlap(@Nonnull UUID dslId, @Nonnull UUID dsId, @Nonnull UUID attributeId,
                                    @Nonnull ParameterDataDto data,
                                    @Nonnull List<UUID> attributePathIds) {
        AttributePathDto attrPathDto = attrPathRepo.create(dslId, dsId, attributeId, attributePathIds);
        //original attributeId is wrapped into attrPath now
        ParameterDto dto = createParameter(dsId, attrPathDto.getId(), data);
        return projection.convertOverlap(dto, attrPathDto);
    }

    private ParameterDto createParameter(@Nonnull UUID dsId,
                                         @Nonnull UUID attributeId,
                                         @Nonnull ParameterDataDto data) {
        UUID id = insertWithData(data)
                .set(PARAM.attributeId, attributeId)
                .set(PARAM.datasetId, dsId)
                .executeWithKey(PARAM.id);
        return new ParameterDto(id, dsId, attributeId, data);
    }

    /**
     * Updates data of a parameter.
     *
     * @return new parameter with updated data.
     */
    @Nonnull
    @CacheEvict(value = CacheEnum.Constants.PARAMETER_CACHE, key = "#parameter.getId()")
    public Parameter update(@Nonnull Parameter parameter, @Nonnull ParameterDataDto data) {
        Preconditions.checkArgument(update(parameter.getId(), data),
                "Can not update parameter: %s with data: %s", parameter, data);
        ParameterDto dto = new ParameterDto(parameter.getId(),
                parameter.getDataSet().getId(),
                parameter.getAttribute().getId(), data);
        AttributePathDto attrPathDto = ParameterDtoProjection.extractAttrPathDto(parameter);
        return projection.convert(dto, attrPathDto);
    }

    /**
     * Updates data of a parameters.
     *
     * @return true if anything was updated.
     */
    @Nonnull
    public boolean update(@Nonnull List<UUID> parameterIds, @Nonnull ParameterDataDto data) {
        return updateWithData(data).where(PARAM.id.in(parameterIds)).execute() > 0;
    }

    /**
     * Update parameter.
     */
    public boolean update(@Nonnull UUID paramId, @Nonnull ParameterDataDto data) {
        return updateWithData(data).where(PARAM.id.eq(paramId)).execute() > 0;
    }

    @Nullable
    public Parameter getById(UUID id) {
        return cacheRepo.tryComputeIfAbsent(Parameter.class, id, uuid -> select(PARAM.id.eq(uuid)).fetchOne());
    }

    public boolean existsById(UUID id) {
        return cacheRepo.getIfPresent(Parameter.class, id) != null
                || select(PARAM.id.eq(id)).fetchCount() > 0;
    }

    @Nonnull
    List<Parameter> getByDataSetId(UUID dsId) {
        return select(PARAM.datasetId.eq(dsId)).fetch();
    }

    @Nonnull
    public List<Parameter> getByAttributeId(UUID attributeId) {
        return select(PARAM.attributeId.eq(attributeId)).fetch();
    }

    @Nullable
    public Parameter getByDataSetIdAttributeId(@Nonnull UUID dataSetId, @Nonnull UUID attributeId) {
        return select(PARAM.datasetId.eq(dataSetId).and(PARAM.attributeId.eq(attributeId))).fetchOne();
    }

    @Nullable
    public List<Parameter> getByAttributeIdAndDatasetIds(@Nonnull UUID attributeId, @Nonnull Set<UUID> datasetIds) {
        return select(PARAM.attributeId.eq(attributeId).and(PARAM.datasetId.in(datasetIds))).fetch();
    }

    /**
     * Loads parameter from repository and returns parameter as Overlapped parameter.
     */
    @Nullable
    public ParameterOverlap getOverlap(@Nonnull UUID dslId, @Nonnull UUID dsId, @Nonnull UUID attributeId,
                                       @Nonnull List<UUID> attributePathIds) {
        AttributePathDto attrPathDto = attrPathRepo.get(dslId, dsId, attributeId, attributePathIds);
        if (attrPathDto == null) {
            return null;
        }
        Parameter param = getByDataSetIdAttributeId(dsId, attrPathDto.getId());
        if (param == null) {
            return null;
        }
        return param.asOverlap();
    }

    @Nonnull
    public List<Parameter> getAll() {
        return queryFactory.select(projection).from(PARAM).fetch();
    }

    /**
     * Get overridden parameters.
     *
     * @return {@link ParameterOverlap}s of target attribute
     */
    @Nonnull
    public Stream<ParameterOverlap> getOverlaps(@Nonnull UUID targetAttributeId,
                                                java.util.function.Predicate<AttributePath> filter) {
        List<AttributePathDto> pathes = attrPathRepo.getByTargetAttrId(targetAttributeId);
        if (pathes == null) {
            return Stream.empty();
        }
        return pathes.stream()
                .map(projection::convertAttrPath)
                .filter(filter)
                .flatMap(path -> getByAttributeId(path.getId()).stream())
                .map(Parameter::asOverlap);
    }

    /**
     * Deletes all overlapped parameters for target attribute id by predicate.
     */
    public void deleteOverlaps(UUID targetAttributeId, java.util.function.Predicate<AttributePath> filter) {
        List<ParameterOverlap> toDelete = getOverlaps(targetAttributeId, filter).collect(Collectors.toList());
        for (ParameterOverlap overlap : toDelete) {
            delete(overlap);
        }
    }

    /**
     * Deletes overlapped parameter.
     */
    public void deleteOverlap(@Nonnull UUID dataSetListId,
                              @Nonnull UUID dataSetId,
                              @Nonnull UUID targetAttributeId,
                              @Nonnull List<UUID> attributePathIds) {
        AttributePathRepository attrPathRepo = this.attrPathRepo;
        attrPathRepo.delete(dataSetListId, dataSetId, targetAttributeId, attributePathIds);
    }

    /**
     * Deletes parameter. It can be Simple parameter or Overlapped parameter.
     */
    public boolean delete(Parameter parameter) {
        if (parameter.isOverlap()) {
            AttributePath path = parameter.asOverlap().getAttributePath();
            return attrPathRepo.delete(path.getId());
        }
        clearCacheService.evictParameterCache(parameter.getId());
        clearCacheService.evictDatasetListContextCache(parameter.getDataSet().getId());
        return delete(parameter.getId());
    }

    private boolean delete(UUID id) {
        gridFsRepoProvider.onDeleteCascade(Collections.singletonList(id));
        return delete(PARAM.id.eq(id)) > 0;
    }

    private long delete(@Nonnull Predicate predicate) {
        return queryFactory.delete(PARAM).where(predicate).execute();
    }

    private void setData(@Nonnull StoreClause clause, @Nonnull ParameterDataDto data) {
        if (data.getStringValue() != null) {
            clause.set(PARAM.string, data.getStringValue());
        } else if (data.getListValueId() != null) {
            clause.set(PARAM.list, data.getListValueId());
        } else if (data.getDataSetReferenceId() != null) {
            clause.set(PARAM.ds, data.getDataSetReferenceId());
        } else {
            clause.set(PARAM.string, data.getStringValue());
        }
    }

    private void setUpdateData(@Nonnull StoreClause clause, @Nonnull ParameterDataDto data) {
        if (data.getListValueId() != null) {
            clause.set(PARAM.string, null);
            clause.set(PARAM.ds, null);
        } else if (data.getDataSetReferenceId() != null) {
            clause.set(PARAM.list, null);
            clause.set(PARAM.string, null);
        } else {
            clause.set(PARAM.list, null);
            clause.set(PARAM.ds, null);
        }
    }

    private SQLInsertClause insertWithData(@Nonnull ParameterDataDto data) {
        SQLInsertClause result = queryFactory.insert(PARAM);
        setData(result, data);
        return result;
    }

    private SQLUpdateClause updateWithData(@Nonnull ParameterDataDto data) {
        SQLUpdateClause result = queryFactory.update(PARAM);
        setData(result, data);
        setUpdateData(result, data);
        return result;
    }

    void onDsDeleteCascade(@Nonnull UUID dsId) {
        select(PARAM.datasetId.eq(dsId)).fetch().forEach(this::delete);
        select(PARAM.ds.eq(dsId)).fetch().forEach(this::delete);
    }

    void onAttrDeleteCascade(@Nonnull UUID attrId) {
        List<UUID> params = queryFactory.select(PARAM.id)
                .from(PARAM).where(PARAM.attributeId.eq(attrId)).fetch();
        gridFsRepoProvider.onDeleteCascade(params);
        attrPathRepo.onAttrDeleteCascade(attrId);
        delete(PARAM.id.in(params));
    }

    void onAttrPathDeleteCascade(@Nonnull UUID attrPathId) {
        List<UUID> params = queryFactory.select(PARAM.id).from(PARAM)
                .where(PARAM.attributeId.eq(attrPathId)).fetch();
        gridFsRepoProvider.onDeleteCascade(params);
        delete(PARAM.id.in(params));
    }

    @Nonnull
    private SQLQuery<Parameter> select(@Nonnull Predicate predicate) {
        return queryFactory.select(projection)
                .from(PARAM)
                .leftJoin(AK)
                .on(PARAM.attributeId.eq(AK.id))
                .where(predicate);
    }

    void onListValueDeleteCascade(UUID id) {
        delete(PARAM.list.eq(id));
    }

    void onListValuesDeleteCascade(List<UUID> ids) {
        queryFactory.update(PARAM).setNull(PARAM.list).where(PARAM.list.in(ids)).execute();
    }

    public List<Parameter> findAllByListValueId(UUID listValueId) {
        return select(PARAM.list.eq(listValueId)).fetch();
    }

    /**
     * To find information about dataset list, dataset and attribute of dependant list value id.
     */
    public List<?> findAllByListValueIdWithDataSetList(UUID listValueId) {
        BooleanExpression eq = PARAM.list.eq(listValueId);
        List<Tuple> result = getTableResponseListByPredicate(eq);
        return result.stream().map(TableResponse::fromParameterTuple).collect(Collectors.toList());
    }

    /**
     * To find information about dataset list, dataset and attribute of dependant list value ids.
     */
    public List<TableResponse> findAllByListValueIdsWithDataSetList(List<UUID> listValueIds) {
        BooleanExpression in = PARAM.list.in(listValueIds);
        List<Tuple> result = getTableResponseListByPredicate(in);
        return result.stream().map(TableResponse::fromParameterTuple).collect(Collectors.toList());
    }

    private List<Tuple> getTableResponseListByPredicate(BooleanExpression predicate) {
        return queryFactory
                .select(DSL.id, DSL.name, DS.id, DS.name, PARAM.id, PARAM.list, ATTR.id, ATTR.name)
                .from(PARAM)
                .where(predicate)
                .leftJoin(DS).on(PARAM.datasetId.eq(DS.id))
                .leftJoin(DSL).on(DS.datasetlistId.eq(DSL.id))
                .leftJoin(ATTR).on(ATTR.id.eq(PARAM.attributeId))
                .fetch();
    }

    private static class ParameterDtoProjection extends MappingProjection<Parameter> {

        private final ParameterRepository parameterRepository;

        ParameterDtoProjection(ParameterRepository parameterRepository) {
            super(Parameter.class, PARAM.all(), AK.all());
            this.parameterRepository = parameterRepository;
        }

        @Nullable
        private static AttributePathDto extractAttrPathDto(@Nonnull Parameter parameter) {
            if (!parameter.isOverlap()) {
                return null;
            }
            AttributePath attrPath = parameter.asOverlap().getAttributePath();
            return new AttributePathDto(attrPath.getId(),
                    attrPath.getDataSet().getDataSetList().getId(),
                    attrPath.getDataSet().getId(),
                    attrPath.getTargetAttribute().getId(),
                    attrPath.getPath().stream().map(Identified::getId).collect(Collectors.toList()));
        }

        @Override
        protected Parameter map(Tuple row) {
            UUID id = row.get(PARAM.id);
            UUID attrId = row.get(PARAM.attributeId);
            assert attrId != null;
            AttributePathDto attrPath = null;
            UUID attrKeyId = row.get(AK.id);
            if (attrKeyId != null) {
                attrPath = AttributePathRepository.AttributePathProjection.createDto(AK, row);
            }
            UUID dsId = row.get(PARAM.datasetId);
            assert dsId != null;
            ParameterDto dto = new ParameterDto(id, dsId, attrId,
                    new ParameterDataDto(row.get(PARAM.string), row.get(PARAM.ds), row.get(PARAM.list)));
            return convert(dto, attrPath);
        }

        @Nonnull
        private ParameterOverlap convertOverlap(@Nonnull ParameterDto dto, @Nonnull AttributePathDto attrPath) {
            ParameterOverlap result = new ParameterOverlapImpl();
            AttributePathImpl path = convertAttrPath(attrPath);
            fillValues(result, dto, path.getTargetAttribute().getType());
            result.setAttributePath(path);
            return result;
        }

        @Nonnull
        private Parameter convert(@Nonnull ParameterDto dto, @Nullable AttributePathDto attrPath) {
            if (attrPath != null) {
                return convertOverlap(dto, attrPath);
            }
            Parameter result = new ParameterImpl();
            Attribute attribute = parameterRepository.attrRepo.getById(dto.getAttributeId());
            /*According to business logic, there is can't be null, cuz Parameter can't be created without Attribute*/
            result.setAttribute(attribute);
            fillValues(result, dto, attribute.getType());
            return result;
        }

        @Nonnull
        private AttributePathImpl convertAttrPath(@Nonnull AttributePathDto attrPathDto) {
            DataSet ds = Proxies.withId(DataSet.class, attrPathDto.getDataSetId(), parameterRepository.dsRepo::getById);
            Attribute targetAttr = Proxies.withId(Attribute.class, attrPathDto.getTargetAttributeId(),
                    parameterRepository.attrRepo::getById);
            List<Attribute> attrList = attrPathDto.getAttributePathIds().stream()
                    .map(uuid -> Proxies.withId(Attribute.class, uuid, parameterRepository.attrRepo::getById))
                    .collect(Collectors.toList());
            return new AttributePathImpl(attrPathDto.getId(), ds, targetAttr, attrList);
        }

        private void fillValues(Parameter model, ParameterDto parameterDto, AttributeType attributeType) {
            model.setId(parameterDto.getId());
            model.setDataSet(
                    Proxies.withId(
                            DataSet.class,
                            parameterDto.getDataSetId(),
                            parameterRepository.dsRepo::getById
                    )
            );
            ParameterDataDto parameterValue = parameterDto.getParameterData();
            switch (attributeType) {
                case CHANGE:
                case ENCRYPTED:
                case TEXT:
                    model.setText(parameterValue.getStringValue());
                    break;
                case LIST:
                    UUID listValueId = parameterValue.getListValueId();
                    if (listValueId != null) {
                        ListValue listValue = Proxies.withId(
                                ListValue.class,
                                listValueId,
                                parameterRepository.lvRepo::getById
                        );
                        model.setListValue(listValue);
                    }
                    break;
                case DSL:
                    UUID dataSetReferenceId = parameterValue.getDataSetReferenceId();
                    if (dataSetReferenceId != null) {
                        DataSet referencedDataSet = Proxies.withId(
                                DataSet.class,
                                dataSetReferenceId,
                                parameterRepository.dsRepo::getById
                        );
                        model.setDataSetReference(referencedDataSet);
                    }
                    break;
                case FILE:
                    Optional<FileData> fileInfo = parameterRepository.gridFsRepoProvider.getFileInfo(
                            parameterDto.getId()
                    );
                    fileInfo.ifPresent(model::setFileData);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown attribute type");
            }
        }
    }
}
