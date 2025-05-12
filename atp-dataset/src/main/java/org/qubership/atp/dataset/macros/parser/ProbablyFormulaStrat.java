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

package org.qubership.atp.dataset.macros.parser;

import org.qubership.atp.dataset.macros.MacroRegistry;

class ProbablyFormulaStrat extends Strategy {

    private final MacroRegistry macroFinder;
    private final OpenBrackets brackets;
    private final ParsingState parsingState;
    private final StringBuilder buffer = new StringBuilder();
    private TextPartStrat prefix = null;

    ProbablyFormulaStrat(ParsingState parsingState, MacroRegistry macroFinder, OpenBrackets brackets) {
        super(TokenType.PROBABLY_FORMULA);
        this.parsingState = parsingState;
        this.macroFinder = macroFinder;
        this.brackets = brackets;
    }

    @Override
    public Strategy read(char chr) {
        if (chr == '(' && macroFinder.fullyEquals(buffer.toString())) {
            if (prefix != null) {
                parsingState.pushToken(parsingState.textPart);
            }
            TerminalStrat formulaStarted = parsingState.formulaStart.of(buffer);
            parsingState.pushToken(formulaStarted);
            brackets.goDeeper();
            return formulaStarted;
        }
        if (macroFinder.partiallyEquals(buffer.toString() + chr)) {
            buffer.append(chr);
            return this;
        }
        //it's not a macro
        buffer.insert(0, '#');
        if (prefix != null) {
            return prefix.append(buffer).read(chr);
        } else {
            return parsingState.textPart.of(buffer).read(chr);
        }
    }

    @Override
    public String getValue() {
        String result = '#' + buffer.toString();
        if (prefix == null) {
            return result;
        }
        return prefix.getValue() + result;
    }

    @Override
    protected void clearState() {
        super.clearState();
        prefix = null;
        buffer.setLength(0);
    }

    ProbablyFormulaStrat noPrefix() {
        clearState();
        return this;
    }

    ProbablyFormulaStrat withPrefix(TextPartStrat prefix) {
        ProbablyFormulaStrat result = noPrefix();
        result.prefix = prefix;
        return result;
    }
}
