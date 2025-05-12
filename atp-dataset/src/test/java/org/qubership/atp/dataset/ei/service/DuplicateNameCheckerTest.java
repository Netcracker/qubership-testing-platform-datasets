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

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Spy;

import org.qubership.atp.dataset.ei.model.IdEntity;

@Isolated
public class DuplicateNameCheckerTest {

    @Spy
    private DuplicateNameChecker duplicateNameChecker = new DuplicateNameChecker();

    @Test
    public void isNameUsed_whenNameIsUsed_thenReturnTrue() {
        IdEntity entity = new IdEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("Entity 1");

        UUID parentId = UUID.randomUUID();

        duplicateNameChecker.addToCache(parentId, entity);
        Assertions.assertTrue(duplicateNameChecker.isInitialized(IdEntity.class, parentId));

        entity.setId(UUID.randomUUID());

        Assertions.assertTrue(duplicateNameChecker.isNameUsed(parentId, entity));
    }

    @Test
    public void isNameUsed_whenNameIsUsedForTheSameObject_thenReturnFalse() {
        IdEntity entity = new IdEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("Entity 1");

        UUID parentId = UUID.randomUUID();

        duplicateNameChecker.addToCache(parentId, entity);
        Assertions.assertTrue(duplicateNameChecker.isInitialized(IdEntity.class, parentId));
        Assertions.assertFalse(duplicateNameChecker.isNameUsed(parentId, entity));
    }

    @Test
    public void isNameUsed_whenNameIsNotUsed_thenReturnFalse() {
        IdEntity entity = new IdEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("Entity 1");

        UUID parentId = UUID.randomUUID();

        duplicateNameChecker.addToCache(parentId, entity);
        Assertions.assertTrue(duplicateNameChecker.isInitialized(IdEntity.class, parentId));

        entity.setName("Entity 2");

        Assertions.assertFalse(duplicateNameChecker.isNameUsed(parentId, entity));
    }

    @Test
    public void checkAndCorrectName_whenNameIsUsed_thenGenerateNewName() {
        IdEntity entity = new IdEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("Entity 1");

        UUID parentId = UUID.randomUUID();

        duplicateNameChecker.addToCache(parentId, entity);
        Assertions.assertTrue(duplicateNameChecker.isInitialized(IdEntity.class, parentId));

        entity.setId(UUID.randomUUID());

        duplicateNameChecker.checkAndCorrectName(parentId, entity);

        Assertions.assertEquals("Entity 1 Copy", entity.getName());
    }

    @Test
    public void checkAndCorrectName_whenNameIsUsedForTheSameObject_thenKeepCurrentName() {
        IdEntity entity = new IdEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("Entity 1");

        UUID parentId = UUID.randomUUID();

        duplicateNameChecker.addToCache(parentId, entity);
        Assertions.assertTrue(duplicateNameChecker.isInitialized(IdEntity.class, parentId));

        duplicateNameChecker.checkAndCorrectName(parentId, entity);

        Assertions.assertEquals("Entity 1", entity.getName());
    }

    @Test
    public void checkAndCorrectName_whenNameIsNotUsed_thenKeepCurrentName() {
        IdEntity entity = new IdEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("Entity 1");

        UUID parentId = UUID.randomUUID();

        duplicateNameChecker.addToCache(parentId, entity);
        Assertions.assertTrue(duplicateNameChecker.isInitialized(IdEntity.class, parentId));

        entity.setName("Entity 2");

        duplicateNameChecker.checkAndCorrectName(parentId, entity);

        Assertions.assertEquals("Entity 2", entity.getName());
    }
}
