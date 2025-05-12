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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.IsNull;
import org.junit.After;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.exception.dataset.DataSetExistsException;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Filter;
import org.qubership.atp.dataset.model.Label;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.service.AbstractTest;
import lombok.SneakyThrows;

@Isolated
@ContextConfiguration(classes = {TestConfiguration.class})
@TestPropertySource(properties = {"atp-dataset.javers.enabled=false"})
public class FilterServiceImplTest extends AbstractTest {

    private static final String TEST_FILTER_NAME = "Test";
    private VisibilityArea va;
    private DataSetList dsl;
    private DataSet ds;
    private Label dslLabel;
    private Filter filter;
    private List<UUID> dslLabels;

    @BeforeEach
    @SneakyThrows(DataSetExistsException.class)
    public void setUp() {
        va = visibilityAreaService.create("Filter_test_VA");
        dsl = dataSetListService.create(va.getId(), "DSL", null);
        ds = dataSetService.create(dsl.getId(), "DS");
        dslLabel = dataSetListService.mark(dsl.getId(), "T_Label");
        dslLabels = Collections.singletonList(dslLabel.getId());
        filter = filterService.create(
                TEST_FILTER_NAME, va.getId(), emptyList(), dslLabels
        );
    }

//    @Test(expected = IllegalArgumentException.class)
    @Test
    public void testFilterCreationFailedDueToBothLabelReferenceIsEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            filterService
                    .create(TEST_FILTER_NAME, va.getId(), emptyList(), emptyList());
        });

    }

    @Test
    public void testFilterIsCreated() {
        assertThat(filter, IsNot.not(IsNull.nullValue()));
        assertThat(filter.getName(), Is.is(TEST_FILTER_NAME));
        assertTrue(filter.getId() != null && filter.getId() instanceof UUID);
        assertThat(filter.getDataSetLabels(), Is.is(emptyList()));
        assertThat(filter.getDataSetListLabels(), Is.is(dslLabels));
    }

    @Test
    public void testFilterIsGetFromRepository() {
        filter = filterService.get(filter.getId());

        assertNotNull(filter);
        assertThat(filter.getDataSetLabels().size(), Is.is(0));
        assertThat(filter.getDataSetListLabels().size(), Is.is(1));
        assertThat(filter.getName(), Is.is(TEST_FILTER_NAME));
    }

    @Test
    public void testFilterDoesNotExistAfterDeletion() {
        filterService.delete(filter.getId());

        assertThat(filterService.get(filter.getId()), IsNull.nullValue());
    }

    @Test
    public void testUpdateFilterSavedName() {
        Filter secondFilter = filterService.create(TEST_FILTER_NAME, va.getId(), emptyList(), dslLabels);
        filterService.update(filter.getId(), "Updated", filter.getDataSetLabels(), filter.getDataSetListLabels());
        secondFilter = filterService.get(secondFilter.getId());
        Filter filter = filterService.get(this.filter.getId());

        assertThat(filter, IsNot.not(IsNull.nullValue()));
        assertThat(filter.getName(), Is.is("Updated"));
        assertThat(secondFilter.getName(), Is.is(TEST_FILTER_NAME));
    }

    @Test
    public void testUpdateFilterAtSaveWillAddRefToLabel() {
        Label secondDSL = dataSetListService.mark(dsl.getId(), "SecondDSL");
        filterService.update(filter.getId(), TEST_FILTER_NAME, emptyList(),
                Arrays.asList(dslLabel.getId(), secondDSL.getId())
        );
        Filter filter = filterService.get(this.filter.getId());

        assertThat(filter.getDataSetListLabels().size(), Is.is(2));
    }

    @Test
    public void testDeleteCascade() {
        visibilityAreaService.delete(va.getId());

        assertNull(filterService.get(filter.getId()));
    }

    @Test
    public void testGetAllUnderVaIdReturnsAllLabelsUnderVa() {
        VisibilityArea secondVa = visibilityAreaService.create("Second");
        try {
            DataSetList secondDsl = dataSetListService.create(secondVa.getId(), "SecondDsl", null);
            Label secondDslLabel = dataSetListService.mark(secondDsl.getId(), "ShouldNotBeDisplayed");
            Filter secondFilter = filterService
                    .create("SecondFilter", secondVa.getId(), emptyList(), singletonList(secondDslLabel.getId()));
            List<Filter> all = filterService.getAll(va.getId());

            assertFalse(all.contains(secondFilter));
            assertTrue(all.contains(filter));
        } finally {
            visibilityAreaService.delete(secondVa.getId());
        }
    }

    @Test
    public void getAll_createNewFilterAndGetAllFiltersOnDsl_newFilterIsPresent() {
        Label dslLabel = dataSetListService.mark(dsl.getId(), "ShouldBeDisplayed");
        Filter filter = filterService.create("Filter", va.getId(), emptyList(), singletonList(dslLabel.getId()));

        List<Filter> filters = filterService.getAll(va.getId());

        assertTrue(filters.contains(filter));
    }

    @After
    public void tearDown() {
        visibilityAreaService.delete(va.getId());
    }
}
