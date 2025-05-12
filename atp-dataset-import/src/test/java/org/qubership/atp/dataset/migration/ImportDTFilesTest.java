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

package org.qubership.atp.dataset.migration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.qubership.atp.crypt.api.Decryptor;
import org.qubership.atp.crypt.api.Encryptor;
import org.qubership.atp.dataset.migration.config.TestConfiguration;
import org.qubership.atp.dataset.migration.formula.model.EvaluationContext;
import org.qubership.atp.dataset.migration.model.ImportResources;
import org.qubership.atp.dataset.migration.repo.DsServicesFacade;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Named;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.ParameterOverlap;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.model.utils.OverlapIterator;
import org.qubership.atp.dataset.service.direct.AliasWrapperService;
import org.qubership.atp.dataset.service.direct.impl.ClearCacheServiceImpl;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfiguration.class})
@TestPropertySource(properties = {
        "jdbc.leak.detection.threshold=10",
        "atp-dataset.last.revision.count=200",
        "atp-dataset.archive.job.bulk-delete-count=1000",
        "atp-dataset.archive.cron.expression=0 0 0 * * ?",
        "atp-dataset.archive.job.name=atp-dataset-archive-job",
        "atp-dataset.archive.job.page-size=50",
        "atp-dataset.archive.job.thread.max-pool-size=5",
        "atp-dataset.archive.job.thread.core-pool-size=5",
        "atp-dataset.archive.job.thread.queue-capacity=100"
})
@Isolated
public class ImportDTFilesTest {

    public static final String[] OVERLAP_LIST_VALUES = {"false", "CVS", "LSC"};
    public static final String[] DEFAULT_LIST_VALUES = {"de_DE", "en_GB", "en_US"};
    @Autowired
    DsServicesFacade services;
    @Autowired
    AliasWrapperService wrapperService;
    @MockBean
    protected Encryptor encryptor;
    @MockBean
    protected Decryptor decryptor;
    @MockBean
    ClearCacheServiceImpl clearCacheService;

    @BeforeEach
    public void setUp() throws Exception {
        EvaluationContext.getContext().clear();
    }

