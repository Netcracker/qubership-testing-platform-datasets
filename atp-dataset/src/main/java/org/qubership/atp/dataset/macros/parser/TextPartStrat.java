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

class TextPartStrat extends Strategy {

    private final OpenBrackets brackets;
    private final StringBuilder buffer = new StringBuilder();
    private final ParsingState parsingState;

    TextPartStrat(ParsingState parsingState, OpenBrackets brackets) {
        super(TokenType.TEXT_PART);
        this.parsingState = parsingState;
        this.brackets = brackets;
    }

    @Override
    public Strategy read(char chr) {
        if (chr == ')') {
            if (brackets.getOpen() == 0) {
                TerminalStrat formulaEnded = parsingState.formulaEnd.of(buffer);
                parsingState.pushToken(formulaEnded);
                brackets.goUpper();
                return formulaEnded;
            } else {
                brackets.decOpen();
            }
        }
        if (chr == '#') {
            return parsingState.probablyFormula.withPrefix(this);
        }
        if (chr == '(') {
            brackets.incOpen();
        }
        buffer.append(chr);
        return this;
    }

    @Override
    public String getValue() {
        return buffer.toString();
    }

    @Override
    protected void clearState() {
        super.clearState();
        buffer.setLength(0);
    }

    TextPartStrat of() {
        clearState();
        return this;
    }

    TextPartStrat of(CharSequence chr) {
        TextPartStrat result = of();
        result.append(chr);
        return this;
    }

    TextPartStrat append(CharSequence postfix) {
        buffer.append(postfix);
        return this;
    }
}
