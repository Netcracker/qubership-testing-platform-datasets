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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.qubership.atp.dataset.db.ListValueRepository;
import org.qubership.atp.dataset.model.ListValue;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class ListValueServiceImpl implements ListValueService {

    private final ListValueRepository listValueRepository;

    @Nullable
    @Override
    public ListValue get(@NotNull UUID id) {
        return listValueRepository.getById(id);
    }

    @Override
    public boolean existsById(@NotNull UUID id) {
        return listValueRepository.existsById(id);
    }

    @NotNull
    @Override
    public List<ListValue> getAll() {
        return listValueRepository.getAll();
    }
}
