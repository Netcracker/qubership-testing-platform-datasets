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

package org.qubership.atp.dataset.model.utils;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.function.Supplier;

import org.qubership.atp.dataset.model.*;
import org.qubership.atp.dataset.model.*;
import org.qubership.atp.dataset.service.direct.AliasWrapperService;
import org.qubership.atp.dataset.service.direct.helper.CreationFacade;

public class TestData {

    private static final String SHORT_STRING = "1";
    private static final String STRING = "SIMPLE_TEXT";
    private static final String LONG_STRING = "LONG LONG LONG LONG LONG LONG LONG LONG LONG LONG";
    private static final String RANDOM_FORMULA = "#RANDOMBETWEEN(10, 100)";

    public static VisibilityArea bensCases(CreationFacade factory) {
        return createBensCases(factory, "BensCases").getVisibilityArea();
    }

    public static DataSetList createBensCases(CreationFacade factory, String vaName) {
        VisibilityArea va = factory.va(vaName);
        //region Postal Code
        DataSet pc1 = factory.ds(va, "Postal Code", "PC #001");
        DataSet pc2 = factory.ds(pc1.getDataSetList(), "PC #002");
        Parameter pc1First = factory.textParam(pc1, "First", "A1A");
        Parameter pc2First = factory.textParam(pc2, pc1First.getAttribute(), "B2B");
        Parameter pc1FirstLowerCase = factory.textParam(pc1, "FirstLowerCase", "a1a");
        Parameter pc2FirstLowerCase = factory.textParam(pc2, pc1FirstLowerCase.getAttribute(), "b2b");
        Parameter pc1Second = factory.textParam(pc1, "Second", "1A1");
        Parameter pc2Second = factory.textParam(pc2, pc1Second.getAttribute(), "2B2");
        //region ATPII-581
        DataSet pc3 = factory.ds(pc1.getDataSetList(), "PC #003");
        Parameter pc3First = factory.textParam(pc3, pc1First.getAttribute(), "C3C");
        Parameter pc3Second = factory.textParam(pc3, pc1Second.getAttribute(), "3C3");
        //endregion
        //region ATPII-862
        Attribute pcStatus = factory.listAttr(pc1.getDataSetList(), "Status",
                "Active", "Pending", "Disconnected");
        Parameter pc1Status = factory.listParam(pc1, pcStatus, "Disconnected");
        Parameter pc2Status = factory.listParam(pc2, pcStatus, "Disconnected");
        Parameter pc3Status = factory.listParam(pc3, pcStatus, "Disconnected");
        //endregion
        //endregion
        //region Address
        DataSet addr1 = factory.ds(va, "Address", "ADR #001");
        DataSet addr2 = factory.ds(addr1.getDataSetList(), "ADR #002");
        Parameter addr1Province = factory.textParam(addr1, "Provice", "Ontario");
        Parameter addr2Province = factory.textParam(addr2, addr1Province.getAttribute(), "Québec");
        Parameter addr1Municipality = factory.textParam(addr1, "Municipality", "BROCKVILLE");
        Parameter addr2Municipality = factory.textParam(addr2, addr1Municipality.getAttribute(), "MONTRÉAL");
        Parameter addr1Pc1 = factory.refParam(addr1, "Postal Code", pc1);
        Parameter addr2Pc2 = factory.refParam(addr2, addr1Pc1.getAttribute(), pc2);
        Parameter addr1Street = factory.textParam(addr1, "Street", "ELMSLEY");
        Parameter addr2Street = factory.textParam(addr2, addr1Street.getAttribute(), "HENRI-BOURASSA");
        Parameter addr1Market = factory.textParam(addr1, "Market", "Ontario > Burloak");
        Parameter addr2Market = factory.textParam(addr2, addr1Market.getAttribute(), "Quebec > Telus");
        factory.overrideParam(addr1, pc1Second.getAttribute(), "1A1 Overridden",
                null, null, null, addr1Pc1.getAttribute());
        //endregion
        //region Customer
        DataSet customer1 = factory.ds(va, "Customer", "Customer 1");
        DataSet customer2 = factory.ds(customer1.getDataSetList(), "Customer 2");
        Parameter cust1Name = factory.textParam(customer1, "Name", "Ivan");
        Parameter cust2Name = factory.textParam(customer2, cust1Name.getAttribute(), "Boris");
        Parameter cust1Addr1 = factory.refParam(customer1, "Address", addr1);
        Parameter cust2Addr2 = factory.refParam(customer2, cust1Addr1.getAttribute(), addr2);
        Parameter cust1SourceLoc = factory.textParam(customer1, "Source Location", "a");
        Parameter cust2SourceLoc = factory.textParam(customer2, cust1SourceLoc.getAttribute(), "a2");
        Parameter cust1TargetLoc = factory.textParam(customer1, "Target Location", "ae");
        Parameter cust2TargetLoc = factory.textParam(customer2, cust1TargetLoc.getAttribute(), "ae2");
        Parameter cust1Category = factory.textParam(customer1, "Customer Category", "a");
        Parameter cust2Category = factory.textParam(customer2, cust1Category.getAttribute(), "a2");
        //endregion
        //region Input
        DataSet input1 = factory.ds(va, "Input", "Modify Internet + Phone");
        DataSet input2 = factory.ds(input1.getDataSetList(), "New Internet + Digital TV");
        // ATPII-1369
        String longDataSetName = "Res WS (AccessPoint, WF_action != keine Montage, with ER WO, w/o Arbeitsauftrag) " +
                "18_3 - Copy Res WS (AccessPoint, WF_action != keine Montage, with ER WO, w/o Arbeitsauftrag)18_3";
        DataSet input4 = factory.ds(input1.getDataSetList(), longDataSetName);
        Parameter input1BaId = factory.textParam(input1, "Billing Account ID", "BA.033");
        Parameter input2BaId = factory.textParam(input2, input1BaId.getAttribute(), "BA.027");
        Parameter input1Cust1 = factory.refParam(input1, "Customer", customer1);
        Parameter input2Cust2 = factory.refParam(input2, input1Cust1.getAttribute(), customer2);
        //endregion
        //region Output
        DataSetList output = factory.dsl(va, "Output");
        DataSet output1 = factory.ds(output, "Modify Internet + Phone");
        DataSet output2 = factory.ds(output, "New Internet + Digital TV");
        factory.listAttr(output, "Net Charge Cost", (List<String>) null);
        factory.listAttr(output, "Total Charge Tax", (List<String>) null);
        factory.listAttr(output, "Total Charge Cost", (List<String>) null);
        //endregion
        //region E2E
        DataSet e1 = factory.ds(va, "E2E", "E2E_CC_004_TC#001");
        DataSet e2 = factory.ds(e1.getDataSetList(), "E2E_PoS_001_TC#09");
        Parameter e1Input1 = factory.refParam(e1, "Input", input1);
        Parameter e2Input2 = factory.refParam(e2, e1Input1.getAttribute(), input2);
        Parameter e1Output1 = factory.refParam(e1, "Output", output1);
        Parameter e2Output2 = factory.refParam(e2, e1Output1.getAttribute(), output2);
        //endregion
        return e1.getDataSetList();
    }

