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

package org.qubership.atp.dataset.service.rest.dto.versioning;

public enum ChangeSummary {

    ADDED("Added"),
    DELETED("Deleted"),
    CHANGED("Changed"),
    DSL_ADDED("DSL Added"),
    DSL_CHANGED("DSL Changed"),
    OVERRIDE_CREATE("Override Created"),
    OVERRIDE_CHANGED("Override Changed"),
    OVERRIDE_DELETED("Override Removed"),
    RESTORED("Restored to v. %s");

    private String value;

    ChangeSummary(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public String toString(Object... extraParameters) {
        return String.format(value, extraParameters);
    }

    /**
     * Returns instance of String value.
     */
    public static ChangeSummary fromValue(String value) {
        for (ChangeSummary b : ChangeSummary.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
