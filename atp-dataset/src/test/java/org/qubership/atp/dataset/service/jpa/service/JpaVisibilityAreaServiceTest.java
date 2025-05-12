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

package org.qubership.atp.dataset.service.jpa.service;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.exception.visibilityarea.VisibilityAreaNameException;
import org.qubership.atp.dataset.service.jpa.model.VisibilityAreaFlatModel;

//@Isolated
@Disabled
@SpringBootTest
@ContextConfiguration(classes = {TestConfiguration.class})
@ExtendWith(SpringExtension.class)
public class JpaVisibilityAreaServiceTest extends AbstractJpaTest {

    @Test
    public void getSorted_notNullNotEmpty() {
        List<VisibilityAreaFlatModel> byNameAsc = visibilityAreaService.getAllSortedByNameAsc();
        Assertions.assertNotNull(byNameAsc);
        Assertions.assertFalse(byNameAsc.isEmpty());
    }

    @Test
    public void getAllAreas_notNullNotEmpty() {
        List<VisibilityAreaFlatModel> all = visibilityAreaService.getAll();
        Assertions.assertNotNull(all);
        Assertions.assertFalse(all.isEmpty());
    }

    @Test
    public void createWithName_correctName_uuidNotNull() throws Exception {
        String testName = "test name";
        UUID withName = visibilityAreaService.getOrCreateWithName(testName);
        Assertions.assertNotNull(withName);
        VisibilityAreaFlatModel visibilityArea = visibilityAreaService.getFlatById(withName);
        Assertions.assertEquals(visibilityArea.getName(), testName);
    }

    @Test
    public void createWithName_twiceSameName_returnsExisting() throws Exception {
        String testName = "test name";
        UUID firstTry = visibilityAreaService.getOrCreateWithName(testName);
        UUID secondTry = visibilityAreaService.getOrCreateWithName(testName);
        Assertions.assertEquals(firstTry, secondTry);
    }

    @Test
    public void createWithName_null_throwsException() {
        Assertions.assertThrows(VisibilityAreaNameException.class, ()-> {
            visibilityAreaService.getOrCreateWithName(null);
        });

    }

    @Test
    public void createWithName_empty_throwsException() throws Exception {
        Assertions.assertThrows(VisibilityAreaNameException.class, ()-> {
            visibilityAreaService.getOrCreateWithName("");
        });

    }

    @Test
    public void setName_correct_nameSet() throws Exception {
        UUID visibilityAreaId = visibilityAreaService.getOrCreateWithName("test1 correct");
        String newName = "new name correct";
        visibilityAreaService.setName(visibilityAreaId, newName);
        VisibilityAreaFlatModel visibilityArea = visibilityAreaService.getFlatById(visibilityAreaId);
        Assertions.assertEquals(visibilityArea.getName(), newName);
    }

    @Test
    public void setName_null_throwsException() throws Exception {
        UUID visibilityAreaId = visibilityAreaService.getOrCreateWithName("test2 name");
        Assertions.assertThrows(VisibilityAreaNameException.class, ()-> {
            visibilityAreaService.setName(visibilityAreaId,null);
        });

    }

    @Test
    public void setName_empty_throwsException() throws Exception {
        UUID visibilityAreaId = visibilityAreaService.getOrCreateWithName("test3 removed");
        Assertions.assertThrows(VisibilityAreaNameException.class, ()-> {
            visibilityAreaService.setName(visibilityAreaId,"");
        });

    }

    @Test
    public void delete_returnNullWhenSelectByIdAfterDelete() throws Exception {
        VisibilityAreaFlatModel result = visibilityAreaService.getFlatById(UUID.randomUUID());
        Assertions.assertNull(result);
    }
}
