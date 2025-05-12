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

package org.qubership.atp.dataset.db.migration.customchange.constant;

import java.util.regex.Pattern;

public class RegExpConstants {

    private static final String MACROS_MARKER_REGEXP = "[#|$]+";
    private static final String DATE_MACROS_NAME_REGEXP = "DATE";
    private static final String OPEN_REGEXP = "\\(";
    private static final String CLOSE_REGEXP = "\\)";
    private static final String PARAMETER_VALUE = "[a-zA-Z0-9\\s\r\t*-:~+.,#$_&'<>\\\\]*";
    private static final String PARAMETER_VALUE_REGEXP = JoinerSplitterConstants.EMPTY_STRING_JOINER.join(
            OPEN_REGEXP,
            PARAMETER_VALUE,
            CLOSE_REGEXP);
    private static final String ANY_SYMBOL_REGEXP = "(.*)";

    private static final String TZ_REGEXP = "(''T''|''Z''|\\''T''|^\\''T\\''|\\''Z''|^\\''Z\\'')";
    private static final String T_OR_Z_QUOTE = "[\\\\]*[']+[T|Z]+[\\\\]*[']+";

    public static final Pattern TZ_QUOTE_PATTERN = Pattern.compile(T_OR_Z_QUOTE);
    public static final Pattern DATE_MACROS_PATTERN = Pattern.compile(
            JoinerSplitterConstants.EMPTY_STRING_JOINER.join(MACROS_MARKER_REGEXP,
                    DATE_MACROS_NAME_REGEXP,
                    PARAMETER_VALUE_REGEXP));

    public static String MACROS_DATE_TX_REGEXP = JoinerSplitterConstants.EMPTY_STRING_JOINER
            .join(MACROS_MARKER_REGEXP,
                    DATE_MACROS_NAME_REGEXP,
                    OPEN_REGEXP,
                    ANY_SYMBOL_REGEXP,
                    TZ_REGEXP,
                    ANY_SYMBOL_REGEXP,
                    CLOSE_REGEXP
                    );
}
