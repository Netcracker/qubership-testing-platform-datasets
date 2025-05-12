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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import lombok.SneakyThrows;

public class ListValueCustomChange implements CustomTaskChange {

    //    private JpaListValueRepository listValueRepository;
    private JdbcConnection connection;
    private final Map<UUID, Map<UUID, UUID>> listValues = new HashMap<>();

    @SneakyThrows
    @Override
    public void execute(Database database) {
        connection = (JdbcConnection) database.getConnection();

        ResultSet rs = getParametersId();
        processParameters(rs);

        ResultSet rsOverlap = getOverlapParametersId();
        processParameters(rsOverlap);

        connection.commit();
    }

    private ResultSet getParametersId() throws DatabaseException, SQLException {
        String nativeQuery = "select p.id, p.attribute_id, p.list, lv.text "
                + "from \"parameter\" p, \"list_values\" lv, \"attribute\" a "
                + "where p.attribute_id = a.id and p.list = lv.id and p.attribute_id != lv.attribute_id";

        PreparedStatement stmt = connection.prepareStatement(nativeQuery);
        return stmt.executeQuery();
    }

    private ResultSet getOverlapParametersId() throws DatabaseException, SQLException {
        String nativeQuery = "select p.id, ak.attribute_id, p.list, lv.text "
                + "from \"parameter\" p, \"list_values\" lv, \"attribute_key\" ak "
                + "where p.attribute_id = ak.id and p.list = lv.id and ak.attribute_id != lv.attribute_id";

        PreparedStatement stmt = connection.prepareStatement(nativeQuery);
        return stmt.executeQuery();
    }

    private void processParameters(ResultSet rs)
            throws SQLException, DatabaseException {

        int index = 0;
        while (rs.next()) {
            UUID parameterId = UUID.fromString(rs.getString(1));
            UUID attrId = UUID.fromString(rs.getString(2));
            UUID incorrectListValueId = UUID.fromString(rs.getString(3));
            String oldListValueText = rs.getString(4);

            listValues.putIfAbsent(attrId, new HashMap<>());

            if (listValues.get(attrId).containsKey(incorrectListValueId)) {
                updateParameter(listValues.get(attrId).get(incorrectListValueId), parameterId);
            } else {


                List<ListValueDto> newValues = getListValues(attrId);
                Optional<ListValueDto> optionalValue =
                        newValues.stream().filter(value -> value.getText().equals(oldListValueText)).findFirst();

                if (optionalValue.isPresent()) {
                    UUID correctListValueId = optionalValue.get().getId();
                    updateParameter(correctListValueId, parameterId);
                    listValues.get(attrId).put(incorrectListValueId, correctListValueId);
                } else {
                    UUID generatedListValueId = UUID.randomUUID();
                    insertListValue(generatedListValueId, attrId, oldListValueText);
                    updateParameter(generatedListValueId, parameterId);
                    listValues.get(attrId).put(incorrectListValueId, generatedListValueId);
                }
            }
            if (index == 1000) {
                connection.commit();
                index = 0;
            } else {
                index++;
            }
        }
    }

    private List<ListValueDto> getListValues(UUID attributeId) throws DatabaseException, SQLException {
        List<ListValueDto> listValues = new ArrayList<>();
        PreparedStatement stmt2 = connection.prepareStatement("select lv.id, lv.attribute_id, lv.text "
                        + "from list_values lv "
                        + "where attribute_id = ?::uuid");
        stmt2.setString(1, attributeId.toString());
        ResultSet rs = stmt2.executeQuery();
        while (rs.next()) {
            UUID id = UUID.fromString(rs.getString(1));
            UUID attrId = UUID.fromString(rs.getString(2));
            String text = rs.getString(3);
            listValues.add(new ListValueDto(id, attrId, text));
        }

        return listValues;
    }

    private void updateParameter(UUID listValueId, UUID parameterId)
            throws DatabaseException, SQLException {
        String updateParameter = String.format("UPDATE public.\"parameter\"\n"
                + "SET   list='%s'::uuid\n"
                + "WHERE id='%s'::uuid", listValueId, parameterId);
        PreparedStatement stmt2 = connection.prepareStatement(updateParameter);
        stmt2.execute();
    }

    private void insertListValue(UUID listValueId, UUID attrId, String text)
            throws DatabaseException, SQLException {
        String insertListValue = String.format("INSERT INTO public.list_values"
                        + "(id, attribute_id, \"text\") "
                        + "VALUES ('%s', '%s', '%s');",
                listValueId, attrId, text.replace("'", "''"));
        PreparedStatement stmt3 = connection.prepareStatement(insertListValue);
        stmt3.execute();
    }

    @Override
    public String getConfirmationMessage() {
        return null;
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

    private class ListValueDto {

        private UUID id;
        private UUID attrId;
        private String text;

        private ListValueDto(UUID id, UUID attrId, String text) {
            this.id = id;
            this.attrId = attrId;
            this.text = text;
        }

        private UUID getId() {
            return this.id;
        }

        private String getText() {
            return this.text;
        }
    }
}
