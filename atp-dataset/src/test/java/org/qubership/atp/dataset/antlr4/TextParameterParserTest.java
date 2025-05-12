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

package org.qubership.atp.dataset.antlr4;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.qubership.atp.dataset.db.jpa.entities.DataSetListEntity;
import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.impl.macro.CachedDslMacroResultContainer;
import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;
import org.qubership.atp.dataset.service.jpa.model.MacroContextService;
import org.qubership.atp.dataset.service.jpa.model.PathStep;
import org.qubership.atp.dataset.service.jpa.model.dscontext.DataSetListContext;
import org.qubership.atp.dataset.service.jpa.model.tree.params.AbstractTextParameter;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.ParameterPositionContext;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.RefDslMacro;

@ExtendWith(SpringExtension.class)
public class TextParameterParserTest {
    private static MacroContext macroContext;
    public static String MACRO_RESULT = "MY TEXT";
    public static UUID DATA_SET_LIST_ID = UUID.randomUUID();
    public static UUID DATA_SET_ID = UUID.randomUUID();
    @Mock
    CachedDslMacroResultContainer dslMacroResultContainer;

    @BeforeEach
    public void setUp() {
        macroContext = new MacroContext();
        macroContext.addAtpDataSetContext("{'ER_NAME': 'Some Er Name'}");
        when(dslMacroResultContainer.getCachedValue(any(), any(), any(), any())).thenReturn(null);
        doNothing().when(dslMacroResultContainer).storeValue(any(), any(), any(), any(), any());
        MacroContextService macroContextService = new MacroContextService() {
            @Override
            public String getTextParameterByListDataSetAndPath(UUID visibilityAreaId, PathStep dataSetList,
                                                               PathStep dataSet, List<PathStep> referenceAttributePath,
                                                               PathStep parameterAttribute)
                    throws DataSetServiceException {
                return MACRO_RESULT;
            }

            @Override
            public String getTextParameterByExternalListDataSetAndPath(UUID visibilityAreaId, PathStep dataSetList,
                                                                       PathStep dataSet,
                                                                       List<PathStep> referenceAttributePath,
                                                                       PathStep parameterAttribute)
                    throws DataSetServiceException {
                return MACRO_RESULT;
            }

            @Override
            public String getTextParameterFromCachedContextByNamesPath(UUID visibilityAreaId,
                                                                       PathStep topLevelDataSetList, UUID dataSetId,
                                                                       int dataSetColumn, List<UUID> macroPosition,
                                                                       List<PathStep> pathSteps, PathStep attribute)
                    throws DataSetServiceException {
                return MACRO_RESULT;
            }

            @Override
            public String getDataSetListName(UUID dataSetListId) throws DataSetServiceException {
                return MACRO_RESULT;
            }

            @Override
            public String getDataSetName(UUID dataSetListId) throws DataSetServiceException {
                return MACRO_RESULT;
            }

            @Override
            public UUID getDataSetUuid(String dataSetName, UUID dataSetListId) {
                return DATA_SET_ID;
            }

            @Override
            public DataSetList getDataSetList(UUID visibilityAreaId, PathStep dataSetListPathStep)
                    throws DataSetServiceException {
                DataSetListEntity dataSetListEntity = new DataSetListEntity();
                dataSetListEntity.setId(DATA_SET_LIST_ID);
                return new DataSetList(dataSetListEntity);
            }

            @Override
            public String getAttributeName(UUID dataSetListId) throws DataSetServiceException {
                return MACRO_RESULT;
            }

            @Override
            public Map<ParameterPositionContext, String> getCachedEvaluatedValues(UUID dataSetListId) {
                return null;
            }

            @Override
            public CachedDslMacroResultContainer getDslMacroCache() {
                return dslMacroResultContainer;
            }

            @Override
            public void dropLocalThreadCache() {

            }
        };
        macroContext.setMacroContextService(macroContextService);
        DataSetListContext dataSetListContext = new DataSetListContext(null);
        macroContext.setDataSetListContext(dataSetListContext);
    }

