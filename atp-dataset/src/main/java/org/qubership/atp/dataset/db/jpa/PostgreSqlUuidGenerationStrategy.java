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

package org.qubership.atp.dataset.db.jpa;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.UUIDGenerationStrategy;

public class PostgreSqlUuidGenerationStrategy implements UUIDGenerationStrategy {

    @Override
    public int getGeneratedVersion() {
        return 4;
    }

    @Override
    public UUID generateUUID(
            SharedSessionContractImplementor session) {
        return ((Session) session).doReturningWork(connection -> {
            try (
                    Statement statement = connection.createStatement();
                    ResultSet resultSet = statement.executeQuery("select uuid_generate_v4()")
            ) {
                if (resultSet.next()) {
                    return (UUID) resultSet.getObject(1);
                }
            }
            throw new IllegalArgumentException("Can't fetch a new UUID");
        });
    }
}
