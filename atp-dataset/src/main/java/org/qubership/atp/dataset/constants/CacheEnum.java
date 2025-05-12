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

package org.qubership.atp.dataset.constants;

import lombok.Getter;

public enum CacheEnum {
    PROJECT_CACHE(Constants.PROJECT_CACHE, 600),
    DATASET_LIST_CONTEXT_CACHE(Constants.DATASET_LIST_CONTEXT_CACHE, 600),
    JAVERS_DIFF_CACHE(Constants.JAVERS_DIFF_CACHE, 600),
    PARAMETER_CACHE(Constants.PARAMETER_CACHE, 600);

    @Getter
    private final String key;
    @Getter
    private final int timeToLiveSec;


    CacheEnum(String key, int timeToLiveSec) {
        this.key = key;
        this.timeToLiveSec = timeToLiveSec;
    }

public static class Constants {
    public static final String PROJECT_CACHE = "projects";
    public static final String DATASET_LIST_CONTEXT_CACHE = "ATP_DATASETS_DATASET_LIST_CONTEXT_CACHE";

    public static final String JAVERS_DIFF_CACHE = "JAVERS_DIFF_CACHE";
    public static final String PARAMETER_CACHE = "ATP_DATASETS_PARAMETER_CACHE";
}
}
