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

import java.util.List;
import java.util.UUID;

import org.qubership.atp.dataset.service.jpa.delegates.Parameter;

public interface JpaParameterService {
    Parameter createParameter(UUID dataSetId, UUID attributeId);

    Parameter createParameter(UUID dataSetId, UUID attributeId, String value,
                              UUID dataSetReference, UUID listValueReference);

    Parameter createParameterWithoutCommitting(UUID dataSetId, UUID attributeId, String value,
                                               UUID dataSetReference, UUID listValueReference);

    Parameter createParameterOverlapWithoutCommitting(UUID attributeId, List<UUID> attributePath, UUID dataSetId,
                                                      String value, UUID dataSetReference, UUID listValueReference);

    Parameter replicate(UUID id, UUID dataSetId, UUID attributeId, UUID sourceId) throws DataSetServiceException;

    void remove(UUID id);

    void removeWithoutCommitting(UUID id);

    void save(Parameter parameter);

    Parameter getById(UUID id);

    Parameter getParameterByAttributeIdAndDataSetId(UUID attributeId, UUID dataSetId);

    Parameter updateParameter(UUID parameterId, String value, UUID dataSetReference, UUID listValueReference);

    Parameter updateParameterWithoutCommitting(UUID parameterId, String value,
                                               UUID dataSetReference, UUID listValueReference);

    void updateOverlapParameterWithoutCommitting(Parameter parameter, String value,
                                                      UUID dataSetReference, UUID listValueReference);

    boolean bulkUpdateValues(String stringValue, UUID dataSetReference, UUID listValueReference,
                             UUID dataSetListId, UUID dataSetId, List<UUID> listIdsParametersToChange);

    List<Parameter> getBySourceIdAndDataSetId(UUID sourceId, UUID dataSetId);
}
