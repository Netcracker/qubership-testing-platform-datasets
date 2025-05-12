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

import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.model.Label;

public interface LabelProviderService {

    /**
     * Adds label.
     *
     * @param id   - id of target dataSetList or dataSet.
     * @param name - name of label.
     */
    Label mark(@Nonnull UUID id, @Nonnull String name);

    List<Label> getLabels(@Nonnull UUID id);

    /**
     * Deletes label.
     *
     * @param id - id of target dataSetList or dataSet.
     */
    boolean unmark(@Nonnull UUID id, @Nonnull UUID labelId);
}
