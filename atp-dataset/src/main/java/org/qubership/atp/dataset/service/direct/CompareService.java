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

import java.util.UUID;

import org.qubership.atp.dataset.model.api.DetailedComparisonDsRequest;
import org.qubership.atp.dataset.model.api.DetailedComparisonDsResponse;
import org.qubership.atp.dataset.model.enums.CompareStatus;
import org.qubership.atp.dataset.model.enums.DetailedComparisonStatus;

public interface CompareService {

    CompareStatus compare(UUID leftId, UUID rightId);

    DetailedComparisonDsResponse detailedComparison(DetailedComparisonDsRequest request);

    DetailedComparisonStatus compareAttributeValues(UUID leftDatasetId, UUID rightDataSetId,
                                                    UUID leftAttributeId, UUID rightAttributeId);

    /**
     * Checks that two attributes can be compared.
     * @param leftAttrId left attribute id
     * @param rightAttrId right attribute id
     * @return true if comparable, otherwise false
     */
    boolean isAttributesComparable(UUID leftAttrId, UUID rightAttrId);

}