    public static VisibilityArea createFileAttribute(CreationFacade factory) {
        VisibilityArea va = factory.va("FileAttributesVA");

        DataSet fa1 = factory.ds(va, "File Attributes", "FA #001");

        return va;
    }

    public static VisibilityArea renametestPlan(CreationFacade factory) {
        VisibilityArea va = factory.va("ATPII-5119");
        return va;
    }

    public static VisibilityArea addTestPlan(CreationFacade factory) {
        VisibilityArea va = factory.va("ATPII-5119-2");
        DataSetList dsl = factory.dsl(va, "datasetlistForTesPlan");

        TestPlan testPlan = factory.testPlan(va, "TP");

        //for manager
        TestPlan testPlanWithDsl = factory.testPlan(va, "TP2");
        TestPlan testPlanWithoutDsl = factory.testPlan(va, "TP3");

        DataSetList dsl2 = factory.dsl(va, "datasetlistForTesPlan2", testPlanWithDsl); //inside testplan
        DataSetList dsl3 = factory.dsl(va, "datasetlistForTesPlan3", testPlanWithDsl); //inside testplan
        DataSetList dslWithoutTP = factory.dsl(va, "dsl"); //should not be visible
        return va;
    }
    /**
     * ATPII-665, ATPII-1377
     */
    public static VisibilityArea createListValues(CreationFacade factory) {
        VisibilityArea va = factory.va("ATPII-665");
        DataSet lv1 = factory.ds(va, "List Values", "LV #001");
        DataSet lv2 = factory.ds(lv1.getDataSetList(), "LV #002");
        DataSet lv3 = factory.ds(lv1.getDataSetList(), "LV #003");
        DataSet lv4 = factory.ds(lv1.getDataSetList(), "LV #004");
        Attribute lvStatus = factory.listAttr(lv1.getDataSetList(), "Status",
                "Active", "Pending", "Disconnected");
        Attribute lvAction = factory.listAttr(lv1.getDataSetList(), "Action",
                "New", "Copy", "Move", "Delete");
        Attribute lvType = factory.listAttr(lv1.getDataSetList(), "Type",
                "Integer", "String", "Boolean");
        Attribute lvDelete = factory.listAttr(lv1.getDataSetList(), "Delete",
                "WFM Core settings", "WFM Rule Catalog", "WFM Time Grid", "WFM Function Catalog");
        Parameter lv1Status = factory.listParam(lv1, lvStatus, "Active");
        Parameter lv2Status = factory.listParam(lv2, lvStatus, "Active");
        Parameter lv3Status = factory.listParam(lv3, lvStatus, "Active");
        Parameter lv1Action = factory.listParam(lv1, lvAction, "Copy");
        Parameter lv2Action = factory.listParam(lv2, lvAction, "Move");
        Parameter lv3Action = factory.listParam(lv3, lvAction, "Delete");
        Parameter lv1Type = factory.listParam(lv1, lvType, "Boolean");
        Parameter lv2Type = factory.listParam(lv2, lvType, "String");
        Parameter lv3Type = factory.listParam(lv3, lvType, "Integer");
        Parameter lv1Delete = factory.listParam(lv1, lvDelete, "WFM Core settings");
        Parameter lv2Delete = factory.listParam(lv2, lvDelete, "WFM Core settings");
        Parameter lv3Delete = factory.listParam(lv3, lvDelete, "WFM Core settings");
        for (int i = 0; i < 20; i++) {
            Attribute lv = factory.listAttr(lv1.getDataSetList(), "Status" + i,
                    "Active", "Pending", "Disconnected");
            factory.listParam(lv1, lv, "Active");
        }
        return va;
    }