    @Test
    public void importSmall_dataset_E2E_ParentChildTest() throws Exception {
        String va_name = "TEST NEW LOGIC 2";
        services.va.getAll().stream()
                .filter(va -> va.getName().equals(va_name))
                .findFirst()
                .ifPresent(va -> services.va.delete(va.getId()));
        String excelFolder = "TEST_DATA/test_data_1/";
        String parentFileName = excelFolder + "Parent_SMALL.xlsx";
        String childFileName = excelFolder + "Child_SMALL.xlsx";
        final String test_dsl = "TEST DSL";
        String groupDataSetName_1 = "TEST Default";
        ParentDsImporter.process(ImportResources.create(services, va_name, excelFolder, parentFileName,
                groupDataSetName_1), false);
        ChildDsImporter.process(ImportResources.create(services, va_name, excelFolder, childFileName,
                groupDataSetName_1), test_dsl, false);
        parentFileName = excelFolder + "Parent_SMALL_2.xlsx";
        childFileName = excelFolder + "Child_SMALL_2.xlsx";
        String groupDataSetName_2 = "TEST Default 2";
        ParentDsImporter.process(ImportResources.create(services, va_name, excelFolder, parentFileName,
                groupDataSetName_2), false);
        ChildDsImporter.process(ImportResources.create(services, va_name, excelFolder, childFileName,
                groupDataSetName_2), test_dsl, false);
        //check all data here
        VisibilityArea va = services.va.getAll().stream()
                .filter(v -> v.getName().equals(va_name))
                .findFirst().get();
        assertEquals(va_name, va.getName(), "Visibility Area created");
        DataSetList dsl = services.dsl.getAll(va.getId()).stream()
                .filter(v -> v.getName().equals(test_dsl))
                .findFirst().get();
        assertThat(Arrays.asList("CHILD_DS_1", "CHILD_DS_2", "CHILD_DS_3", "CHILD_DS_4", "CHILD_DS_5",
                "CHILD_DS_2_1", "CHILD_DS_2_2", "CHILD_DS_2_3"),
                containsInAnyOrder(
                        dsl.getDataSets().stream().map(Named::getName).toArray()
                ));
        Map<String, DataSet> childDses = dsl.getDataSets().stream().collect(Collectors.toMap(Named::getName, ds -> ds));
        assertThat(Arrays.asList("Common", "CFS", "Keys"),
                containsInAnyOrder(
                        dsl.getAttributes().stream().map(Named::getName).toArray()
                ));
        Map<String, Attribute> groupAttrs = dsl.getAttributes().stream().collect(Collectors.toMap(Named::getName,
                attr -> attr));
        DataSet child1 = childDses.get("CHILD_DS_1");
        DataSet childDS_CommonGroup = getParameter(child1, "Common").getDataSetReference();
        assertEquals(groupDataSetName_1, childDS_CommonGroup.getName());
        assertEquals("simple_text", getParameter(childDS_CommonGroup, "TEXT").getText());
        assertEquals("#RANDOMBETWEEN(10, 100)", getParameter(childDS_CommonGroup, "RANDOM_VALUE").getText());
        assertEquals("#UUID_UPPERCASE()", getParameter(childDS_CommonGroup, "UUID_UP").getText());
        assertEquals("#UUID()", getParameter(childDS_CommonGroup, "UUID_LOW").getText());
        assertEquals("FORMULA_AS_TEXT", getParameter(childDS_CommonGroup, "FORMULA_AS_TEXT").getText());
        assertNull(getParameter(childDS_CommonGroup, "ONLY_IN_SECOND"));
        assertThat(getParameter(childDS_CommonGroup, "LIST_VALUE").getListValue().getName(),
                isOneOf(DEFAULT_LIST_VALUES));
        assertEquals("simple_text", getParameter(childDS_CommonGroup, "NOT_LIST_VALUE").getText());
        DataSet childDS_CfsGroup = getParameter(child1, "CFS").getDataSetReference();
        assertEquals(groupDataSetName_1, childDS_CfsGroup.getName());
        assertEquals(
                "#REF_DSL(CFS.TEST Default.ref_to_another_group)",
                wrapperService.unWrapAlias(getParameter(childDS_CfsGroup, "forward_ref").getText())
        );
        assertEquals(
                "#REF_DSL(CFS.TEST Default.forward_ref)",
                wrapperService.unWrapAlias(getParameter(childDS_CfsGroup, "backward_ref").getText())
        );
        assertEquals(
                "#REF_DSL(Keys.TEST Default.E164_list_keyA_1)",
                wrapperService.unWrapAlias(getParameter(childDS_CfsGroup, "ref_to_another_group").getText())
        );
        DataSet child2 = childDses.get("CHILD_DS_2_1");
        DataSet childDS_2_CommonGroup = getParameter(child2, "Common").getDataSetReference();
        assertEquals(groupDataSetName_2, childDS_2_CommonGroup.getName());
        assertEquals("VALUE_FROM_PARENT_SMALL_2", getParameter(childDS_2_CommonGroup, "TEXT").getText());
        assertEquals("#RANDOMBETWEEN(10, 100)", getParameter(childDS_2_CommonGroup, "RANDOM_VALUE").getText());
        assertEquals("#UUID_UPPERCASE()", getParameter(childDS_2_CommonGroup, "UUID_UP").getText());
        assertEquals("#UUID()", getParameter(childDS_2_CommonGroup, "UUID_LOW").getText());
        assertEquals("FORMULA_AS_TEXT", getParameter(childDS_2_CommonGroup, "FORMULA_AS_TEXT").getText());
        assertEquals("SOME_TEXT", getParameter(childDS_2_CommonGroup, "ONLY_IN_SECOND").getText());
        DataSet childDS_2_CfsGroup = getParameter(child2, "CFS").getDataSetReference();
        assertEquals(groupDataSetName_2, childDS_2_CfsGroup.getName());
        assertEquals(
                "#REF_DSL(CFS.TEST Default 2.ref_to_another_group)",
                wrapperService.unWrapAlias(getParameter(childDS_2_CfsGroup, "forward_ref").getText())
        );
        assertEquals(
                "#REF_DSL(CFS.TEST Default 2.forward_ref)",
                wrapperService.unWrapAlias(getParameter(childDS_2_CfsGroup, "backward_ref").getText())
        );
        assertEquals(
                "#REF_DSL(Keys.TEST Default 2.E164_list_keyA_1)",
                wrapperService.unWrapAlias(getParameter(childDS_2_CfsGroup, "ref_to_another_group").getText())
        );
        Attribute commonGroup = groupAttrs.get("Common");
        Attribute listValueAttr = commonGroup.getDataSetListReference().getAttributes().stream().filter(attr ->
                "LIST_VALUE".equals(attr.getName())).findAny().get();
        //overlap list value by text works
        OverlapIterator listValueParams = OverlapIterator.create(childDses.get("CHILD_DS_2"), listValueAttr.getId(),
                Collections.singletonList(commonGroup.getId()));
        ParameterOverlap parameterOverlap = listValueParams.next().asReachable().asParameterOverlap();
        assertThat(parameterOverlap.getListValue(), isIn(parameterOverlap.getAttribute().getListValues()));
        assertEquals("simple_text", parameterOverlap.getListValue().getName());
        assertThat(listValueParams.next().getParameter().get().getListValue().getName(), isOneOf(DEFAULT_LIST_VALUES));
        //overlap list value by another list value works
        listValueParams = OverlapIterator.create(childDses.get("CHILD_DS_3"), listValueAttr.getId(),
                Collections.singletonList(commonGroup.getId()));
        assertThat(listValueParams.next().asReachable().asParameterOverlap().getListValue().getName(), isOneOf
                (OVERLAP_LIST_VALUES));
        assertThat(listValueParams.next().getParameter().get().getListValue().getName(), isOneOf(DEFAULT_LIST_VALUES));
        //overlap list value by CONSTANT_TEXT_VALUE works
        listValueParams = OverlapIterator.create(childDses.get("CHILD_DS_4"), listValueAttr.getId(),
                Collections.singletonList(commonGroup.getId()));
        parameterOverlap = listValueParams.next().asReachable().asParameterOverlap();
        assertThat(parameterOverlap.getListValue(), isIn(parameterOverlap.getAttribute().getListValues()));
        assertEquals("simple_text", parameterOverlap.getListValue().getName());
        assertThat(listValueParams.next().getParameter().get().getListValue().getName(), isOneOf(DEFAULT_LIST_VALUES));
        //overlap list value by macros is restricted
        listValueParams = OverlapIterator.create(childDses.get("CHILD_DS_5"), listValueAttr.getId(),
                Collections.singletonList(commonGroup.getId()));
        assertThat(listValueParams.next().getParameter().get().getListValue().getName(), isOneOf(DEFAULT_LIST_VALUES));
        Assertions.assertFalse(listValueParams.hasNext(), "List value should not be overridden by macros");
        //overlap text value by list value item works
        Attribute notListValueAttr = commonGroup.getDataSetListReference().getAttributes().stream().filter(attr ->
                "NOT_LIST_VALUE".equals(attr.getName())).findAny().get();
        OverlapIterator notListValueParams = OverlapIterator.create(childDses.get("CHILD_DS_3"),
                notListValueAttr.getId(), Collections.singletonList(commonGroup.getId()));
        assertThat(notListValueParams.next().asReachable().asParameterOverlap().getText(),
                isOneOf(OVERLAP_LIST_VALUES));
        assertEquals("simple_text", notListValueParams.next().getParameter().get().getText());
    }

