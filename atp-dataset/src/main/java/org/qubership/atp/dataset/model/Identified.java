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

package org.qubership.atp.dataset.model;

import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.service.rest.View;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;

public interface Identified {

    @JsonView({View.IdName.class, View.IdNameLabelsTestPlan.class})
    @Schema(required = true)
    UUID getId();

    void setId(UUID id);

    @Nonnull
    @JsonIgnore
    Stream<Identified> getReferences();
}
