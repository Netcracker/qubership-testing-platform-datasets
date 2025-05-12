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

package org.qubership.atp.dataset.service.direct.impl;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.qubership.atp.dataset.db.FilterRepository;
import org.qubership.atp.dataset.model.Filter;
import org.qubership.atp.dataset.service.direct.FilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Strings;

@Service
public class FilterServiceImpl implements FilterService {

    private FilterRepository filterRepository;

    @Autowired
    public void setFilterRepository(FilterRepository filterRepository) {
        this.filterRepository = filterRepository;
    }

    @Override
    @Transactional
    public Filter create(
            @Nonnull String name,
            @Nonnull UUID vaId,
            @Nonnull List<UUID> dsLabels,
            @Nonnull List<UUID> dslLabels
    ) {
        if (dsLabels.isEmpty() && dslLabels.isEmpty()) {
            throw new IllegalArgumentException("Filter can't be created with empty references to ds/dsl labels");
        }
        return filterRepository.create(name, vaId, dsLabels, dslLabels);
    }

    @Override
    @Transactional
    public void delete(UUID filterId) {
        filterRepository.delete(filterId);
    }

    @Override
    public void update(UUID filterId, String name, List<UUID> dsLabels, List<UUID> dslLabels) {
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("Filter name can't be null or empty!");
        }
        filterRepository.update(filterId, name, dsLabels, dslLabels);
    }

    @Nullable
    @Override
    public Filter get(@Nonnull UUID filterId) {
        return filterRepository.get(filterId);
    }

    @Override
    public boolean existsById(@NotNull UUID id) {
        return filterRepository.existsById(id);
    }

    /**
     * Unsupported we must not have an ability to get ALL filters, only under VA. use {@link
     * #getAll(UUID)}
     */
    @Nonnull
    @Override
    @Deprecated
    public List<Filter> getAll() {
        throw new UnsupportedOperationException("Use method getAll(DSL_ID)");
    }

    @Override
    public List<Filter> getAll(UUID vaId) {
        return filterRepository.getAll(vaId);
    }
}
