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

package org.qubership.atp.dataset.service.jpa.service.versioning;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.constants.Constants;
import org.qubership.atp.dataset.service.direct.DataSetListService;
import org.qubership.atp.dataset.service.direct.DataSetService;
import org.qubership.atp.dataset.service.jpa.service.AbstractJpaTest;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManDataSetList;
import org.qubership.atp.dataset.service.rest.dto.versioning.HistoryItemDto;
import org.qubership.atp.dataset.service.rest.dto.versioning.HistoryItemResponseDto;
import org.qubership.atp.dataset.versioning.service.JaversHistoryService;
import org.qubership.atp.dataset.versioning.service.RestoreService;

@Disabled
@Isolated
@SpringBootTest
@ContextConfiguration(classes = {TestConfiguration.class})
@ExtendWith(SpringExtension.class)
public class VersioningRestoreTest extends AbstractJpaTest {
    @Autowired
    protected DataSetListService queryDslDataSetListService;
    @Autowired
    protected RestoreService restoreService;
    @Autowired
    protected JaversHistoryService historyService;
    @Autowired
    protected DataSetService dsService;
    protected static ObjectMapper objectMapper = new ObjectMapper();


    UUID rootDataSetListId = UUID.fromString("f546d8c7-7486-4813-adab-8271a9df76f7");
    List<UUID> dataSetsToLock = new ArrayList<>(Collections.singletonList(UUID.fromString("8bd4b9ee-4a26-4df1-bcc7-e72fd2ef673c")));
    List<UUID> paramsToDelete = new ArrayList<>(Collections.singletonList(UUID.fromString("01da9b97-080d-4dbd-bbb7-a40fc1563dca")));
    List<UUID> textParamsToUpdate = new ArrayList<>(Collections.singletonList(UUID.fromString("f6d23b9a-259b-49d0-8285-c5789c35a6ca")));
    List<UUID> dataSetsToRename = new ArrayList<>(Collections.singletonList(UUID.fromString("8bd4b9ee-4a26-4df1-bcc7-e72fd2ef673c")));
    List<UUID> dataSetsToRemove = new ArrayList<>();
    List<UUID> attributesToRemove = new ArrayList<>();

    @Test
    @Sql(scripts = "classpath:test_data/sql/version_restore_test/versionRestorTest.sql")
    public void createHistoryItem_lockDataSet_newFilledRowInHistory() throws Exception {

        int initialRevision = getCurrentRevision(rootDataSetListId);
        // Lock DataSet
        UUID dataSetUuidLock = null;
        for (UUID dataSetId : dataSetsToLock) {
            dataSetUuidLock = dataSetId;
            dataSetService.lock(rootDataSetListId, Collections.singletonList(dataSetUuidLock), false);
            dataSetService.lock(rootDataSetListId, Collections.singletonList(dataSetUuidLock), true);
        }

        HistoryItemResponseDto allHistory = historyService.getAllHistory(rootDataSetListId, 0, 50);
        HistoryItemDto item = allHistory.getHistoryItems().get(0);

        Assertions.assertEquals(Constants.UNLOCKED, item.getOldValue());
        Assertions.assertEquals(Constants.LOCKED, item.getNewValue());
    }

    @Test
    @Sql(scripts = "classpath:test_data/sql/version_restore_test/versionRestorTest.sql")
    public void restore_RestoreWithLockedDataSet_Exception() throws Exception {
        //Original data
        int initialRevision = getCurrentRevision(rootDataSetListId);
        // Lock DataSet
        UUID dataSetUuidLock = null;
        for (UUID dataSetId : dataSetsToLock) {
            dataSetUuidLock = dataSetId;
            dataSetService.lock(rootDataSetListId, Collections.singletonList(dataSetUuidLock), false);
            dataSetService.lock(rootDataSetListId, Collections.singletonList(dataSetUuidLock), true);
        }
        HistoryItemResponseDto allHistory = historyService.getAllHistory(rootDataSetListId, 0, 50);

        try {
            restoreService.restore(rootDataSetListId, allHistory.getHistoryItems().size() - 1);
        } catch (IllegalArgumentException iae) {
            assertTrue(iae.getMessage().contains("Can not restore DSL"));
        }
    }

