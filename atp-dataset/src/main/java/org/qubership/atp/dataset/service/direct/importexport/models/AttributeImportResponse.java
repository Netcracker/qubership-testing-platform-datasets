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
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.Data;

@Data
public class AttributeImportResponse {
    private UUID attributeId;
    private String attributeName;
    private List<ParameterImportResponse> success = new ArrayList<>();
    private List<ParameterImportResponse> errors = new ArrayList<>();

    public AttributeImportResponse(UUID attributeId, String attributeName) {
        this.attributeId = attributeId;
        this.attributeName = attributeName;
    }

    public void setErrors(Collection<ParameterImportResponse> errors) {
        this.errors.addAll(errors);
    }

    @Hidden
    public void setError(ParameterImportResponse error) {
        this.errors.add(error);
    }

    public void setSuccess(ParameterImportResponse success) {
        this.success.add(success);
    }
}
