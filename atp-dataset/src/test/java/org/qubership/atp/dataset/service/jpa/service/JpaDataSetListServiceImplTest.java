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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.model.dsllazyload.referencedcontext.RefDataSetListFlat;
import org.qubership.atp.dataset.service.rest.server.CopyDataSetListsResponse;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import lombok.AllArgsConstructor;
import lombok.Data;

//@Isolated
@Disabled
@SpringBootTest
@ContextConfiguration(classes = {TestConfiguration.class})
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JpaDataSetListServiceImplTest extends AbstractJpaTest {

	private static final Map<UUID, String> datasetListsMap = new HashMap<>();
	private final UUID visibilityAreaId = UUID.fromString("b703c594-e214-49cc-bda7-4eb51311ddb1");

	@BeforeAll
	public static void generateData() {
		datasetListsMap.put(UUID.fromString("39d0e387-1150-4344-a061-4b81a6a571df"), "Test DataSetList 1");
		datasetListsMap.put(UUID.fromString("da430230-6c37-42ee-8b6c-f855e6024eed"), "Test DataSetList 2");
		datasetListsMap.put(UUID.fromString("c5e341f7-8c6e-437b-9af0-850710dffd83"), "Test DataSetList 3");
	}

	@Test
	@Sql(scripts = "classpath:test_data/sql/jpa_data_set_list_service_lmpl_test/jpaDataSetListImplTest.sql")
	public void copyDataSetLists_withPrevNamePattern() {
		List<PrevNamePatternTestCase> testCases = asList(
				new PrevNamePatternTestCase("DS Release 23.1", "Release 23.1", "Release 23.2", "DS Release 23.2"),
				new PrevNamePatternTestCase("DS Release 23.1 DS", "Release 23.1", "Release 23.2", "DS Release 23.2 DS"),
				new PrevNamePatternTestCase("Release 23.1 DS", "Release 23.1", "Release 23.2", "Release 23.2 DS"),
				new PrevNamePatternTestCase("DS Release 23.1 Release 23.1 test", "Release 23.1", "Release 23.2", "DS Release 23.2 Release 23.2 test"),
				new PrevNamePatternTestCase("DS Release 23.1", "Release 23.1", null, "DS Copy"),
				new PrevNamePatternTestCase("DS Release 23.3", "Release 23.1", "Release 23.2", "DS Release 23.3 Release 23.2")
		);

		Map<String, DataSetList> createdDslMap = new HashMap<>();

		for (PrevNamePatternTestCase testCase : testCases) {
			DataSetList createdDataSetList;
			if (createdDslMap.containsKey(testCase.getInitialDslName())) {
				createdDataSetList = createdDslMap.get(testCase.getInitialDslName());
			} else {
				createdDataSetList = dataSetListService.create(testCase.getInitialDslName(), visibilityAreaId);
				createdDslMap.put(testCase.getInitialDslName(), createdDataSetList);
			}

			List<CopyDataSetListsResponse> copyResponse = dataSetListService.copyDataSetLists(
					singletonList(createdDataSetList.getId()), true, null, testCase.getPostfix(), testCase.getPrevNamePattern(), null);
			for (CopyDataSetListsResponse response : copyResponse) {
				DataSetList copiedDatasetList = dataSetListService.getById(response.getCopyId());
				assertEquals(testCase.getExpectedName(), copiedDatasetList.getName());
			}
		}
	}

	@Test
	@Sql(scripts = "classpath:test_data/sql/jpa_data_set_list_service_lmpl_test/jpaDataSetListImplTest.sql")
	public void copyDataSetLists_withoutPostfix() {
		List<CopyDataSetListsResponse> result = dataSetListService.copyDataSetLists(new ArrayList<>(datasetListsMap.keySet()), true, null, null);
		for (CopyDataSetListsResponse response : result) {
			DataSetList copiedDatasetList = dataSetListService.getById(response.getCopyId());
			assertEquals(datasetListsMap.get(response.getOriginalId()) + " Copy", copiedDatasetList.getName());
		}
	}

	@Test
	@Sql(scripts = "classpath:test_data/sql/jpa_data_set_list_service_lmpl_test/jpaDataSetListImplTest.sql")
	public void copyDataSetLists_withPostfix() {
		String postfix = "test";
		List<CopyDataSetListsResponse> result = dataSetListService.copyDataSetLists(
				new ArrayList<>(datasetListsMap.keySet()), true, postfix, null);
		for (CopyDataSetListsResponse response : result) {
			DataSetList copiedDatasetList = dataSetListService.getById(response.getCopyId());
			assertEquals(datasetListsMap.get(response.getOriginalId()) + " " + postfix, copiedDatasetList.getName());
		}
	}

	@Test
	@Sql(scripts = "classpath:test_data/sql/jpa_data_set_list_service_lmpl_test/jpaDataSetListImplTest.sql")
	public void copyDataSetLists_copiedDataSetListNameAlreadyExists() {
		// given --- prepare datasets with "Copy" postfix
		dataSetListService.copyDataSetLists(
				new ArrayList<>(datasetListsMap.keySet()), true, null, null);
		// when
		List<CopyDataSetListsResponse> result = dataSetListService.copyDataSetLists(new ArrayList<>(datasetListsMap.keySet()), true, null, null);
		// then
		for (CopyDataSetListsResponse response : result) {
			DataSetList copiedDatasetList = dataSetListService.getById(response.getCopyId());
			assertEquals(datasetListsMap.get(response.getOriginalId()) + " Copy Copy", copiedDatasetList.getName());
		}
	}

	@Test
	@Sql(scripts = "classpath:test_data/sql/jpa_data_set_list_service_lmpl_test/jpaDataSetListImplTest.sql")
	public void getReferencedDataSetListFlat_maxPageSize() {
		UUID dslId = UUID.fromString("39d0e387-1150-4344-a061-4b81a6a571df");
		UUID dslAttrId = UUID.fromString("686026e5-26d8-4feb-9b11-c56eb9f72e0d");
		Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);

		RefDataSetListFlat res = dataSetListService.getReferencedDataSetListFlat(dslId, dslAttrId, "686026e5-26d8-4feb-9b11-c56eb9f72e0d", null,
				pageable);
		assertEquals(4, res.getAttributes().size());
		// parameters count equals datasets count
		assertEquals(2, res.getAttributes().get(0).getParameters().size());
	}

	@Test
	@Sql(scripts = "classpath:test_data/sql/jpa_data_set_list_service_lmpl_test/jpaDataSetListImplTest.sql")
	public void getReferencedDataSetListFlat_minPageSize() {
		UUID dslId = UUID.fromString("39d0e387-1150-4344-a061-4b81a6a571df");
		UUID dslAttrId = UUID.fromString("686026e5-26d8-4feb-9b11-c56eb9f72e0d");
		Pageable pageable = PageRequest.of(0, 1);

		RefDataSetListFlat res = dataSetListService.getReferencedDataSetListFlat(dslId, dslAttrId, "686026e5-26d8-4feb-9b11-c56eb9f72e0d", null,
				pageable);
		assertEquals(4, res.getAttributes().size());
		// parameters count equals datasets count
		assertEquals(1, res.getAttributes().get(0).getParameters().size());
	}

	@Test
	@Sql(scripts = "classpath:test_data/sql/jpa_data_set_list_service_lmpl_test/jpaDataSetListImplTest.sql")
	public void copyDataSetLists_withSagaSessionId() {
		// given
		UUID sagaSessionId = UUID.randomUUID();

		// when
		List<CopyDataSetListsResponse> result = dataSetListService.copyDataSetLists(new ArrayList<>(datasetListsMap.keySet()),
				true, null, null, null, sagaSessionId);

		// then
		for (CopyDataSetListsResponse response : result) {
			DataSetList copiedDatasetList = dataSetListService.getById(response.getCopyId());
			assertEquals(datasetListsMap.get(response.getOriginalId()) + " Copy Copy", copiedDatasetList.getName());
			assertEquals(sagaSessionId, copiedDatasetList.getSagaSessionId());
		}
	}

	@Data
	@AllArgsConstructor
	private static class PrevNamePatternTestCase {
		private String initialDslName;
		private String prevNamePattern;
		private String postfix;
		private String expectedName;
	}
}
