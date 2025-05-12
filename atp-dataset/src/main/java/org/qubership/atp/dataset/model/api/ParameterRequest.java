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

package org.qubership.atp.dataset.model.api;

import java.util.List;
import java.util.UUID;

import brave.internal.Nullable;
import lombok.Data;

@Data
public class ParameterRequest {

    private Long modifiedWhen;
    @Nullable
    private String value;
    @Nullable
    private UUID dataSetReference;
    @Nullable
    private UUID listValueReference;
    @Nullable
    private UUID dataSetListId;
    private UUID dataSetId;
    @Nullable
    private List<UUID> attrPathIds;
    @Nullable
    private List<UUID> listIdsParametersToChange;
    @Nullable
    private List<UUID> dataSetsIds;
}
