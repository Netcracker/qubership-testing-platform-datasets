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

package org.qubership.atp.dataset.ei.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.dataset.ei.model.IdEntity;
import org.springframework.stereotype.Service;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DuplicateNameChecker {

    private ThreadLocal<Map<String, Multimap<String, UUID>>> existingNamesCache
            = ThreadLocal.withInitial(HashMap::new);

    public void init(Class clazz, UUID parentId, Multimap<String, UUID> map) {
        String key = getKey(clazz, parentId);
        existingNamesCache.get().put(key, HashMultimap.create(map));
    }

    public boolean isInitialized(Class clazz, UUID parentId) {
        String key = getKey(clazz, parentId);
        return existingNamesCache.get().containsKey(key);
    }

    /**
     * Check that name is used.
     *
     * @param parentId parent id
     * @param object object to check
     * @return result of checking
     */
    public boolean isNameUsed(UUID parentId, IdEntity object) {
        String key = getKey(object.getClass(), parentId);
        return existingNamesCache.get().containsKey(key)
                && existingNamesCache.get().get(key).containsKey(object.getName())
                && !existingNamesCache.get().get(key).get(object.getName()).contains(object.getId());
    }

    /**
     * Check and correct name.
     *
     * @param parentId parent id
     * @param object object to check
     */
    public void checkAndCorrectName(UUID parentId, IdEntity object) {
        int i = 0;
        String newName;
        String initName = object.getName();
        while (isNameUsed(parentId, object)) {
            if (i == 0) {
                initName = object.getName() + " Copy";
                newName = initName;
            } else {
                newName = initName + " _" + i;
            }
            object.setName(newName);
            ++i;
        }
    }

    /**
     * Add an object to cache.
     *
     * @param parentId parent id
     * @param object object
     */
    public void addToCache(UUID parentId, IdEntity object) {
        String key = getKey(object.getClass(), parentId);
        if (!existingNamesCache.get().containsKey(key)) {
            log.debug("[!existingNamesCache] when cache no contains key. Object Name{}, object Id{}, "
                    + "existingNamesCache:{}", object.getName(), object.getId(), existingNamesCache.get());
            existingNamesCache.get().put(key, HashMultimap.create());
        }
        existingNamesCache.get().get(key).put(object.getName(), object.getId());
    }

    public void clearCache() {
        existingNamesCache.remove();
    }

    private String getKey(Class clazz, UUID parentId) {
        return clazz.getSimpleName() + parentId;
    }
}
