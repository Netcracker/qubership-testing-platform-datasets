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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Filter;
import org.qubership.atp.dataset.model.impl.FilterImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.common.collect.Sets;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.MappingProjection;
import com.querydsl.core.types.dsl.SimplePath;
import com.querydsl.sql.RelationalPathBase;
import com.querydsl.sql.SQLQueryFactory;

@Repository
public class FilterRepository extends AbstractRepository {

    private final SQLQueryFactory queryFactory;
    private final FilterProjection filterProjection = new FilterProjection();

    @Autowired
    public FilterRepository(SQLQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    /**
     * Creates filter with binding to DSL. And contains DSL Labels and DS Labels id.
     *
     * @param name      filter name
     * @param vaId      target visibility area, which contains this filter
     * @param dsLabels  - dataset labels which included in filter
     * @param dslLabels - dataset list labels which included in filter
     * @return instance of {@link FilterImpl}.
     */
    public Filter create(String name, UUID vaId, List<UUID> dsLabels, List<UUID> dslLabels) {
        UUID filterId = queryFactory.insert(FILTERS)
                .set(FILTERS.name, name)
                .set(FILTERS.vaId, vaId)
                .executeWithKey(FILTERS.id);
        dsLabels.forEach(dsLabelId -> queryFactory.insert(FILTER_DS_LABELS)
                .set(FILTER_DS_LABELS.filterId, filterId)
                .set(FILTER_DS_LABELS.dsLabelId, dsLabelId)
                .execute()
        );
        dslLabels.forEach(dslLabelId -> queryFactory.insert(FILTER_DSL_LABELS)
                .set(FILTER_DSL_LABELS.filterId, filterId)
                .set(FILTER_DSL_LABELS.dslLabelId, dslLabelId)
                .execute()
        );
        //In case labels or dsl doesn't exist, we would get an error in insert method above (check constrains).
        //therefore we can make decision, that they exist, and it could be set.
        FilterImpl filter = new FilterImpl(filterId, name);
        filter.setDataSetListLabels(dslLabels);
        filter.setDataSetLabels(dsLabels);
        filter.setVisibilityAreaId(vaId);
        return filter;
    }

    /**
     * Cascade delete for dsl. Method will drop references to labels and all filters which bound to
     * target dsl.
     *
     * @param vaId of {@link DataSetList}
     */
    public void onVaDeleteCascade(UUID vaId) {
        List<UUID> filters = queryFactory.select(FILTERS.id).from(FILTERS).where(FILTERS.vaId.eq(vaId)).fetch();
        filters.forEach(this::delete);
    }

    /**
     * Method will delete filter by Id, and removes references fro Filter_DS/DSL_labels.
     *
     * @param filterId of target {@link Filter}
     */
    public void delete(UUID filterId) {
        deleteReferenceToLabel(filterId, FILTER_DSL_LABELS, FILTER_DSL_LABELS.filterId);
        deleteReferenceToLabel(filterId, FILTER_DS_LABELS, FILTER_DS_LABELS.filterId);
        deleteReferenceToLabel(filterId, FILTERS, FILTERS.id);
    }

    private void deleteReferenceToLabel(UUID filterId, RelationalPathBase path, SimplePath<UUID> simplePath) {
        queryFactory.delete(path).where(simplePath.eq(filterId)).execute();
    }

    /**
     * Retrieve the filter from repository by filterId.
     *
     * @param filterId of target {@link Filter}
     * @return {@link Filter} which found by targetId.
     */
    public Filter get(UUID filterId) {
        Filter filter = queryFactory
                .select(filterProjection)
                .from(FILTERS)
                .where(FILTERS.id.eq(filterId)).fetchOne();
        if (filter == null) {
            return null;
        }
        fillLabels(filter);
        return filter;
    }

    public boolean existsById(UUID id) {
        return queryFactory.select(filterProjection).from(FILTERS).where(FILTERS.id.eq(id)).fetchCount() > 0;
    }

    private void fillLabels(Filter filter) {
        filter.setDataSetLabels(getLabelIds(
                filter.getId(), FILTER_DS_LABELS.dsLabelId, FILTER_DS_LABELS, FILTER_DS_LABELS.filterId
        ));
        filter.setDataSetListLabels(getLabelIds(
                filter.getId(), FILTER_DSL_LABELS.dslLabelId, FILTER_DSL_LABELS, FILTER_DSL_LABELS.filterId
        ));
    }

    private List<UUID> getLabelIds(
            UUID filterId, SimplePath<UUID> labelPth, RelationalPathBase fromPath, SimplePath<UUID> whereClause
    ) {
        return queryFactory
                .select(labelPth)
                .from(fromPath)
                .where(whereClause.eq(filterId))
                .fetch();
    }

    /**
     * Update filter. Method will create new references to label and delete old references.
     *
     * @param filterId  - target filter id {@link UUID}
     * @param name      - filter name
     * @param dsLabels  - references to {@link DataSet} labels
     * @param dslLabels - references to {@link DataSetList} labels
     */
    public void update(UUID filterId, String name, List<UUID> dsLabels, List<UUID> dslLabels) {
        queryFactory.update(FILTERS).set(FILTERS.name, name).where(FILTERS.id.eq(filterId)).execute();
        removeOldLabelReferences(filterId, dsLabels,
                FILTER_DS_LABELS.filterId, FILTER_DS_LABELS.dsLabelId, FILTER_DS_LABELS);
        removeOldLabelReferences(filterId, dslLabels,
                FILTER_DSL_LABELS.filterId, FILTER_DSL_LABELS.dslLabelId, FILTER_DSL_LABELS);
        updateLabels(filterId, dslLabels, FILTER_DSL_LABELS.filterId, FILTER_DSL_LABELS, FILTER_DSL_LABELS.dslLabelId);
        updateLabels(filterId, dsLabels, FILTER_DS_LABELS.filterId, FILTER_DS_LABELS, FILTER_DS_LABELS.dsLabelId);
    }

    private void updateLabels(
            UUID filterId,
            List<UUID> labels,
            SimplePath<UUID> filterIdPath,
            RelationalPathBase path,
            SimplePath<UUID> pathLabelId
    ) {
        List<UUID> existingList = selectAllExisting(filterId, labels, filterIdPath, path, pathLabelId);
        Set<UUID> labelsToUpdate = Sets.newHashSetWithExpectedSize(labels.size() - existingList.size());
        //Find all labels which already present for this filter.
        existingList.forEach(label -> {
            if (labels.contains(label)) {
                labelsToUpdate.add(label);
            }
        });
        labelsToUpdate.forEach(label -> queryFactory
                .insert(path)
                .set(filterIdPath, filterId)
                .set(pathLabelId, label).execute());
    }

    private List<UUID> selectAllExisting(
            UUID filterId,
            List<UUID> dslLabels,
            SimplePath<UUID> filterIdPath,
            RelationalPathBase path,
            SimplePath<UUID> pathLabelId
    ) {
        return queryFactory
                .select(pathLabelId).from(path).where(filterIdPath.eq(filterId).and(pathLabelId.in(dslLabels)))
                .fetch();
    }

    private void removeOldLabelReferences(
            UUID filterId, List<UUID> dsLabels,
            SimplePath<UUID> filterIdPath, SimplePath<UUID> labelPath,
            RelationalPathBase labelContainer
    ) {
        List<UUID> dsLabelsToRemove = queryFactory
                .select(labelPath)
                .from(labelContainer)
                .where(filterIdPath.eq(filterId).and(labelPath.notIn(dsLabels))).fetch();
        queryFactory.delete(labelContainer).where(labelPath.in(dsLabelsToRemove)).execute();
    }

    /**
     * Retrieve all filters under visibility area.
     *
     * @param vaId visibility area id
     * @return collection of filters under
     */
    public List<Filter> getAll(UUID vaId) {
        List<Filter> filters = queryFactory.select(filterProjection).from(FILTERS).where(FILTERS.vaId.eq(vaId)).fetch();
        filters.forEach(this::fillLabels);
        return filters;
    }

    private class FilterProjection extends MappingProjection<Filter> {

        FilterProjection() {
            super(Filter.class, FILTERS.all());
        }

        @Override
        protected Filter map(Tuple row) {
            UUID id = row.get(FILTERS.id);
            String name = row.get(FILTERS.name);
            return new FilterImpl(id, name);
        }
    }
}
