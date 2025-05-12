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

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.model.DataSetListDependencyNode;
import org.qubership.atp.dataset.service.jpa.model.dscontext.DataSetListContext;
import org.qubership.atp.dataset.service.jpa.model.dsllazyload.dsl.DataSetListFlat;
import org.qubership.atp.dataset.service.jpa.model.dsllazyload.referencedcontext.RefDataSetListFlat;
import org.qubership.atp.dataset.service.rest.server.CopyDataSetListsResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface JpaDataSetListService {
    DataSetListFlat getDataSetListFlat(UUID dataSetListId);

    @Transactional
    List<CopyDataSetListsResponse> copyDataSetLists(List<UUID> dataSetListIds, boolean updateReferences,
                                                    UUID targetVisibilityAreaId, @Nullable String postfix,
                                                    String prevNamePattern, @Nullable UUID sagaSessionId);

    List<CopyDataSetListsResponse> copyDataSetLists(List<UUID> dataSetListIds, boolean updateReferences,
                                                    @Nullable String postfix, String prevNamePattern);

    @Transactional
    DataSetList getById(UUID dataSetListId);

    void remove(UUID dataSetListId);

    @Transactional
    DataSetList create(String name, UUID visibilityAreaId);

    DataSetList replicate(UUID id, String name, UUID visibilityAreaId, UUID sourceId,
                          UUID createdBy, Timestamp createdWhen, UUID modifiedBy,
                          Timestamp modifiedWhen) throws DataSetServiceException;

    RefDataSetListFlat getReferencedDataSetListFlat(UUID dataSetListId,
                                                    UUID dataSetListAttributeId,
                                                    String attributePath,
                                                    List<UUID> dataSetIds,
                                                    Pageable pageable);

    RefDataSetListFlat getReferencedDataSetListFlatRows(UUID dataSetListId,
                                                        UUID dataSetListAttributeId,
                                                        String attributePath,
                                                        List<UUID> dataSetIds,
                                                        Pageable pageable);

    DataSetListContext getDataSetListContext(UUID dataSetListId, List<UUID> dataSetsIds, Pageable pageable);

    DataSetListContext getDataSetListContext(UUID dataSetListId, List<UUID> dataSets);

    void save(DataSetList dataSetList);

    List<DataSetListDependencyNode> getDependencies(List<UUID> dataSetListIds);

    List<DataSetListDependencyNode> getDependenciesRecursive(List<UUID> dataSetListIds);

    List<DataSetList> getByNameAndVisibilityAreaId(String name, UUID visibilityArea);

    List<DataSetList> getByVisibilityAreaId(UUID visibilityAreaId);

    List<DataSetList> getBySourceIdAndVisibilityAreaId(UUID sourceId, UUID visibilityAreaId);

    Timestamp getModifiedWhen(UUID dataSetListId);

    LinkedList<UUID> getDataSetsIdsByDataSetListId(UUID dataSetListId);

    void checkDslNames(UUID visibilityAreaId);
}