    @Test
    @Sql(scripts = "classpath:test_data/sql/version_restore_test/versionRestorTest.sql")
    public void rename_createHistoryItem_newFilledRowInHistory() throws Exception {
        //Original data
        int initialRevision = getCurrentRevision(rootDataSetListId);
        String dataSetNewName = "Data Set 1 new name";
        UUID dataSetUuidWithNewName;

        for (UUID dataSetId : dataSetsToRename) {
            dataSetUuidWithNewName = dataSetId;

            dsService.rename(dataSetUuidWithNewName, dataSetNewName);
        }

        HistoryItemResponseDto allHistory = historyService.getAllHistory(rootDataSetListId, 0, 50);
        HistoryItemDto item = allHistory.getHistoryItems().get(0);

        Assertions.assertEquals(item.getOldValue(), "Data Set 1");
        Assertions.assertEquals(item.getNewValue(), dataSetNewName);

        restore(initialRevision);
    }

    @Test
    @Sql(scripts = "classpath:test_data/sql/version_restore_test/versionRestorTest.sql")
    public void createDslChangeAndRestore_TreesEqual() throws Exception {

        for (UUID dataSetId : dataSetsToLock) {
            dataSetService.lock(rootDataSetListId, Collections.singletonList(dataSetId), true);
            dataSetService.lock(rootDataSetListId, Collections.singletonList(dataSetId), false);
        }
        int initialRevision = getCurrentRevision(rootDataSetListId);
        UiManDataSetList firstRevision = queryDslDataSetListService.getAsTree(rootDataSetListId, false);

        removeAndUpdateParameters();
        UiManDataSetList revisionParamsUpdate = queryDslDataSetListService.getAsTree(rootDataSetListId, false);
        //Params changed
        int paramsUpdateRevision = getCurrentRevision(rootDataSetListId);

        restore(initialRevision);
        UiManDataSetList restoredFirstRevision = queryDslDataSetListService.getAsTree(rootDataSetListId, false);

        Assertions.assertEquals(treeToString(firstRevision), treeToString(restoredFirstRevision));
        //Restore to changed data
        restore(paramsUpdateRevision);
        UiManDataSetList restoredRevisionParamsUpdate = queryDslDataSetListService.getAsTree(rootDataSetListId, false);

        Assertions.assertEquals(treeToString(revisionParamsUpdate), treeToString(restoredRevisionParamsUpdate));

        //Restore to original data
        restore(initialRevision);
        for (UUID dataSetId : dataSetsToRemove) {
            dataSetService.remove(dataSetId);
        }
        UiManDataSetList revisionDsDeletion = queryDslDataSetListService.getAsTree(rootDataSetListId, false);

        UiManDataSetList revisionRestoredDsDeletion = queryDslDataSetListService.getAsTree(rootDataSetListId, false);
        //Compare
        Assertions.assertEquals(treeToString(revisionDsDeletion), treeToString(revisionRestoredDsDeletion));

        //Restore to original data
        restore(initialRevision);
        for (UUID attributeId : attributesToRemove) {
            attributeService.remove(attributeId);
        }
    }

    private int getCurrentRevision(UUID dataSetListId) {
        return historyService.getAllHistory(dataSetListId, 0, 100).getHistoryItems().size();
    }

    private String treeToString(UiManDataSetList tree) throws JsonProcessingException {
        return objectMapper.writer().writeValueAsString(tree);
    }

    @Transactional
    public void restore(int revision) {
        restoreService.restore(rootDataSetListId, revision);
    }

    @Transactional
    public void removeAndUpdateParameters() {
        for (UUID parameterId : paramsToDelete) {
            parameterService.remove(parameterId);
        }
        for (UUID parameterId : textParamsToUpdate) {
            parameterService.updateParameter(parameterId, "Changed value", null ,null);
        }
    }
}
