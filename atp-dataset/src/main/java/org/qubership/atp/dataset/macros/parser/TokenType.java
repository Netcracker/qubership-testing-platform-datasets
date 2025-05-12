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

import java.util.function.Function;

public enum TokenType {
    PROBABLY_FORMULA(str -> str, 0),
    TEXT_PART(str -> str, 0),
    FORMULA_START(str -> "#" + str + "(", 2),
    FORMULA_END(str -> str + ")", 1);

    public final Function<String, String> fromValue;
    public final int extraSymbolsCount;

    TokenType(Function<String, String> fromValue, int extraSymbolsCount) {
        this.fromValue = fromValue;
        this.extraSymbolsCount = extraSymbolsCount;
    }
}
