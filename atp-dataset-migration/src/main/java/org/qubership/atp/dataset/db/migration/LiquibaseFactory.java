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

package org.qubership.atp.dataset.db.migration;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;

public class LiquibaseFactory {

    private final DataSource dataSource;

    @Autowired
    public LiquibaseFactory(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Creates liquibase facade for target changelog file.
     *
     * @param pathToChangeLog like 'install.xml'
     * @return Liquibase facade
     * @throws SQLException       if connection can not be acquired
     * @throws LiquibaseException if db implementation not found
     */
    public Liquibase create(String pathToChangeLog) throws SQLException, LiquibaseException {
        return create(pathToChangeLog, new ClassLoaderResourceAccessor());
    }

    /**
     * Creates liquibase facade for target changelog file, with custom resource accessor
     * (ClassLoader).
     *
     * @param pathToChangeLog  path to xml file with changelogs
     * @param resourceAccessor custom class loader
     */
    public Liquibase create(String pathToChangeLog, ResourceAccessor resourceAccessor)
            throws SQLException, LiquibaseException {
        JdbcConnection connection = new JdbcConnection(dataSource.getConnection());
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connection);
        return new Liquibase(pathToChangeLog, resourceAccessor, database);
    }
}
