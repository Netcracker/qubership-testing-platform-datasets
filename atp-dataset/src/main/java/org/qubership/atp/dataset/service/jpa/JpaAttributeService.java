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

package org.qubership.atp.dataset.service.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.qubership.atp.dataset.db.jpa.entities.ListValueEntity;
import org.qubership.atp.dataset.ei.model.AttributeKeyIdsDbUpdate;
import org.qubership.atp.dataset.service.jpa.delegates.AbstractObjectWrapper;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.AttributeKey;
import org.qubership.atp.dataset.service.jpa.delegates.ListValue;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;

public interface JpaAttributeService {

    Attribute create(String name, AttributeTypeName type, UUID dataSetListId);

    Attribute create(String name, AttributeTypeName type, UUID dataSetListId,
                     UUID typeDataSetListId, List<String> listValues);

    Attribute createWithoutCommitting(String name, AttributeTypeName type, UUID dataSetListId);

    Attribute replicate(UUID id, String name, AttributeTypeName type, UUID dataSetListId, UUID sourceId)
            throws DataSetServiceException;

    ListValue createListValue(String text, UUID attributeId);

    ListValue createListValueWithoutCommitting(String text, UUID attributeId);

    ListValue replicateListValue(UUID id, String text, UUID attributeId, UUID sourceId)
            throws DataSetServiceException;

    void setReferencedDataSetList(UUID attributeId, UUID dataSetListId);

    void remove(UUID id);

    void save(AbstractObjectWrapper attribute);

    void removeAttributeKey(UUID id);

    AttributeKey replicateAttributeKey(UUID id, String key, UUID attribute, UUID dataSet, UUID dataSetList,
                                       UUID sourceId) throws DataSetServiceException;

    Attribute getById(UUID id);

    AttributeKey getAttributeKeyById(UUID id);

    List<AttributeKey> getAttributeKeysByDatasetListId(UUID dataSetListId);

    void removeListValuesByAttributeId(UUID id);

    ListValue getListValueById(UUID id);

    ListValue getListValueByAttributeIdAndValue(UUID attributeId, String value);

    List<ListValue> getListValuesByAttributeId(UUID attributeId);

    void setPosition(UUID attributeId, Integer position);

    List<Attribute> getByNameAndDataSetListId(String name, UUID dataSetListId);

    List<Attribute> getByDataSetListId(UUID dataSetListId);

    List<Attribute> getByDataSetListIdIn(Collection<UUID> dataSetListIds);

    List<Attribute> getBySourceIdAndDataSetListId(UUID sourceId, UUID dataSetListId);

    List<AttributeKey> getAttrKeyBySourceIdAndDataSetListId(UUID sourceId, UUID dataSetListId);

    AttributeKey getAttributeKeyByKeyAndDataSetListIdAndDataSetIdAndAttributeId(String key,
                                                                                UUID dataSetListId,
                                                                                UUID datasetId,
                                                                                UUID attributeId);

    List<ListValue> getListValueBySourceIdAndAttrId(UUID sourceId, UUID attributeId);

    Map<UUID, AttributeKeyIdsDbUpdate> getFoundedAttributeKeyIdAndDatasetIdUpdate();

    /**
     * Merge values from the source ListValue to the target ListValue.
     * @param sourceAttrId source attribute id
     * @param targetAttrId target attribute id
     * @param sourceListValue value in source ListValue
     * @return Returns the Id of the ListValue from the result list
     *     whose value matches the sourceListValue. If sourceListValue is null return null
     */
    UUID mergeListValuesAndGetListValueReference(UUID sourceAttrId, UUID targetAttrId,
                                                 @Nullable ListValueEntity sourceListValue);

    /**
     * Merge values from the source ListValue to the right ListValue.
     * @param sourceAttributeId source Attribute Id
     * @param targetAttributeId target Attribute Id
     */
    void mergeListValues(UUID sourceAttributeId, UUID targetAttributeId);
}