    /**
     * ATPII-877, ATPII-804
     */
    public static VisibilityArea createLabels(CreationFacade factory) {
        VisibilityArea va = factory.va("ATPII-877");
        DataSet dataSetLabels1 = factory.ds(va, "Labels", "DS Label 1");
        DataSet dataSetLabels2 = factory.ds(dataSetLabels1.getDataSetList(), "DS Label 2");
        Parameter labels1MContact = factory.textParam(dataSetLabels1, "Marketing Contact", "Yes");
        Parameter labels2MContact = factory.textParam(dataSetLabels2, labels1MContact.getAttribute(), "No");
        Parameter labels1FNationId = factory.textParam(dataSetLabels1, "First Nation ID", "Regular");
        Parameter labels2FNationId = factory.textParam(dataSetLabels2, labels1FNationId.getAttribute(), "No");
        factory.label(dataSetLabels1.getDataSetList(), "DSL#001");
        factory.label(dataSetLabels1.getDataSetList(), "DSL#002");
        factory.label(dataSetLabels1, "DS#001"); // Label for filter test
        factory.label(dataSetLabels1, "DS#002"); // Label for filter test
        factory.label(dataSetLabels1, "DS#003"); // Label for delete test
        factory.label(dataSetLabels2, "DS#001"); // Label for filter test
        return va;
    }

