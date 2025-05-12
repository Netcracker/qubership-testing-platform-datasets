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
import java.util.UUID;

import javax.annotation.Nullable;

import org.json.simple.JSONObject;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;
import org.qubership.atp.dataset.service.jpa.model.dscontext.DataSetListContext;
import org.qubership.atp.dataset.service.jpa.model.tree.ds.DataSetTree;
import org.qubership.atp.dataset.service.rest.dto.manager.AbstractEntityResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface JpaDataSetService {
    DataSetTree getDataSetTreeInAtpFormat(UUID dataSetId, boolean evaluate, String atpContext, ContextType contextType);

    List<JSONObject> getDataSetTreeInAtpFormat(
            UUID dataSetId, boolean evaluate, String atpContext, ContextType contextType, Integer numberOfCopies);

    void remove(UUID id);

    DataSetListContext getDatasetListContext(UUID dataSetId);

    String getDataSetTreeInItfFormat(UUID dataSetId);

    @Transactional
    DataSet create(String name, UUID dataSetListId);

    @Transactional
    DataSet createDsSelectJavers(String name, UUID dataSetListId, boolean isJavers) throws DataSetServiceException;

    DataSet replicate(UUID id, String name, UUID dataSetListId, Long ordering, UUID sourceId, Boolean isLocked)
            throws DataSetServiceException;

    @Transactional
    Parameter setParameterValue(ParameterValue value, UUID dataSetId, UUID attributeId) throws DataSetServiceException;

    @Transactional
    void lock(UUID dataSetListId, List<UUID> datasetIds, boolean isLock);

    @Transactional
    void setOverlapValue(
            ParameterValue value,
            UUID dataSetId,
            UUID attributeId,
            List<UUID> attributePath
    ) throws DataSetServiceException;

    Page<AbstractEntityResponse> getDatasetsIdNamesPageByNameAndVaId(String name, UUID visibilityAreaId,
                                                                     Pageable pageable);

    DataSet getById(UUID id);

    List<DataSet> getByIds(Collection<UUID> ids);

    void save(DataSet dataSet);

    void setPosition(UUID dataSetId, Integer position);

    List<DataSet> getByNameAndDataSetListId(String name, UUID dataSetList);

    List<DataSet> getByDataSetListId(UUID dataSetList);

    List<DataSet> getLockedDataSets(UUID dataSetList);

    List<DataSet> getByDataSetListIdIn(Collection<UUID> dataSetListIds);

    List<DataSet> getBySourceAndDataSetListId(UUID sourceId, UUID dataSetList);

    /**
     * Copy value from source ds to target ds by attr id.
     * If source attribute id and target attribute id are null then do nothing
     * @param sourceDsId source dataset id
     * @param targetDsId target dataset id
     * @param sourceAttrId source attribute id, may be null
     * @param targetAttrId target attribute id, may be null
     * @return targetAttributeId
     */
    UUID copyDsAttributeValue(UUID sourceDsId, UUID targetDsId,
                              @Nullable UUID sourceAttrId, @Nullable UUID targetAttrId);

    /**
     * Copy values from source ds to target ds.
     * @param sourceDsId source dataset id
     * @param targetDsId target dataset id
     */
    void copyDsAttributeValueBulk(UUID sourceDsId, UUID targetDsId);
}
