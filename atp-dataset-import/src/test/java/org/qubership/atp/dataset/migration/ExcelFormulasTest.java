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

import static org.qubership.atp.dataset.migration.formula.model.FormulaType.CONCATANATION_INTERNAL_REFERENCES;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.annotation.Nonnull;

import org.apache.poi.ss.usermodel.CellType;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.qubership.atp.dataset.migration.formula.ExcelFormulasEvaluator;
import org.qubership.atp.dataset.migration.formula.model.CellData;
import org.qubership.atp.dataset.migration.formula.model.EvaluationContext;
import org.qubership.atp.dataset.migration.formula.model.Formula;
import org.qubership.atp.dataset.migration.formula.model.FormulaType;
import org.qubership.atp.dataset.migration.formula.model.ParameterAssociation;
import org.qubership.atp.dataset.migration.formula.model.TransformationException;
import org.qubership.atp.dataset.migration.model.FalloutReport;
import org.qubership.atp.dataset.migration.model.OverlapParamContainer;
import org.qubership.atp.dataset.migration.model.ToOverlap;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.service.direct.helper.CreationFacade;
import org.qubership.atp.dataset.service.direct.helper.SimpleCreationFacade;

@Isolated
public class ExcelFormulasTest {

    private static CreationFacade CREATE = SimpleCreationFacade.INSTANCE;

