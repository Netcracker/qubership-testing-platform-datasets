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

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.service.direct.helper.CreationFacade;
import org.qubership.atp.dataset.service.direct.helper.SimpleCreationFacade;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManAttribute;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManDataSetList;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManParameter;

public class UtilsTest {

    private static CreationFacade CREATE = SimpleCreationFacade.INSTANCE;

    @Test
    public void test_When_Serialize_Parameter_Ref_Value_Is_Set_For_Ds_param() {
        VisibilityArea va = CREATE.va("test_ds_ref_va");
        //region source dsl/ds
        DataSetList dsl_source = CREATE.dsl(va, "test_dsl");
        DataSet source = CREATE.ds(dsl_source, "test_ds_ref_ds_source");
        DataSetList dslRef = CREATE.dsl(va, "test_dsl_ref");
        Attribute textAttr = CREATE.attr(dslRef, "text_attr", AttributeType.TEXT, null, null);
        //endregion
        //region references
        DataSet ref = CREATE.ds(dslRef, "test_ds_ref_ds_ref");
        CREATE.textParam(ref, textAttr, "text");
        CREATE.refParam(source, "ref", ref);
        //endregion
        //region convert Dsl to UiManDsl
        UiManDataSetList uiManDataSetList = Utils.doUiDs(dsl_source, EvaluatorMock.INSTANCE, true);
        List<UiManAttribute> attributes = uiManDataSetList.getAttributes();
        UiManAttribute manAttribute = attributes.get(0);
        UiManParameter manParameter = manAttribute.getParameters().get(0);
        //endregion
        Assertions.assertEquals(ref.getId(), manParameter.getValueRef());
        Assertions.assertEquals(1, manAttribute.getAttributes().size());
    }

    @Test
    public void test_thenRefValueIsSetForDsParam_expandAllDisabled_whenRefDsAttributesNotPresent() {
        VisibilityArea va = CREATE.va("test_va");
        //region source dsl/ds
        DataSetList dsl_source = CREATE.dsl(va, "test_dsl");
        DataSet source = CREATE.ds(dsl_source, "test_ds_ref_ds_source");
        DataSetList dslRef = CREATE.dsl(va, "test_dsl_ref");
        Attribute textAttr = CREATE.attr(dslRef, "text_attr", AttributeType.TEXT, null, null);
        //endregion
        //region references
        DataSet ref = CREATE.ds(dslRef, "test_ds_ref_ds_ref");
        CREATE.textParam(ref, textAttr, "text");
        CREATE.refParam(source, "ref", ref);
        //endregion
        //region convert Dsl to UiManDsl
        UiManDataSetList uiManDataSetList = Utils.doUiDs(dsl_source, EvaluatorMock.INSTANCE, false);
        List<UiManAttribute> attributes = uiManDataSetList.getAttributes();
        UiManAttribute manAttribute = attributes.get(0);
        UiManParameter manParameter = manAttribute.getParameters().get(0);
        //endregion
        Assertions.assertEquals(ref.getId(), manParameter.getValueRef());
        Assertions.assertEquals(0, manAttribute.getAttributes().size());
    }

    @Test
    public void test_When_Serialize_Parameter_RefValue_Is_Set_For_List_Value() {
        //region create test data in db
        VisibilityArea va = CREATE.va("test_lv_ref_va");
        DataSetList dsl = CREATE.dsl(va, "test_lv_ref_dsl");
        DataSet ds = CREATE.ds(dsl, "test_lv_ref_ds");
        String list_val = "list_val";
        Parameter parameter = CREATE.listParam(ds, "list_value_attr", list_val, list_val);
        ListValue listValue = parameter.getAttribute().getListValues().get(0);
        //endregion
        //region serialize object
        UiManDataSetList manDataSetList = Utils.doUiDs(dsl, EvaluatorMock.INSTANCE, true);
        UiManAttribute manAttribute = manDataSetList.getAttributes().get(0);
        UiManParameter manParameter = manAttribute.getParameters().get(0);
        //endregion
        Assertions.assertEquals(listValue.getId(), manParameter.getValueRef());
    }
}
