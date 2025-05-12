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

import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.qubership.atp.dataset.antlr.DataSetParameterLexer;
import org.qubership.atp.dataset.antlr.DataSetParameterParser;
import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;
import org.qubership.atp.dataset.service.jpa.model.tree.params.AbstractTextParameter;
import org.qubership.atp.dataset.service.jpa.model.tree.params.TextParameter;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.ParameterPositionContext;
import org.qubership.atp.macros.core.parser.antlr4.MacrosLexer;
import org.qubership.atp.macros.core.parser.antlr4.MacrosParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TextParameterParser {
    private static final Logger LOG = LoggerFactory.getLogger(TextParameterParser.class);
    private MacroContext context;
    private ParameterPositionContext parameterPositionContext;

    public TextParameterParser(MacroContext macroContext, ParameterPositionContext parameterPositionContext) {
        this.context = macroContext;
        this.parameterPositionContext = parameterPositionContext;
    }

    /**
     * Parse steps to parameters and set evaluate flag.
     * */
    public List<AbstractTextParameter> parse(String textToParse, boolean setEvaluate) {
        return parseUsingExternalParser(textToParse, setEvaluate);
    }

    /**
     * Use embedded parser. Need to be used for regression prevention before atp-macro parser grammar changes.
     * */
    @Deprecated
    public List<AbstractTextParameter> parseUsingEmbeddedParser(String textToParse, boolean setEvaluate) {
        try {
            DataSetParameterLexer lexer = new DataSetParameterLexer(CharStreams.fromString(textToParse));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            DataSetParameterParser parser = new DataSetParameterParser(tokens);
            ParseTree tree = parser.parameter();
            ParseTreeWalker walker = new ParseTreeWalker();
            ParameterListener parameterListener = new ParameterListener(context, parameterPositionContext, setEvaluate);
            walker.walk(parameterListener, tree);
            return parameterListener.getParsedParameters();
        } catch (Exception e) {
            LOG.debug("Text parameter parse error", e);
            return Collections.singletonList(new TextParameter("Parse error", parameterPositionContext));
        }
    }

    /**
     * Use atp-macro parser. Needs datafix before usage.
     * */
    public List<AbstractTextParameter> parseUsingExternalParser(String textToParse, boolean setEvaluate) {
        try {
            MacrosLexer lexer = new MacrosLexer(CharStreams.fromString(textToParse));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            MacrosParser parser = new MacrosParser(tokens);
            MacrosParser.BodyContext body = parser.body();
            ParseTreeWalker walker = new ParseTreeWalker();
            MacroListener parameterListener = new MacroListener(context, parameterPositionContext, setEvaluate);
            walker.walk(parameterListener, body);
            return parameterListener.getParsedParameters();
        } catch (Exception e) {
            LOG.debug("Text parameter parse error '" + textToParse + "'", e);
            return Collections.singletonList(new TextParameter("Parse error", parameterPositionContext));
        }
    }
}
