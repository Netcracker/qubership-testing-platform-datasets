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

package org.qubership.atp.dataset.service.direct;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.db.jpa.entities.UserSettingsEntity;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.utils.Utils;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManDataSetList;
import org.qubership.atp.dataset.service.ws.entities.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public interface AttributeService extends IdentifiedService<Attribute> {

    @Nonnull
    Attribute create(@Nonnull UUID dslId, @Nonnull Integer order, @Nonnull String name,
                     @Nonnull AttributeType type, @Nullable UUID dslRefId, @Nullable List<String> listValues);

    boolean updateTypeDslId(@Nonnull UUID dslId, UUID id);

    boolean update(@Nonnull UUID id, @Nonnull String name);

    boolean updateDslReference(@Nonnull UUID id, @Nonnull UUID dslReferenceId);

    @Nullable
    Object getOptions(@Nonnull UUID id);

    /**
     * Returns all attributes by dataSetList id.
     *
     * @return collection of attributes by dataSetList id.
     */
    @Nullable
    Collection<Attribute> getByDslId(@Nonnull UUID dslId);

    /**
     * See {@link Utils#serializeAttrInItfWay(DataSetList, ObjectMapper)}.
     */
    @Nullable
    ArrayNode getByDslIdInItfFormat(@Nonnull UUID dslId);

    boolean delete(@Nonnull UUID id);

    /**
     * Creates list value.
     */
    @Nonnull
    ListValue createListValue(@Nonnull UUID attributeId, @Nonnull String value);

    /**
     * Bulk create list values.
     */
    @Nonnull
    List<UUID> createListValues(@Nonnull UUID attributeId, @Nonnull List<String> values);

    /**
     * Deletes list value from attribute.
     */
    boolean deleteListValue(UUID attributeId, @Nonnull UUID id);

    /**
     * Bulk delete list values from attribute.
     */
    boolean deleteListValues(UUID attributeId, @Nonnull List<UUID> ids);

    Attribute copy(@Nonnull DataSetList newParentDsl, @Nonnull Attribute attribute, int ordering);

    void updateOrdering(@Nonnull List<Pair<UUID, Integer>> attributesOrdering);

    void saveAttributeSortConfigurationForUser(@Nonnull UUID userId, boolean isSortEnabled);

    UserSettingsEntity getAttributeSortConfigurationForUser(@Nonnull UUID userId);

    Map<String, List<UUID>> getParametersAndDataSetIdsForAttributeSorting(
            @Nonnull UiManDataSetList tree,  @Nonnull UUID dataSetListId,
            @Nonnull UUID targetAttrId, @Nonnull List<UUID> attrFilterIds);

}