    /**
     * ATPII-1130
     */
    public static VisibilityArea createVAFor1130(CreationFacade facade) {
        VisibilityArea va = facade.va("ATPII-1130");
        //DSL 1 with list attribute
        DataSetList dsl1 = facade.dsl(va, "DSL1");
        Attribute dsl1ListAttr = facade.listAttr(dsl1, "LIST_ATTR_DSL1",
                asList(SHORT_STRING, LONG_STRING, STRING, RANDOM_FORMULA));
        //DSL 2 with text attribute
        DataSetList dsl2 = facade.dsl(va, "DSL2");
        Attribute dsl2TextAttr = facade.textAttr(dsl2, "TEXT_ATTR_DSL2");
        //DSL_1 > DSL_2
        Attribute dsl2Ref = facade.refAttr(dsl1, "DSL_REF_DSL2", dsl2);
        //dsl 1 has 2 datasets
        DataSet ds_1_dsl1 = facade.ds(dsl1, "DS_1_DSL1");
        DataSet ds_2_dsl1 = facade.ds(dsl1, "DS_2_DSL1");
        //dsl 2 has 2 datasets
        DataSet ds_1_dsl2 = facade.ds(dsl2, "DS_1_DSL2");
        DataSet ds_2_dsl2 = facade.ds(dsl2, "DS_2_DSL2");
        DataSet ds_3_dsl2 = facade.ds(dsl2, "DS_3_DSL2");
        //set text params to child 2 DS
        facade.textParam(ds_1_dsl2, dsl2TextAttr, SHORT_STRING);
        facade.textParam(ds_2_dsl2, dsl2TextAttr, RANDOM_FORMULA);
        facade.textParam(ds_3_dsl2, dsl2TextAttr, LONG_STRING);
        //set list params to DSL 1
        facade.listParam(ds_1_dsl1, dsl1ListAttr, RANDOM_FORMULA);
        facade.listParam(ds_2_dsl1, dsl1ListAttr, SHORT_STRING);
        //ref from DSL 1 to DSL 2
        facade.refParam(ds_1_dsl1, dsl2Ref, ds_1_dsl2);
        facade.refParam(ds_2_dsl1, dsl2Ref, ds_1_dsl2);
        return va;
    }

    /**
     * ATPII-1533
     */
    public static VisibilityArea createTestDataFor1153(CreationFacade facade) {
        VisibilityArea va = facade.va("ATPII-1153");
        //DSL 1 with one list attribute and empty list attribute
        DataSetList dsl1 = facade.dsl(va, "DSL1");
        Attribute dsl1ListAttr = facade.listAttr(dsl1, "LIST_ATTR_DSL1",
                asList(SHORT_STRING, LONG_STRING, STRING, RANDOM_FORMULA));
        Attribute dsl1EmptyListAttr = facade.listAttr(dsl1, "EMPTY_LIST_ATTR_DSL1", "");
        //DSL 2 with text attribute
        DataSetList dsl2 = facade.dsl(va, "DSL2");
        Attribute dsl2TextAttr = facade.textAttr(dsl2, "TEXT_ATTR_DSL2");
        //DSL_1 > DSL_2
        Attribute dsl2Ref = facade.refAttr(dsl1, "DSL_REF_DSL2", dsl2);
        //dsl 1 has 2 datasets
        DataSet ds_1_dsl1 = facade.ds(dsl1, "DS_1_DSL1");
        DataSet ds_2_dsl1 = facade.ds(dsl1, "DS_2_DSL1");
        DataSet ds_3_dsl1 = facade.ds(dsl1, "DS_3_DSL1");
        //dsl 2 has 2 datasets
        DataSet ds_1_dsl2 = facade.ds(dsl2, "DS_1_DSL2");
        DataSet ds_2_dsl2 = facade.ds(dsl2, "DS_2_DSL2");
        DataSet ds_3_dsl2 = facade.ds(dsl2, "DS_3_DSL2");
        //set text params to child 2 DS
        facade.textParam(ds_1_dsl2, dsl2TextAttr, SHORT_STRING);
        facade.textParam(ds_2_dsl2, dsl2TextAttr, RANDOM_FORMULA);
        facade.textParam(ds_3_dsl2, dsl2TextAttr, LONG_STRING);
        //set list params to DSL 1
        facade.listParam(ds_1_dsl1, dsl1ListAttr, RANDOM_FORMULA);
        facade.listParam(ds_2_dsl1, dsl1ListAttr, SHORT_STRING);
        facade.listParam(ds_1_dsl1, dsl1EmptyListAttr, "");
        //ref from DSL 1 to DSL 2
        facade.refParam(ds_1_dsl1, dsl2Ref, ds_1_dsl2);
        facade.refParam(ds_2_dsl1, dsl2Ref, ds_1_dsl2);
        return va;
    }

