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

package org.qubership.atp.dataset.ei;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.qubership.atp.dataset.ei.service.DataSetAttributesImporter;
import org.qubership.atp.dataset.ei.service.DataSetFileImporter;
import org.qubership.atp.dataset.ei.service.DataSetListImporter;
import org.qubership.atp.dataset.ei.service.DataSetParametersImporter;
import org.qubership.atp.dataset.ei.service.DataSetsImporter;
import org.qubership.atp.ei.node.ImportExecutor;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ValidationResult;
import org.qubership.atp.ei.node.dto.validation.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DataSetImportExecutor implements ImportExecutor {

    private final DataSetListImporter dataSetListImporter;
    private final DataSetsImporter dataSetsImporter;
    private final DataSetAttributesImporter dataSetAttributesImporter;
    private final DataSetParametersImporter dataSetParametersImporter;
    private final DataSetFileImporter dataSetFileImporter;

    /**
     * Import data in server.
     * Import order:
     * - VA
     * - Data Set List
     * - Data Set
     * - Data Set Attribute
     * - Data Set Attribute Key
     * - Data Set List Values
     * - Data Set Parameter
     * - Files
     *
     * @param importData import data information
     * @param workDir    dir where import files are located
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void importData(ExportImportData importData, Path workDir) throws Exception {
        log.info("start importData(importData: {}, workDir: {})", importData, workDir);
        dataSetListImporter.clearDuplicateNamesCache();
        List<UUID> dataSetLists = dataSetListImporter.importDataSetLists(workDir, importData);
        dataSetsImporter.importDataSets(workDir, importData);
        dataSetAttributesImporter.importDataSetAttributes(workDir, dataSetLists, importData);
        dataSetAttributesImporter.importDataSetAttributeKeys(workDir, importData);
        dataSetParametersImporter.importDataSetParameters(workDir, importData);
        dataSetFileImporter.importFiles(workDir, importData);
        log.info("end importData()");
        dataSetListImporter.clearDuplicateNamesCache();
    }

    @Override
    @Transactional(readOnly = true)
    public ValidationResult preValidateData(ExportImportData exportImportData, Path path) throws Exception {
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public ValidationResult validateData(ExportImportData importData, Path workDir) throws Exception {
        log.info("Start validate Data in path: {}", workDir);
        List<UserMessage> details = new ArrayList<>();
        Set<String> messages = new HashSet<>();
        Map<UUID, UUID> repMap = new HashMap<>(importData.getReplacementMap());
        dataSetListImporter.clearDuplicateNamesCache();

        log.debug("[validateData]start handle import workDir: {} is new project:{} is interProject: {}",
                workDir, importData.isCreateNewProject(), importData.isInterProjectImport());
        if (importData.isImportFirstTime()) {
            log.info("validate will be skipped, because isImportFirstTime = true");
        } else if (importData.isCreateNewProject()) {
            handleCreateNewProjectValidation(workDir, repMap);
        } else if (importData.isInterProjectImport()) {
            handleInterProjectImportValidation(workDir, messages, repMap);
        } else {
            handleImportInTheSameProjectValidation(workDir, messages, repMap);
        }

        if (CollectionUtils.isNotEmpty(messages)) {
            details.addAll(messages.stream().map(UserMessage::new).collect(Collectors.toList()));
        }
        dataSetListImporter.clearDuplicateNamesCache();
        return new ValidationResult(details, repMap);
    }

    private void handleImportInTheSameProjectValidation(Path workDir, Set<String> messages, Map<UUID, UUID> repMap) {
        messages.addAll(dataSetListImporter.validateDataSetLists(workDir, repMap, false));
        messages.addAll(dataSetsImporter.validateDataSets(workDir, repMap, false));
        messages.addAll(dataSetAttributesImporter.validateDataSetAttributes(workDir, repMap, false));
        messages.addAll(dataSetParametersImporter.validateDataSetParameters(workDir, repMap, false));
    }

    private void handleInterProjectImportValidation(Path workDir, Set<String> messages, Map<UUID, UUID> repMap) {
        dataSetListImporter.fillRepMapWithSourceTargetValues(repMap, workDir);
        dataSetsImporter.fillRepMapWithSourceTargetValues(repMap, workDir);
        dataSetAttributesImporter.fillRepMapWithSourceTargetValues(repMap, workDir);
        dataSetParametersImporter.fillRepMapWithSourceTargetValues(repMap, workDir);
        repMap.entrySet().forEach(entry -> {
            if (entry.getValue() == null) {
                entry.setValue(UUID.randomUUID());
            }
        });

        messages.addAll(dataSetListImporter.validateDataSetLists(workDir, repMap, true));
        messages.addAll(dataSetsImporter.validateDataSets(workDir, repMap, true));
        messages.addAll(dataSetAttributesImporter.validateDataSetAttributes(workDir, repMap, true));
        messages.addAll(dataSetParametersImporter.validateDataSetParameters(workDir,  repMap, true));
    }

    private void handleCreateNewProjectValidation(Path workDir, Map<UUID, UUID> repMap) {
        List<UUID> allIds = new ArrayList<>();
        allIds.addAll(dataSetListImporter.getDslIds(workDir));
        allIds.addAll(dataSetsImporter.getDsIds(workDir));
        allIds.addAll(dataSetAttributesImporter.getDsAttributeIds(workDir));
        allIds.addAll(dataSetAttributesImporter.getDsAttributeKeysIds(workDir));
        allIds.addAll(dataSetAttributesImporter.getDsListValuesIds(workDir));
        allIds.addAll(dataSetParametersImporter.getDsParameterIds(workDir));
        allIds.forEach(id -> repMap.put(id, UUID.randomUUID()));
    }

}