    @Test
    public void importFromAtp_TransformAtpMacroRAND_CovertedToDataSetMacroRANDBETWEEN() throws Exception {
        String va_name = "importVariablesWithMacros_2";
        services.va.getAll().stream()
                .filter(va -> va.getName().equals(va_name))
                .findFirst()
                .ifPresent(va -> services.va.delete(va.getId()));
        String excelFolder = "TEST_DATA/";
        String parentFileName = excelFolder + "importVariablesWithMacros_2.xlsx";
        String childFileName = excelFolder + "importVariablesWithMacros_2.xlsx";
        final String dslName = "DJR_6620";
        String groupDataSetName_1 = "Default";
        ParentDsImporter.process(ImportResources.create(services, va_name, excelFolder, parentFileName,
                groupDataSetName_1), false);
        ChildDsImporter.process(ImportResources.create(services, va_name, excelFolder, childFileName,
                groupDataSetName_1), dslName, false);
        VisibilityArea va = services.va.getAll().stream()
                .filter(visibilityArea -> visibilityArea.getName().equals(va_name))
                .findAny().get();
        Map<String, DataSetList> dataSetLists = va.getDataSetLists().stream()
                .collect(Collectors.toMap(Named::getName, dataSetList -> dataSetList));
        DataSetList dsl = dataSetLists.get(dslName);
        DataSet ds = dsl.getDataSets().iterator().next();
        DataSetList customer = dataSetLists.get("EmailContact");
        Attribute dslIntoEmailContact =
                dsl.getAttributes().stream().filter(attr -> "EmailContact".equals(attr.getName()))
                        .findAny().get();
        Attribute mail1 = customer.getAttributes().stream().filter(attr -> "mail1".equals(attr.getName()))
                .findAny().get();
        Attribute mail2 = customer.getAttributes().stream().filter(attr -> "mail2".equals(attr.getName()))
                .findAny().get();
        Attribute mail3 = customer.getAttributes().stream().filter(attr -> "mail3".equals(attr.getName()))
                .findAny().get();
        Attribute mail4 = customer.getAttributes().stream().filter(attr -> "mail4".equals(attr.getName()))
                .findAny().get();
        Attribute mail5 = customer.getAttributes().stream().filter(attr -> "mail5".equals(attr.getName()))
                .findAny().get();
        String mail1Value = OverlapIterator.create(ds, mail1.getId(), Collections.singleton
                (dslIntoEmailContact.getId()
                )).next().asReachable().getValue().get();
        assertEquals("#RANDOMBETWEEN(0,9)at@yandex.ru", mail1Value);
        String mail2Value = OverlapIterator.create(ds, mail2.getId(), Collections.singleton
                (dslIntoEmailContact.getId()
                )).next().asReachable().getValue().get();
        assertEquals("#RANDOMBETWEEN(0,9)at@yandex.ru", mail2Value);
        String mail3Value = OverlapIterator.create(ds, mail3.getId(), Collections.singleton
                (dslIntoEmailContact.getId()
                )).next().asReachable().getValue().get();
        assertEquals("#RANDOMBETWEEN(0,9)at@yandex.ru", mail3Value);
        String mail4Value = OverlapIterator.create(ds, mail4.getId(), Collections.singleton
                (dslIntoEmailContact.getId()
                )).next().asReachable().getValue().get();
        assertEquals("#RANDOMBETWEEN(10,99)at@yandex.ru", mail4Value);
        String mail5Value = OverlapIterator.create(ds, mail5.getId(), Collections.singleton
                (dslIntoEmailContact.getId()
                )).next().asReachable().getValue().get();
        assertEquals("#RANDOMBETWEEN(1000000000,9999999999)at@yandex.ru", mail5Value);
    }