    /**
     * ATPII-1566
     * <pre>
     * {
     * 	"Home": {
     * 		"Postal Code": {
     * 			"Code": "PC#OfHome",
     * 			"CodeLV": "PC#OfHome"
     *        }
     *    },
     * 	"Work": {
     * 		"Postal Code": {
     * 			"Code": "PC#OfWork", <-overlap under "Work"
     * 			"CodeLV": "PC#OfWork" <-overlap under "Work"
     *        }
     *    }
     * }
     * </pre>
     */
    public static DataSet customerWithAddressAndPostalCode(CreationFacade factory) {
        VisibilityArea va = factory.va("ATPII-1566");
        DataSet customer = factory.ds(va, "Customer", "Customer");
        DataSetList addressDsl = factory.dsl(va, "Address");
        DataSet home = factory.ds(addressDsl, "Home");
        DataSet work = factory.ds(addressDsl, "Work");
        DataSet postalCode = factory.ds(va, "Postal Code", "PC");
        Attribute addrToPC = factory.refAttr(addressDsl, "Postal Code", postalCode.getDataSetList());
        factory.refParam(home, addrToPC, postalCode);
        factory.refParam(work, addrToPC, postalCode);
        factory.refParam(customer, "Home", home);
        factory.refParam(customer, "Work", work);
        Parameter code = factory.textParam(postalCode, "Code", "PC#OfHome");
        Parameter codeLV = factory.listParam(postalCode, "CodeLV", "PC#OfHome",
                "PC#OfWork", "PC#OfHome", "PC#Unused");
        factory.overrideParam(work, code.getAttribute(), "PC#OfWork", null, null, null, addrToPC);
        factory.overrideParam(work, codeLV.getAttribute(), null, "PC#OfWork", null, null, addrToPC);
        return customer;
    }

    public static VisibilityArea dsWithListAndDsRef(CreationFacade facade) {
        VisibilityArea va = facade.va("ATPII-2121");
        DataSetList dsl = facade.dsl(va, "DSL");
        DataSetList dslRef = facade.dsl(va, "DSLref");
        DataSet ds = facade.ds(dsl, "DS");
        DataSet dsRef = facade.ds(dslRef, "DSref");
        Attribute refAttr = facade.refAttr(dsl, "refAttr", dslRef);
        facade.refParam(ds, refAttr, dsRef);


        DataSetList dslLV = facade.dsl(va, "DSLlv");
        DataSet dsLV = facade.ds(dslLV, "DS");
        Attribute listAttr = facade.listAttr(dslLV, "listAttr", "value1", "value2");
        facade.listParam(dsLV, listAttr, "value1");
        return va;
    }

    public static class RefToOverlappedText implements Supplier<VisibilityArea> {

        public final VisibilityArea va;
        public final DataSetList dsl;
        public final DataSet ds;
        public final Parameter dsIntoParams;
        public final Parameter dsIntoEdit;
        public final DataSetList paramsDsl;
        public final DataSet paramsDs;
        public final Parameter defaultCalling;
        public final String callingMacro = "#REF(ViWIFI - MOC.Params for editing.originCalling)";
        public final Parameter calling;
        public final DataSetList editDsl;
        public final DataSet editDs;
        public final Parameter defaultOriginCalling;
        public final Parameter needToConsume;
        public final Parameter originCalling;

        public RefToOverlappedText(CreationFacade create, AliasWrapperService service) {
            va = create.va("ATPII-4091");
            dsl = create.dsl(va, "ViWIFI_MOC");
            ds = create.ds(dsl, "ViWIFI - MOC");
            paramsDsl = create.dsl(va, "Sit_wifi");
            paramsDs = create.ds(paramsDsl, "DEFAULT_TEST");
            dsIntoParams = create.refParam(ds, "Params", paramsDs);
            defaultCalling = create.textParam(paramsDs, "Calling", null);
            editDsl = create.dsl(va, "IMS_SIT_editable_params");
            editDs = create.ds(editDsl, "DEFAULT_TEST");
            dsIntoEdit = create.refParam(ds, "Params for editing", editDs);
            defaultOriginCalling = create.textParam(editDs, "originCalling", "");
            needToConsume = create.textParam(editDs, "needToConsume", "181");
            originCalling = create.overrideParam(ds, defaultOriginCalling.getAttribute(),
                    "17385113644", null, null, null,
                    dsIntoEdit.getAttribute());
            String wrappedMacro = service.wrapToAlias(callingMacro, va, dsl);
            calling = create.overrideParam(ds, defaultCalling.getAttribute(), wrappedMacro, null, null, null, dsIntoParams.getAttribute());
        }

        @Override
        public VisibilityArea get() {
            return va;
        }
    }
}
