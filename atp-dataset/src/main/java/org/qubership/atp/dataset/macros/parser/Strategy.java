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

public abstract class Strategy implements Token {

    private final TokenType type;
    protected boolean consumed;

    Strategy(TokenType type) {
        this.type = type;
    }

    protected abstract Strategy read(char chr);

    protected void clearState() {
        consumed = false;
    }

    @Override
    public TokenType getKey() {
        return type;
    }

    @Override
    public String setValue(String value) {
        throw new UnsupportedOperationException("Designed to be immutable: " + this);
    }

    @Override
    public String toString() {
        return String.format("%s   %s", String.valueOf(getKey()), String.valueOf(getValue()));
    }
}
