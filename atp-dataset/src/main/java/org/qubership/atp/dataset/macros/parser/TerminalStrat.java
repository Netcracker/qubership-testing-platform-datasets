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

class TerminalStrat extends Strategy {

    private final StringBuilder buffer = new StringBuilder();
    private final ParsingState parsingState;

    TerminalStrat(ParsingState parsingState, TokenType type) {
        super(type);
        this.parsingState = parsingState;
    }

    @Override
    public Strategy read(char chr) {
        if (chr == '#') {
            return parsingState.probablyFormula.noPrefix();
        } else {
            return parsingState.textPart.of().read(chr);
        }
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

    public TerminalStrat of(CharSequence text) {
        clearState();
        buffer.append(text);
        return this;
    }
}
