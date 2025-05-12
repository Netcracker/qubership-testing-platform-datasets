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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.qubership.atp.dataset.db.jpa.repositories.JpaAttributeKeyRepository;
import org.qubership.atp.dataset.ei.DataSetImportExecutor;
import org.qubership.atp.dataset.service.jpa.JpaAttributeService;
import org.qubership.atp.dataset.service.jpa.delegates.AttributeKey;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ValidationResult;
import org.qubership.atp.ei.node.dto.validation.ValidationType;

@Isolated
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class DataSetAttributesImporterIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    protected JpaAttributeKeyRepository attributeKeyRepository;
    @Autowired
    protected JpaAttributeService attributeService;
    @Autowired
    private DataSetImportExecutor dataSetImportExecutor;
    private ExportImportData importData;
    @Autowired
    protected DataSource dataSource;

    @BeforeEach
    public void setUp() throws Exception {
        importData = new ExportImportData(null, null, null, false, false, null, new HashMap<>(), new HashMap<>(), ValidationType.VALIDATE, false);
    }

    @Test
    @Sql(scripts = "classpath:test_data/sql/overlap_lost_positive.sql")
    public void importData_importOverlapIntoDbWithOverlapsWithTheSameParameters_OverlapsInDbShouldBeChangedWithoutDuplication() throws Exception {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206211");
        int attributeKeysNumber = attributeKeyRepository.findAll().size();

        dataSetImportExecutor.importData(importData, workDir);

        AttributeKey attributeKey = attributeService.getAttributeKeyById(
                UUID.fromString("f7d1609f-6f08-4742-b6d0-3a0c6ca1af6e"));
        assertEquals("b37f7e80-1c04-4663-af6c-8f71f5f2dab9", attributeKey.getKey());
        assertEquals(UUID.fromString("f26f9fc4-7cea-44da-b02f-6392520801d9"), attributeKey.getDataSetList().getId());
        assertEquals(UUID.fromString("46cbd694-dbdd-4d1e-a967-e6edee97f318"), attributeKey.getDataSet().getId());
        assertEquals(UUID.fromString("d5d0b2d7-782c-4ae2-a273-c6377f874d82"), attributeKey.getAttribute().getId());
        assertEquals(attributeKeysNumber, attributeKeyRepository.findAll().size());
    }

    @Test
    @Sql(scripts = "classpath:test_data/sql/overlap_lost_negative.sql")
    public void importData_importOverlapIntoDbWithOverlapsWithTheSameParameters_OverlapsInDbShouldBeDuplicated() throws Exception {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206211");
        int attributeKeysNumber = attributeKeyRepository.findAll().size();

        dataSetImportExecutor.importData(importData, workDir);

        AttributeKey ake1 = attributeService.getAttributeKeyById(
                UUID.fromString("f7d1609f-6f08-4742-b6d0-3a0c6ca1af6f"));
        AttributeKey ake2 = attributeService.getAttributeKeyById(
                UUID.fromString("f7d1609f-6f08-4742-b6d0-3a0c6ca1af6d"));

        assertNotNull(ake1);
        assertEquals("b37f7e80-1c04-4663-af6c-8f71f5f2dab9", ake1.getKey());
        assertEquals(UUID.fromString("f26f9fc4-7cea-44da-b02f-6392520801d9"), ake1.getDataSetList().getId());
        assertEquals(UUID.fromString("46cbd694-dbdd-4d1e-a967-e6edee97f318"), ake1.getDataSet().getId());
        assertEquals(UUID.fromString("d5d0b2d7-782c-4ae2-a273-c6377f874d82"), ake1.getAttribute().getId());

        assertNotNull(ake2);
        assertEquals("b37f7e80-1c04-4663-af6c-8f71f5f2dab9", ake2.getKey());
        assertEquals(UUID.fromString("f26f9fc4-7cea-44da-b02f-6392520801d9"), ake2.getDataSetList().getId());
        assertEquals(UUID.fromString("46cbd694-dbdd-4d1e-a967-e6edee97f318"), ake2.getDataSet().getId());
        assertEquals(UUID.fromString("d5d0b2d7-782c-4ae2-a273-c6377f874d81"), ake2.getAttribute().getId());
        assertEquals(attributeKeysNumber + 1, attributeKeyRepository.findAll().size());
    }

    @Test
    public void handleCreateNewProjectValidation_validateData_returnReplacementMap() throws Exception {
        ExportImportData importData = new ExportImportData(UUID.randomUUID(), null, null,
                true, false, null, new HashMap<>(), new HashMap<>(), ValidationType.VALIDATE, false);
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206211");

        ValidationResult result = dataSetImportExecutor.validateData(importData, workDir);

        assertTrue(result.isValid());
        assertNotNull(result.getDetails());
        assertTrue(result.getDetails().isEmpty());
        assertEquals(10, result.getReplacementMap().size());
    }

    @Test
    public void handleInterProjectImportValidation_validateData_returnReplacementMap() throws Exception {
        ExportImportData importData = new ExportImportData(UUID.randomUUID(), null, null,
                false, true, null, new HashMap<>(), new HashMap<>(), ValidationType.VALIDATE, false);
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206211");

        ValidationResult result = dataSetImportExecutor.validateData(importData, workDir);
        assertTrue(result.isValid());
        assertNotNull(result.getDetails());
        assertTrue(result.getDetails().isEmpty());
        assertEquals(10, result.getReplacementMap().size());
    }

    @AfterEach
    public void clearDb() throws IOException {
        String pathStr = "src/test/resources/test_data/sql/overlap_lost_clear_tables.sql";
        Path path = Paths.get(pathStr);
        Files.readAllBytes(path);
        String query = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        jdbcTemplate.execute(query);
    }
}
