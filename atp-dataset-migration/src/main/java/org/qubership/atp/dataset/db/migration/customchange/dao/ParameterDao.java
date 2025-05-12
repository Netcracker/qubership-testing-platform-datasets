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

package org.qubership.atp.dataset.db.migration.customchange.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.dataset.db.migration.customchange.constant.QueryConstants;
import org.qubership.atp.dataset.db.migration.customchange.model.DateMacros;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ParameterDao {

    private PreparedStatement preparedStatement;
    private Connection connection;
    private ResultSet resultSet;

    public ParameterDao(Connection connection) {
       this.connection = connection;
    }

    /**
     * Select all macros date from parameters with 'T'/'Z'.
     */
    public List<DateMacros> selectParameterMacrosDateExists() throws SQLException {
        List<DateMacros> idStringPair = new LinkedList();
        this.preparedStatement = connection.prepareStatement(QueryConstants.SELECT_PARAMETER_MACROS_DATE);
        this.resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            UUID id = (UUID) resultSet.getObject(QueryConstants.ID_FIELD_NAME_PARAMETER);
            String stringValue = (String) resultSet.getObject(QueryConstants.STRING_FIELD_NAME_PARAMETER);
            idStringPair.add(DateMacros.builder().id(id).macros(stringValue).build());
        }
        return idStringPair;
    }

    /**
     * Update parameter macros date to new value with escape quotes.
     */
    public void updateParameterMacrosDate(DateMacros newValue) throws SQLException {
        this.preparedStatement = connection.prepareStatement(QueryConstants.UPDATE_MACROS_DATE);
        preparedStatement.setString(1, newValue.getMacros());
        preparedStatement.setObject(2, newValue.getId());
        preparedStatement.executeUpdate();
    }

    /**
     * Close connection.
     */
    public void close() {
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (Exception exception) {
                log.error("Can not close resources", exception);
            }
        }
    }

}
