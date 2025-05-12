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

package org.qubership.atp.dataset.db;

import org.springframework.beans.factory.annotation.Value;

import lombok.Getter;

public class DbConfigImpl implements DBConfig {

    @Getter
    @Value("${jdbc.Url}")
    private String jdbcUrl;
    @Getter
    @Value("${jdbc.User}")
    private String username;
    @Getter
    @Value("${jdbc.Password}")
    private String password;
    @Getter
    @Value("${jdbc.Dialect:com.querydsl.sql.PostgreSQLTemplates}")
    private String dialect;
    @Getter
    @Value("${jdbc.Driver:org.postgresql.Driver}")
    private String driverClassName;
    @Getter
    @Value("${jdbc.MinIdle:10}")
    private int minIdle;
    @Getter
    @Value("${jdbc.MaxPoolSize:50}")
    private int maxPoolSize;

    @Value("${jdbc.Debug:false}")
    private boolean jdbcDebug;

    @Getter
    @Value("${jdbc.leak.detection.threshold}")
    private long leakDetectionThreshold;

    @Override
    public boolean isDebug() {
        return jdbcDebug;
    }
}
