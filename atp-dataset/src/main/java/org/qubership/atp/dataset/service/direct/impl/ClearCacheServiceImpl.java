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

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.dataset.constants.CacheEnum;
import org.qubership.atp.dataset.service.direct.ClearCacheService;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ClearCacheServiceImpl implements ClearCacheService {

    private final CacheManager cacheManager;

    public ClearCacheServiceImpl(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void evictParameterCache(UUID parameterId) {
        log.info("Remove parameter - \"{}\" from Parameter cache", parameterId);
        cacheManager.getCache(CacheEnum.Constants.PARAMETER_CACHE).evict(parameterId);
    }

    @Override
    public void evictDatasetListContextCache(UUID datasetId) {
        log.info("Remove dataset - \"{}\" from DatasetListContext cache", datasetId);
        cacheManager.getCache(CacheEnum.Constants.DATASET_LIST_CONTEXT_CACHE).evict(datasetId);
    }

    @Override
    public void evictDatasetListContextCache(Set<UUID> datasetIds) {
        log.info("Remove datasets - \"{}\" from DatasetListContext cache", datasetIds);
        Cache contextCache = cacheManager.getCache(CacheEnum.Constants.DATASET_LIST_CONTEXT_CACHE);
        if (Objects.nonNull(contextCache)) {
            datasetIds.forEach(contextCache::evict);
        }
    }
}
