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

package org.qubership.atp.dataset.db.migration.customchange.task;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.qubership.atp.dataset.db.migration.customchange.dao.ParameterDao;
import org.qubership.atp.dataset.db.migration.customchange.db.ConnectionReceiving;
import org.qubership.atp.dataset.db.migration.customchange.executor.DateMacrosEscapeQuoteExecutor;
import org.qubership.atp.dataset.db.migration.customchange.executor.Executor;
import org.qubership.atp.dataset.db.migration.customchange.model.DateMacros;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EscapeQuoteDateMacrosTask implements CustomTaskChange {

    private Executor dateMacrosEscapeQuoteExecutor; 
    private ParameterDao macrosDateDao;
    private Connection connection;

    private void init(Database database) {
        this.dateMacrosEscapeQuoteExecutor = DateMacrosEscapeQuoteExecutor.builder().build();
        this.connection = ConnectionReceiving.getConnection(database);
        this.macrosDateDao = new ParameterDao(this.connection);
    }

    @Override
    public void execute(Database database) throws CustomChangeException {
        init(database);

        try {
            List<DateMacros> dateMacrosParameters = macrosDateDao.selectParameterMacrosDateExists();
            dateMacrosParameters = escapeQuoteDateMacros(dateMacrosParameters);

            dateMacrosParameters.forEach(dateMacros -> {
                try {
                    macrosDateDao.updateParameterMacrosDate(dateMacros);
                } catch (SQLException e) {
                    log.error("Error query execution", e);
                }
            });
        } catch (Throwable throwables) {
            throw new CustomChangeException("Can not migrate attributes", throwables);
        } finally {
           macrosDateDao.close();
        }
    }

    private List<DateMacros> escapeQuoteDateMacros(List<DateMacros> dateMacrosParameters) {
        dateMacrosParameters
                .forEach(dateMacrosParameter ->
                      dateMacrosParameter.setMacros((String)dateMacrosEscapeQuoteExecutor
                              .execute(dateMacrosParameter.getMacros())));
        return dateMacrosParameters;
    }

    @Override
    public String getConfirmationMessage() {
        return "Passed";
    }

    @Override
    public void setUp() throws SetupException {
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
    }

    @Override
    public ValidationErrors validate(Database database) {
        return null;
    }
}
