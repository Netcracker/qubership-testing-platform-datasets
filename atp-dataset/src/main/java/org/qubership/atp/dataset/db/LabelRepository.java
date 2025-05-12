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
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.model.Label;
import org.qubership.atp.dataset.model.impl.LabelImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.common.base.Preconditions;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.MappingProjection;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.SimplePath;
import com.querydsl.sql.RelationalPathBase;
import com.querydsl.sql.SQLQueryFactory;

@Repository
public class LabelRepository extends AbstractRepository {

    private final LabelProjection projection;
    private final SQLQueryFactory queryFactory;

    /**
     * Label repository.
     *
     * @param queryFactory is used for creating sql queries.
     */
    @Autowired
    public LabelRepository(@Nonnull SQLQueryFactory queryFactory) {
        super();
        this.queryFactory = queryFactory;
        this.projection = new LabelProjection();
    }

    /**
     * Gets label by name.
     *
     * @param name of label
     */
    @Nonnull
    private Label getByNameOrCreate(String name) {
        Label label = getLabelByName(name);
        if (label != null) {
            return label;
        }
        //will create one
        UUID id = queryFactory
                .insert(LABEL)
                .set(LABEL.name, name)
                .executeWithKey(LABEL.id);
        Preconditions.checkNotNull(id, "nothing created");
        return new LabelImpl(id, name);
    }

    /**
     * Adds DataSetListLabel.
     *
     * @param dataSetListId {@link UUID}
     * @param labelName     {@link String}
     * @return {@link Label}
     */
    @Nonnull
    Label markDsl(@Nonnull UUID dataSetListId, @Nonnull String labelName) {
        return mark(DSLLABEL, DSLLABEL.datasetlistId, dataSetListId, DSLLABEL.labelId, labelName);
    }

    /**
     * Adds DataSetLabel.
     *
     * @param dataSetId {@link UUID}
     * @param labelName {@link String}
     * @return {@link Label}
     */
    @Nonnull
    Label markDs(@Nonnull UUID dataSetId, @Nonnull String labelName) {
        return mark(DSLABEL, DSLABEL.datasetId, dataSetId, DSLABEL.labelId, labelName);
    }

    /**
     * Gets DataSetListLabel by Id.
     *
     * @param dslId id of DataSetList
     */
    @Nonnull
    List<Label> getLabelsOfDsl(@Nonnull UUID dslId) {
        return getLabels(DSLLABEL, DSLLABEL.labelId, DSLLABEL.datasetlistId.eq(dslId));
    }

    /**
     * Returns a label with name. If found.
     */
    @Nullable
    public Label getLabelByName(@Nonnull String name) {
        return queryFactory
                .select(projection)
                .from(LABEL)
                .where(LABEL.name.eq(name))
                .fetchOne();
    }

    /**
     * Gets DataSetLabel by Id.
     *
     * @param dsId id of DataSet
     */
    @Nonnull
    List<Label> getLabelsOfDs(@Nonnull UUID dsId) {
        return getLabels(DSLABEL, DSLABEL.labelId, DSLABEL.datasetId.eq(dsId));
    }

    /**
     * Deletes label by dataSetListId and labelId.
     */
    boolean unmarkDsl(@Nonnull UUID dslId, @Nonnull UUID labelId) {
        return delete(DSLLABEL, DSLLABEL.datasetlistId.eq(dslId).and(DSLLABEL.labelId.eq(labelId))) > 0;
    }

    /**
     * Deletes label by dataSetId and labelId.
     */
    boolean unmarkDs(@Nonnull UUID dsId, @Nonnull UUID labelId) {
        return delete(DSLABEL, DSLABEL.datasetId.eq(dsId).and(DSLABEL.labelId.eq(labelId))) > 0;
    }

    void onDslDeleteCascade(@Nonnull UUID dslId) {
        delete(DSLLABEL, DSLLABEL.datasetlistId.eq(dslId));
    }

    void onDsDeleteCascade(@Nonnull UUID dsId) {
        delete(DSLABEL, DSLABEL.datasetId.eq(dsId));
    }

    /**
     * Deletes object.
     */
    private long delete(@Nonnull RelationalPathBase table, @Nonnull Predicate predicate) {
        return queryFactory.delete(table).where(predicate).execute();
    }

    private Label mark(@Nonnull RelationalPathBase table, @Nonnull SimplePath<UUID> idColumn,
                       @Nonnull UUID id, @Nonnull SimplePath<UUID> labelColumn,
                       @Nonnull String labelName) {
        Label label = getByNameOrCreate(labelName);
        queryFactory.insert(table).set(idColumn, id).set(labelColumn, label.getId()).execute();
        return label;
    }

    BooleanExpression dslByLabelName(@Nonnull String labelName) {
        return DSL.id.in(
                queryFactory.select(DSLLABEL.datasetlistId)
                        .from(DSLLABEL)
                        .where(DSLLABEL.labelId.eq(
                                queryFactory.select(LABEL.id)
                                        .from(LABEL)
                                        .where(LABEL.name.eq(labelName)))
                        )
        );
    }

    BooleanExpression dsByLabelName(@Nonnull String labelName) {
        return DS.id.in(
                queryFactory.select(DSLABEL.datasetId)
                        .from(DSLABEL)
                        .where(DSLABEL.labelId.eq(
                                queryFactory.select(LABEL.id)
                                        .from(LABEL)
                                        .where(LABEL.name.eq(labelName)))
                        )
        );
    }

    private List<Label> getLabels(@Nonnull RelationalPathBase table, @Nonnull SimplePath<UUID> labelColumn,
                                  @Nonnull Predicate predicate) {
        return queryFactory
                .select(projection)
                .from(LABEL)
                .where(LABEL.id.in(queryFactory.select(labelColumn)
                        .from(table)
                        .where(predicate).fetch()))
                .fetch();
    }

    private class LabelProjection extends MappingProjection<Label> {

        LabelProjection() {
            super(Label.class, LABEL.all());
        }

        @Override
        protected LabelImpl map(Tuple row) {
            UUID id = row.get(LABEL.id);
            assert id != null;
            String name = row.get(LABEL.name);
            assert name != null;
            return new LabelImpl(id, name);
        }
    }
}