    public static Collection<Object[]> data() {
        EvaluationContext context = EvaluationContext.getContext();
        DataSetList dataSetList = CREATE.dsl("va", "DATA_SET_LIST_NAME");
        DataSet dataset = CREATE.ds(dataSetList, "DATA_SET_NAME");
        Attribute attribute = CREATE.textAttr(dataSetList, "ATTR_NAME");
        Attribute groupAttribute = CREATE.textAttr(dataSetList, "GROUP_ATTR_NAME");
        Parameter parameter = CREATE.textParam(dataset, attribute, "REFERENCED_VALUE");
        OverlapParamContainer childGroup = new OverlapParamContainer() {

            @Override
            public Attribute getRefToDsl() {
                return groupAttribute;
            }

            @Override
            public Parameter getRefToDs() {
                return null;//not used for some reason
            }

            @Override
            public DataSet getGroupDs() {
                return dataset;
            }
        };
        ToOverlap parameterSup = new ToOverlap() {

            @Nonnull
            @Override
            public OverlapParamContainer getContainer() {
                return childGroup;
            }

            @Override
            public Attribute getAttributeToOverlap() {
                return attribute;
            }

            @Override
            public Parameter getParameterToOverlap() {
                return parameter;
            }
        };
        CellData data = new CellData("C26", "123", "C26", CellType.FORMULA);
        ParameterAssociation parameterAssoc = new ParameterAssociation(data, parameterSup);
        context.put("C26", parameterAssoc);
        context.put("AABB12344", parameterAssoc);
        return Arrays.asList(
                //simple text
                new Object[]{"\"123\"", FormulaType.CONSTANT_TEXT_VALUE, "123"},
                new Object[]{"\"\"", FormulaType.CONSTANT_TEXT_VALUE, ""},
                //random chars
                new Object[]{"CHAR(RANDBETWEEN(65,90))", FormulaType.RANDOM_CHAR, "#CHARS_UPPERCASE(1)"},
                new Object[]{"CHAR(RANDBETWEEN(65,90))&CHAR(RANDBETWEEN(65,90))&CHAR(RANDBETWEEN(65,90))",
                        CONCATANATION_INTERNAL_REFERENCES, "#CHARS_UPPERCASE(1)#CHARS_UPPERCASE(1)#CHARS_UPPERCASE(1)"},
                new Object[]{"CHAR(RANDBETWEEN(65,90))&CHAR(RANDBETWEEN(65,90))&CHAR(RANDBETWEEN(65,90))&\"123\"",
                        CONCATANATION_INTERNAL_REFERENCES, "#CHARS_UPPERCASE(1)#CHARS_UPPERCASE(1)#CHARS_UPPERCASE(1)123"},
                //internal reference as is
//                new Object[]{"C26", FormulaType.REFERENCE, "#REF_DSL(DATA_SET_LIST_NAME.DATA_SET_NAME.ATTR_NAME)"},
//                new Object[]{"AABB12344", FormulaType.REFERENCE, "#REF_DSL(DATA_SET_LIST_NAME.DATA_SET_NAME.ATTR_NAME)"},
                //empty string + string + random
//                new Object[]{"\"\" & \"asdf\"&RANDBETWEEN(123,45) & \"_\" & C26", CONCATANATION_INTERNAL_REFERENCES,
//                        "asdf#RANDOMBETWEEN(123, 45)_#REF_DSL(DATA_SET_LIST_NAME.DATA_SET_NAME.ATTR_NAME)"},
//                DS1_ATTR1 = ${REF_TO(DS2_ATTR2)}_${radbetween()}
//                DS1_ATTR2 = ${REF_TO(DS2_ATTR2)}_${radbetween()}
//                DS2_ATTR2 = ${REF_TO(DS2_ATTR2)}_${radbetween()}
                new Object[]{"RANDBETWEEN(123,435)", FormulaType.RANDOM, "#RANDOMBETWEEN(123, 435)"},
                new Object[]{"RANDBETWEEN(-123,-45)", FormulaType.RANDOM, "#RANDOMBETWEEN(-123, -45)"},
                new Object[]{"RANDBETWEEN(123,435)", FormulaType.RANDOM, "#RANDOMBETWEEN(123, 435)"},
                new Object[]{"RANDBETWEEN(123, 435)", FormulaType.RANDOM, "#RANDOMBETWEEN(123, 435)"},
                new Object[]{"RANDBETWEEN(40000000,49999999)", FormulaType.RANDOM, "#RANDOMBETWEEN(40000000, 49999999)"},
                //external references are saved as is
                new Object[]{"IF([1]IqSP!$C110=\"\",\"\",[1]IqSP!$C110)",
                        FormulaType.EXTERNAL_REFERENCE, "${IF([1]IqSP!$C110=\"\",\"\",[1]IqSP!$C110)}"},
                //dates formulas
                new Object[]{"CONCATENATE(TEXT(NOW(),\"yyyy-MM-dd\"),CONCATENATE(\"T\",CONCATENATE(TEXT(NOW(),\"HH:mm:ss\"),\".123Z\")))", FormulaType.DATE, "#DATE(yyyy-MM-dd'T'HH:mm:ss.123'Z')"},
                new Object[]{"CONCATENATE(TEXT(NOW(),\"yyyy-MM-dd\"),CONCATENATE(\"T\",CONCATENATE(TEXT(NOW(),\"HH:mm:ss\"),\"+03:00\")))", FormulaType.DATE, "#DATE(yyyy-MM-dd'T'HH:mm:ss+03:00)"},
                new Object[]{"CONCATENATE(TEXT(TODAY()+2,\"yyyy-MM-dd\"),CONCATENATE(\"T\",CONCATENATE(TEXT(TIME(12,0,1),\"HH:mm:ss\"),\".123Z\")))", FormulaType.DATE, "#DATE(+2d, yyyy-MM-dd'T'12:00:01.123'Z')"},
                new Object[]{"CONCATENATE(TEXT(TODAY()-24,\"yyyy-MM-dd\"),CONCATENATE(\"T\",CONCATENATE(TEXT(TIME(12,30,50),\"HH:mm:ss\"),\".123Z\")))", FormulaType.DATE, "#DATE(-24d, yyyy-MM-dd'T'12:30:50.123'Z')"},
                new Object[]{"CONCATENATE(TEXT(TODAY()-24,\"yyyy-MM-dd\"),CONCATENATE(\"T\",CONCATENATE(TEXT(TIME(12,30,50),\"HH:mm:ss\"),\".1qWERTy23ZxcvbN\")))", FormulaType.DATE, "#DATE(-24d, yyyy-MM-dd'T'12:30:50.1'qWERTy'23'ZxcvbN')"},
                new Object[]{"INDEX(Params!$H$2:$H$4;RANDBETWEEN(1; COUNTA(Params!$A$2:$A$4)))", FormulaType.LIST_VALUE, "test value"}
        );
    }

    @ParameterizedTest(name = "{index}: formula {0} is {1} and converts to {2}")
    @MethodSource("data")
    public void excelFormula_parsedToDatasetFormat_successfully(String excelFormula, FormulaType type,
                                                                String finalExpression) throws TransformationException,
            IOException {
        ExcelFormulasEvaluator formulas = new ExcelFormulasEvaluator(new FalloutReport("test.report.tsv"));
        Formula formula = formulas.getFormula(new CellData(excelFormula, "test value", "test location", CellType._NONE));
        assertEquals(finalExpression, formula.getDatasetValue());
        assertEquals(type, formula.getFormulaType());
    }
}
