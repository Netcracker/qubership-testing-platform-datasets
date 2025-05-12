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

package org.qubership.atp.dataset.config.listeners;

import static java.util.Objects.nonNull;
import static org.springframework.jdbc.datasource.DataSourceUtils.isConnectionTransactional;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.querydsl.core.QueryException;
import com.querydsl.sql.AbstractSQLQuery;
import com.querydsl.sql.SQLBaseListener;
import com.querydsl.sql.SQLListenerContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConnectionClosedListener extends SQLBaseListener {

    private final DataSource dataSource;

    @Override
    public void end(SQLListenerContext context) {
        Connection connection = context.getConnection();
        if (nonNull(connection)
                && !isConnectionTransactional(connection, dataSource)
                && context.getData(AbstractSQLQuery.class.getName() + "#PARENT_CONTEXT") == null) {
            try {
                connection.close();
            } catch (SQLException exception) {
                throw new QueryException(exception);
            }
        }
    }
}