    @Test
    public void TestParseMacroService() {
        ParameterPositionContext parameterPositionContext =
                new ParameterPositionContext(Collections.emptyList(), 0, null, 0L, null);
        TextParameterParser parser = new TextParameterParser(macroContext, parameterPositionContext);
        String parameterValue =
                "text #REF_DSL(#REF(ce8a95b5-2bf4-47c7-9fc3-fba56df0fa52.ce8a95b5-2bf4-47c7-9fc3-fba56df0fa52)" +
                        ".#REF(ce8a95b5-2bf4-47c7-9fc3-fba56df0fa52.ce8a95b5-2bf4-47c7-9fc3-fba56df0fa52)some" +
                        ".#REF(ce8a95b5-2bf4-47c7-9fc3-fba56df0fa52.ce8a95b5-2bf4-47c7-9fc3-fba56df0fa52))";
        List<AbstractTextParameter> parseResult = parser.parse(parameterValue, false);
        StringBuilder result = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseResult) {
            result.append(abstractTextParameter.getValue());
        }
        Assertions.assertEquals(parameterValue, result.toString());
    }

    @Test
    public void TestParseMacroService_quoteIsNotLost() {
        ParameterPositionContext parameterPositionContext =
                new ParameterPositionContext(Collections.emptyList(), 0, null, 0L, null);
        TextParameterParser parser = new TextParameterParser(macroContext, parameterPositionContext);
        String parameterValue = " AAA ' BBB \" CCC , DDD $RANDOM('aa', 'bb \\'cc\\'') \\ EEE $RANDOM('$RANDOM('df')')";
        List<AbstractTextParameter> parseResult = parser.parse(parameterValue, false);
        StringBuilder result = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseResult) {
            result.append(abstractTextParameter.getValue());
        }
        Assertions.assertEquals(parameterValue, result.toString());
    }

    @Test
    public void testParseMacroServiceCollectsDataSetsAndDataSetListsId() {
        ParameterPositionContext parameterPositionContext =
                new ParameterPositionContext(Collections.emptyList(), 0, null, 0L, null);
        TextParameterParser parser = new TextParameterParser(macroContext, parameterPositionContext);
        String parameterValue = "text #REF_DSL(#REF_DSL(ce8a95b5-2bf4-47c7-9fc3-fba56df0fa52"
                + ".ce8a95b5-2bf4-47c7-9fc3-fba56df0fa52.#REF(ce8a95b5-2bf4-47c7-9fc3-fba56df0fa52.ce8a95b5-2bf4-47c7-9fc3-fba56df0fa52))"
                + ".#REF(ce8a95b5-2bf4-47c7-9fc3-fba56df0fa52.ce8a95b5-2bf4-47c7-9fc3-fba56df0fa52)some"
                + ".#REF(ce8a95b5-2bf4-47c7-9fc3-fba56df0fa52.ce8a95b5-2bf4-47c7-9fc3-fba56df0fa52))";
        List<AbstractTextParameter> parseResult = parser.parse(parameterValue, true);

        for (AbstractTextParameter abstractTextParameter : parseResult) {
            abstractTextParameter.getValue();
        }
        Assertions.assertTrue(parseResult.get(1) instanceof RefDslMacro);
        RefDslMacro dslMacro = (RefDslMacro) parseResult.get(1);
        Assertions.assertEquals(1, dslMacro.getDataSets().size());
        Assertions.assertEquals(1, dslMacro.getDataSetLists().size());
        Assertions.assertTrue(dslMacro.getDataSets().contains(DATA_SET_ID));
        Assertions.assertTrue(dslMacro.getDataSetLists().contains(DATA_SET_LIST_ID));
    }

    @Test
    public void TestParseUnknownMacro_MacroStartsWithDollarSign_MacroNameHasNotChanged() {
        ParameterPositionContext parameterPositionContext =
                new ParameterPositionContext(Collections.emptyList(), 0, null, 0L, null);
        TextParameterParser parser = new TextParameterParser(macroContext, parameterPositionContext);
        String parameterValue = "$EXECUTION_REQUEST_NUMBER()";
        List<AbstractTextParameter> parseResult = parser.parse(parameterValue, true);
        StringBuilder result = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseResult) {
            result.append(abstractTextParameter.getValue());
        }
        Assertions.assertEquals(parameterValue, result.toString());
    }

    @Test
    public void TestParseMacroService_noParamsMacro() {
        ParameterPositionContext parameterPositionContext = new ParameterPositionContext(
                Collections.emptyList(), 0, null, 0L, null
        );
        TextParameterParser parser = new TextParameterParser(macroContext, parameterPositionContext);
        String parameterValue = "#INN()";
        List<AbstractTextParameter> parseResult = parser.parse(parameterValue, false);
        StringBuilder result = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseResult) {
            result.append(abstractTextParameter.getValue());
        }
        Assertions.assertEquals(parameterValue, result.toString());
    }
}
