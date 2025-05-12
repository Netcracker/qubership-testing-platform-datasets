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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Provider;

import org.qubership.atp.dataset.db.dto.AttributePathDto;
import org.qubership.atp.dataset.db.generated.QAttributeKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.MappingProjection;
import com.querydsl.core.types.Predicate;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;

@Repository
public class AttributePathRepository extends AbstractRepository {

    private static final AttributePathProjection PROJECTION = new AttributePathProjection();
    private final Provider<ParameterRepository> paramRepo;
    private final SQLQueryFactory queryFactory;

    public static final Splitter SPLITTER = Splitter.on("_");
    public static final Joiner JOINER = Joiner.on("_");

    /**
     * AttributePathRepository.
     *
     * @param queryFactory is used for creating sql queries.
     */
    @Autowired
    public AttributePathRepository(@Nonnull Provider<ParameterRepository> paramRepo,
                                   @Nonnull SQLQueryFactory queryFactory) {
        super();
        this.paramRepo = paramRepo;
        this.queryFactory = queryFactory;
    }

    /**
     * Creates parameter key.
     */
    @Nonnull
    AttributePathDto create(UUID dataSetListId, UUID dataSetId, UUID targetAttributeId, List<UUID> attributePathIds) {
        String attrKey = JOINER.join(attributePathIds);
        UUID id = Preconditions.checkNotNull(
                queryFactory.insert(AK)
                        .set(AK.key, attrKey)
                        .set(AK.datasetlistId, dataSetListId)
                        .set(AK.datasetId, dataSetId)
                        .set(AK.attributeId, targetAttributeId)
                        .executeWithKey(AK.id),
                "nothing created");
        return new AttributePathDto(id, dataSetListId, dataSetId, targetAttributeId, attributePathIds);
    }

    void delete(UUID dataSetListId, UUID dataSetId, UUID targetAttributeId, List<UUID> attributePathIds) {
        String attrKey = JOINER.join(attributePathIds);
        for (UUID akId : queryFactory.select(AK.id).from(AK)
                .where(AK.key.eq(attrKey)
                        .and(AK.datasetId.eq(dataSetId))
                        .and(AK.attributeId.eq(targetAttributeId)))
                .fetch()) {
            //should be one result, but since we have no unique constraints on db, there may be many results.
            delete(akId);
        }
    }

    boolean delete(@Nonnull UUID id) {
        paramRepo.get().onAttrPathDeleteCascade(id);
        return delete(AK.id.eq(id)) > 0;
    }

    private long delete(@Nonnull Predicate predicate) {
        return queryFactory.delete(AK).where(predicate).execute();
    }

    @Nullable
    AttributePathDto get(UUID dataSetListId, UUID dataSetId, UUID targetAttributeId, List<UUID> attributePathIds) {
        String attrKey = JOINER.join(attributePathIds);
        UUID id = queryFactory.select(AK.id).from(AK)
                .where(AK.key.eq(attrKey)
                        .and(AK.datasetId.eq(dataSetId))
                        .and(AK.attributeId.eq(targetAttributeId)))
                .fetchOne();
        if (id == null) {
            return null;
        }
        return new AttributePathDto(id, dataSetListId, dataSetId, targetAttributeId, attributePathIds);
    }

    @Nullable
    AttributePathDto getById(@Nonnull UUID id) {
        return select(AK.id.eq(id)).fetchOne();
    }

    @Nullable
    List<AttributePathDto> getByTargetAttrId(@Nonnull UUID targetAttrId) {
        return select(AK.attributeId.eq(targetAttrId)).fetch();
    }

    void onAttrDeleteCascade(@Nonnull UUID attributeId) {
        getAllWithAttrId(attributeId).forEach(dto -> {
            UUID id = dto.getId();
            delete(id);
        });
    }

    /**
     * Since attr_key consists of a bunch of ids, you can find all keys related to any of id.
     */
    private List<AttributePathDto> getAllWithAttrId(UUID id) {
        String pattern = "%" + id + "%";
        return select(AK.attributeId.eq(id).or(AK.key.like(pattern))).fetch();
    }

    @Nonnull
    private SQLQuery<AttributePathDto> select(@Nonnull Predicate predicate) {
        return queryFactory.select(PROJECTION).from(AK).where(predicate);
    }

    /**
     * attribute_key can be without parameter, and needs to be deleted.
     */
    public void onDsDeleteCascade(UUID dataSetId) {
        for (UUID attributeKey : queryFactory.select(AK.id).from(AK)
                .where(AK.datasetId.eq(dataSetId))
                .fetch()) {
            delete(attributeKey);
        }
    }

    protected static class AttributePathProjection extends MappingProjection<AttributePathDto> {

        AttributePathProjection() {
            super(AttributePathDto.class, AK.all());
        }

        @Nonnull
        protected static AttributePathDto createDto(@Nonnull QAttributeKey ak, @Nonnull Tuple row) {
            UUID id = row.get(ak.id);
            assert id != null;
            String key = row.get(ak.key);
            assert key != null;
            UUID dslId = row.get(ak.datasetlistId);
            assert dslId != null;
            UUID dsId = row.get(ak.datasetId);
            assert dsId != null;
            UUID targetAttrId = row.get(ak.attributeId);
            assert targetAttrId != null;
            return createDto(id, dslId, dsId, targetAttrId, key);
        }

        protected static AttributePathDto createDto(@Nonnull UUID id,
                                                    @Nonnull UUID dslId,
                                                    @Nonnull UUID dsId,
                                                    @Nonnull UUID targetAttrId,
                                                    @Nonnull String attrKey) {
            List<UUID> parts = StreamSupport.stream(SPLITTER.split(attrKey).spliterator(), false)
                    .map(UUID::fromString)
                    .collect(Collectors.toList());
            return new AttributePathDto(id, dslId, dsId, targetAttrId, parts);
        }

        @Override
        protected AttributePathDto map(Tuple row) {
            return createDto(AK, row);
        }
    }
}
