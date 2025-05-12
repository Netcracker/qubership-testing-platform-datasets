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
import org.qubership.atp.dataset.db.VisibilityAreaRepository;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.service.direct.VisibilityAreaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisibilityAreaServiceImpl implements VisibilityAreaService {

    protected VisibilityAreaRepository repo;

    @Autowired
    public void setRepo(VisibilityAreaRepository repo) {
        this.repo = repo;
    }

    @Nonnull
    @Transactional
    public VisibilityArea create(@Nonnull String name) {
        return repo.create(name);
    }

    @Nonnull
    @Transactional
    public VisibilityArea create(@Nonnull UUID id, @Nonnull String name) {
        return repo.create(id, name);
    }

    @Nullable
    @Override
    public VisibilityArea get(@Nonnull UUID id) {
        return repo.getById(id);
    }

    @Override
    public boolean existsById(@NotNull UUID id) {
        return repo.existsById(id);
    }

    @Nonnull
    public List<VisibilityArea> getAll() {
        return repo.getAll();
    }

    @Nonnull
    @Override
    public List<VisibilityArea> getAllSorted() {
        return repo.getAllSorted();
    }

    @Transactional
    public boolean rename(@Nonnull UUID id, @Nonnull String name) {
        return repo.rename(id, name);
    }

    @Transactional
    public void delete(@Nonnull UUID id) {
        repo.delete(id);
    }
}