    @Test
    public void importFromAtp_ParamHasAtpMacro_ConvertedToDataSetMacro() throws Exception {
        String va_name = "importVariablesWithMacros_1";
        services.va.getAll().stream()
                .filter(va -> va.getName().equals(va_name))
                .findFirst()
                .ifPresent(va -> services.va.delete(va.getId()));
        String excelFolder = "TEST_DATA/";
        String parentFileName = excelFolder + "importVariablesWithMacros_1.xlsx";
        String childFileName = excelFolder + "importVariablesWithMacros_1.xlsx";
        final String dslName = "DJR_6620";
        String groupDataSetName_1 = "Default";
        ParentDsImporter.process(ImportResources.create(services, va_name, excelFolder, parentFileName,
                groupDataSetName_1), false);
        ChildDsImporter.process(ImportResources.create(services, va_name, excelFolder, childFileName,
                groupDataSetName_1), dslName, false);
        VisibilityArea va = services.va.getAll().stream()
                .filter(visibilityArea -> visibilityArea.getName().equals(va_name))
                .findAny().get();
        Map<String, DataSetList> dataSetLists = va.getDataSetLists().stream()
                .collect(Collectors.toMap(Named::getName, dataSetList -> dataSetList));
        DataSetList dsl = dataSetLists.get(dslName);
        DataSet ds = dsl.getDataSets().iterator().next();
        DataSetList customer = dataSetLists.get("Customer");
        Attribute dslIntoCustomer = dsl.getAttributes().stream().filter(attr -> "Customer".equals(attr.getName()))
                .findAny
                        ().get();
        Attribute lastName = customer.getAttributes().stream().filter(attr -> "lastName".equals(attr.getName()))
                .findAny().get();
        Attribute accountNumber = customer.getAttributes().stream().filter(attr -> "accountNumber".equals(attr
                .getName()))
                .findAny().get();
        String accountNumberValue = OverlapIterator.create(ds, accountNumber.getId(), Collections.singleton
                (dslIntoCustomer.getId()
                )).next().asReachable().getValue().get();
        assertEquals("$EXECUTION_REQUEST_NUMBER()", accountNumberValue);
        String lastNameValue = OverlapIterator.create(ds, lastName.getId(), Collections.singleton(dslIntoCustomer
                .getId()
        )).next().asReachable().getValue().get();
        assertEquals("AT#CONTEXT(LOGIN_TENANT)", lastNameValue);
    }

