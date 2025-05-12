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

package org.qubership.atp.dataset.service.direct.importexport.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class DatasetListImportResponse {
    private List<AttributeImportResponse> attributes = new ArrayList<>();

    /**
     * Gets attribute import response for provided import model.
     */
    public AttributeImportResponse getAttributeResponse(AttributeImportModel importModel) {
       final String attributeName = importModel.getName();

        return attributes.stream()
                .filter(attributeImportResponse -> attributeImportResponse.getAttributeName().equals(attributeName))
                .findFirst()
                .orElseGet(() -> addAttributeResponse(importModel));
    }

    /**
     * DatasetListImportResponse constructor.
     */
    public AttributeImportResponse addAttributeResponse(AttributeImportModel importModel) {
        final UUID attributeId = importModel.getId();
        final String attributeName = importModel.getName();
        final AttributeImportResponse attributeImportResponse = new AttributeImportResponse(attributeId, attributeName);
        attributes.add(attributeImportResponse);

        return attributeImportResponse;
    }
}
