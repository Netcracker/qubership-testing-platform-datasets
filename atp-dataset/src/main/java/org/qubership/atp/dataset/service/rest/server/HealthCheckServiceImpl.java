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

package org.qubership.atp.dataset.service.rest.server;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.dataset.db.jdbc.JdbcTemplates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HealthCheckServiceImpl implements HealthCheckService {
    private static final String GET_VISIBILITY_AREAS_IDS = "select id from visibility_area";
    @Autowired
    JdbcTemplates jdbc;

    @Override
    public List<UUID> getCheckVisibilityAreasIdsList() {
        return jdbc.executeSelect(
                GET_VISIBILITY_AREAS_IDS,
                resultSet -> {
                    List<UUID> ids = new LinkedList<>();
                    while (resultSet.next()) {
                        UUID visibilityAreaId = (UUID) resultSet.getObject("id");
                        ids.add(visibilityAreaId);
                    }
                    return ids;
                }
        );
    }
}