    /**
     * returns null when parameter is not found
     */
    private @Nullable Parameter getParameter(DataSet ds, String attributeName) {
        ds.getDataSetList().getAttributes().stream().filter(a -> a.getName().equals(attributeName)).findFirst().get();
        return ds.getParameters().stream()
                .filter(p -> p.getAttribute().getName().equals(attributeName))
                .findFirst()
                .orElse(null);
    }

    @Test
    @Disabled//for demo
    public void import_ISP_noIP_dataset_Test() throws Exception {
        String va_name = "ISP_noIP_1";
        String excelFolder = "TEST_DATA/";
        String parentFileName = excelFolder + "ISP_noIP.xlsx";
        ParentDsImporter.process(ImportResources.create(services, va_name, excelFolder, parentFileName, "Default"),
                false);
    }

    //-----------------------------------------------------------------------------------------------------------
//                                                     import to the existed parent
//-----------------------------------------------------------------------------------------------------------
    @Test
    @Disabled
    public void import_child_to_existed_parent() throws Exception {
        final String va_name = "EVPL TEST NEW LOGIC";
        String excelFolder = "TEST_DATA/test_data_1/";
        String childFilePath = excelFolder + "EVPL Child 18_2.xlsx";
        ChildDsImporter.process(ImportResources.create(services, va_name, excelFolder, childFilePath, "Default"),
                "EVPL_Root", false);
    }
    //-----------------------------------------------------------------------------------------------------------
    //                                                     parent import
    //-----------------------------------------------------------------------------------------------------------

    @Test
    @Disabled
    public void testExcelImportParentNew() throws Exception {
        final String va_name = "EVPL TEST NEW LOGIC";
        final String excelDataFolder = "TEST_DATA/test_data_1";
        final String parentToBeProcessed = excelDataFolder + "/EVPL Parent.xlsx";
        ParentDsImporter.process(ImportResources.create(services, va_name, excelDataFolder, parentToBeProcessed,
                "Default"), false);
    }
}
