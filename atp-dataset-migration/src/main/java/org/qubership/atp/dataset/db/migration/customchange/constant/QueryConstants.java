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

public class QueryConstants {

    public static final String SELECT_PARAMETER_MACROS_DATE = JoinerSplitterConstants.EMPTY_STRING_JOINER.join(
            "select id, string from parameter where string ~ '",
            RegExpConstants.MACROS_DATE_TX_REGEXP,
            "'");

    public static final String UPDATE_MACROS_DATE = "UPDATE parameter SET string = ? WHERE id = ?";

    public static final String ID_FIELD_NAME_PARAMETER = "id";

    public static final String STRING_FIELD_NAME_PARAMETER = "string";
}
