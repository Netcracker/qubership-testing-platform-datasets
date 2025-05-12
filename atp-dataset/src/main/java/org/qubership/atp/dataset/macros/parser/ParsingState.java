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

public abstract class ParsingState {

    protected final ProbablyFormulaStrat probablyFormula;
    protected final OpenBrackets brackets = new OpenBrackets();
    protected final TerminalStrat formulaStart = new TerminalStrat(this, TokenType.FORMULA_START);
    protected final TerminalStrat formulaEnd = new TerminalStrat(this, TokenType.FORMULA_END);
    protected final TextPartStrat textPart = new TextPartStrat(this, brackets);
    protected Strategy currentStrategy = formulaEnd;

    public ParsingState(MacroRegistry macroFinder) {
        this.probablyFormula = new ProbablyFormulaStrat(this, macroFinder, brackets);
    }

    protected void read(char chr) {
        currentStrategy = currentStrategy.read(chr);
    }

    protected void pushToken(Strategy strategy) {
        strategy.consumed = true;
    }

    protected void reset() {
        formulaStart.clearState();
        formulaEnd.clearState();
        textPart.clearState();
        brackets.clearState();
        currentStrategy = formulaEnd;
    }
}
