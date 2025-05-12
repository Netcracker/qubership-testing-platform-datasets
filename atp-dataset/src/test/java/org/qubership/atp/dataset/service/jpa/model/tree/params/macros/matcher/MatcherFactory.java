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

package org.qubership.atp.dataset.service.jpa.model.tree.params.macros.matcher;

import org.hamcrest.Matcher;

public class MatcherFactory {

    // Example: 0c217a69-9658-41ef-9ac1-abe418fd4eeb
    public static Matcher<String> isUuid() {
        return new RegexMatcher("[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}");
    }

    // Example: CADF19C2-86DB-42D3-A4D9-1F42E0656530
    public static Matcher<String> isUuidUpperCase() {
        return new RegexMatcher("[0-9A-F]{8}(?:-[0-9A-F]{4}){3}-[0-9A-F]{12}");
    }

    // Example: 7853753120
    public static Matcher<String> isInn() {
        return new RegexMatcher("[0-9]{10}");
    }

    // Example: 2020-12-04T14:07:56.873+0200
    public static Matcher<String> isIsoDate() {
        return new RegexMatcher("[0-9]{4}(-[0-9]{2}){2}T([0-9]{2}:){2}[0-9]{2}.[0-9]{3}.[0-9]{4}");
    }

    // Example: 2020-12-06T08:00:05+0100
    public static Matcher<String> isIsoDateWithoutMs() {
        return new RegexMatcher("[0-9]{4}(-[0-9]{2}){2}T([0-9]{2}:){2}[0-9]{2}.[0-9]{4}");
    }

    // Example: "2020-12-06T08:00:05+01:00"
    public static Matcher<String> isIsoDateWithoutMsAndWithTimezoneInQuotes() {
        return new RegexMatcher("\"[0-9]{4}(-[0-9]{2}){2}T([0-9]{2}:){2}[0-9]{2}.[0-9]{2}.[0-9]{2}\"");
    }

    // Example: 2020-05-20T14:06:28.028Z
    public static Matcher<String> isIsoDateWithTimezoneCharacter() {
        return new RegexMatcher("[0-9]{4}(-[0-9]{2}){2}T([0-9]{2}:){2}[0-9]{2}.[0-9]{3}Z");
    }

    // Example: "2020-05-20T14:06:28.028Z"
    public static Matcher<String> isIsoDateWithTimezoneCharacterInQuotes() {
        return new RegexMatcher("\"[0-9]{4}(-[0-9]{2}){2}T([0-9]{2}:){2}[0-9]{2}.[0-9]{3}Z\"");
    }

    // Example: "2020-12-04T23:59:59"
    public static Matcher<String> isIsoDateWithoutMsAndWithoutTimezoneInQuotes() {
        return new RegexMatcher("\"[0-9]{4}(-[0-9]{2}){2}T([0-9]{2}:){2}[0-9]{2}\"");
    }

    // Example: 2020-12-04T14:27:33Z
    public static Matcher<String> isIsoDateWithoutMsAndWithTimezoneCharacter() {
        return new RegexMatcher("[0-9]{4}(-[0-9]{2}){2}T([0-9]{2}:){2}[0-9]{2}Z");
    }

    // Example: "2020-12-04T14:27:33Z"
    public static Matcher<String> isIsoDateWithoutMsAndWithTimezoneCharacterInQuotes() {
        return new RegexMatcher("\"[0-9]{4}(-[0-9]{2}){2}T([0-9]{2}:){2}[0-9]{2}Z\"");
    }

    // Example: "2020-12-14T"
    public static Matcher<String> isIsoDateWithoutTimeInQuotes() {
        return new RegexMatcher("\"[0-9]{4}(-[0-9]{2}){2}T\"");
    }

    // Example: 01.01.2021
    public static Matcher<String> isDate() {
        return new RegexMatcher("([0-9]{2}.){2}[0-9]{4}");
    }

    // Example: frowvtswvlelibsqlmdn
    public static Matcher<String> isChars(int count) {
        return new RegexMatcher(String.format("[a-z]{%d}", count));
    }

    // Example: LENVTCOJPDZZBRCNNJWK
    public static Matcher<String> isCharsUpperCase(int count) {
        return new RegexMatcher(String.format("[A-Z]{%d}", count));
    }

    // Example: NaN
    public static Matcher<String> isNaN() {
        return new RegexMatcher("NaN");
    }
}
