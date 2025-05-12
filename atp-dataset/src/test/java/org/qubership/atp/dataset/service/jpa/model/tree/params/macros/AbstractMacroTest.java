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

package org.qubership.atp.dataset.service.jpa.model.tree.params.macros;

import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.TestUtils.getCharsMacros;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.TestUtils.getCharsUpperCaseMacros;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.TestUtils.getInnMacros;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.TestUtils.getRandMacros;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.TestUtils.getUuidMacros;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.script.ScriptEngineManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.qubership.atp.dataset.antlr4.TextParameterParser;
import org.qubership.atp.dataset.db.jpa.entities.DataSetListEntity;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.impl.macro.CachedDslMacroResultContainer;
import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;
import org.qubership.atp.dataset.service.jpa.model.MacroContextService;
import org.qubership.atp.dataset.service.jpa.model.PathStep;
import org.qubership.atp.dataset.service.jpa.model.dscontext.DataSetListContext;
import org.qubership.atp.macros.core.calculator.ScriptMacrosCalculator;

@ExtendWith(SpringExtension.class)
public abstract class AbstractMacroTest {

    private static final UUID DSL_ID = UUID.randomUUID();
    protected static final String MACRO_RESULT = "MY TEXT";

    protected TextParameterParser parser;

    @BeforeEach
    public void setUp() {
        MacroContext macroContext = new MacroContext();
        macroContext.setMacroContextService(new TestMacroContextService());
        macroContext.setDataSetListContext(new DataSetListContext(DSL_ID));
        macroContext.setMacrosCalculator(new ScriptMacrosCalculator(new ScriptEngineManager()));
        macroContext.setMacros(
            asList(getUuidMacros(), getCharsUpperCaseMacros(), getCharsMacros(), getInnMacros(), getRandMacros()));
        ParameterPositionContext parameterPositionContext =
            new ParameterPositionContext(emptyList(), 0, null, 0L, DSL_ID);
        parser = new TextParameterParser(macroContext, parameterPositionContext);
    }

    private static class TestMacroContextService implements MacroContextService {

        @Override
        public String getTextParameterByListDataSetAndPath(UUID visibilityAreaId,
                                                           PathStep dataSetList,
                                                           PathStep dataSet,
                                                           List<PathStep> referenceAttributePath,
                                                           PathStep parameterAttribute) {
            return MACRO_RESULT;
        }

        @Override
        public String getTextParameterByExternalListDataSetAndPath(UUID visibilityAreaId,
                                                                   PathStep dataSetList,
                                                                   PathStep dataSet,
                                                                   List<PathStep> referenceAttributePath,
                                                                   PathStep parameterAttribute) {
            return MACRO_RESULT;
        }

        @Override
        public String getTextParameterFromCachedContextByNamesPath(UUID visibilityAreaId,
                                                                   PathStep topLevelDataSetList,
                                                                   UUID dataSetId,
                                                                   int dataSetColumn,
                                                                   List<UUID> macroPosition,
                                                                   List<PathStep> pathSteps,
                                                                   PathStep attribute) {
            return MACRO_RESULT;
        }

        @Override
        public String getDataSetListName(UUID dataSetListId) {
            return MACRO_RESULT;
        }

        @Override
        public String getDataSetName(UUID dataSetListId) {
            return MACRO_RESULT;
        }

        @Override
        public UUID getDataSetUuid(String dataSetName, UUID dataSetListId) {
            return null;
        }

        @Override
        public DataSetList getDataSetList(UUID visibilityAreaId, PathStep dataSetListPathStep) {
            DataSetListEntity entity = new DataSetListEntity();
            entity.setId(DSL_ID);
            return new DataSetList(entity);
        }

        @Override
        public String getAttributeName(UUID dataSetListId) {
            return MACRO_RESULT;
        }

        @Override
        public Map<ParameterPositionContext, String> getCachedEvaluatedValues(UUID dataSetListId) {
            return null;
        }

        @Override
        public CachedDslMacroResultContainer getDslMacroCache() {
            return new CachedDslMacroResultContainer();
        }

        @Override
        public void dropLocalThreadCache() {

        }
    }
}
