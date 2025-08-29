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

package org.qubership.atp.dataset.service.rest.server.v2;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.qubership.atp.dataset.RegexpMatcher.matchesToRegExp;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.model.api.ParameterRequest;
import org.qubership.atp.dataset.service.jpa.service.AbstractJpaTest;
import org.springframework.aop.AopInvocationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@Isolated
@SpringBootTest
@ContextConfiguration(classes = {TestConfiguration.class})
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"atp-dataset.javers.enabled=false"})
class ParameterControllerV2Test extends AbstractJpaTest {

    @Autowired
    private MockMvc mockMvc;

    ObjectMapper mapper = new ObjectMapper();

    @Test
    @Sql(scripts = "classpath:test_data/sql/parameter_controller_v2/parameterControllerV2.sql")
    void update_existingVisibilityArea_successUpdating() throws Exception {
        UUID dataSet = UUID.fromString("c360900b-d558-4907-b4c4-d596df40d07c");
        UUID attribute = UUID.fromString("2782bcd4-76e1-4093-88db-088e5d4eb829");

        ParameterRequest parameterRequest = new ParameterRequest();
        parameterRequest.setDataSetId(dataSet);
        parameterRequest.setValue("New text value");

        String body = mapper.writeValueAsString(parameterRequest);

        MvcResult response = mockMvc.perform(post("/v2/parameter/ds/{dataSetId}/attribute/{attributeId}",
                        dataSet, attribute)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        String expected = "{\"id\":\"d0c2a5ce-2864-4f17-a3eb-d696f3b008ea\","
                + "\"dataSet\":\"c360900b-d558-4907-b4c4-d596df40d07c\",\"dataSetReference\":null,"
                + "\"listValue\":null,\"fileData\":null,\"attribute\":\"2782bcd4-76e1-4093-88db-088e5d4eb829\","
                + "\"text\":\"New text value\"}";

        Assertions.assertEquals(expected, response.getResponse().getContentAsString());
    }

    @Test
    void update_notExistingVisibilityArea_internalServerError() throws Exception {
        UUID dataSet = UUID.randomUUID();
        UUID attribute = UUID.randomUUID();

        ParameterRequest parameterRequest = new ParameterRequest();
        parameterRequest.setDataSetId(dataSet);
        parameterRequest.setValue("New text value");
        String body = mapper.writeValueAsString(parameterRequest);

        try {
            MvcResult response = mockMvc.perform(post("/v2/parameter/ds/{dataSetId}/attribute/{attributeId}",
                            dataSet, attribute)
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isInternalServerError())
                    .andReturn();
        } catch (Throwable e) {
            /* Expected exception is (in case stubbed atp-auth-library):
                org.springframework.web.util.NestedServletException: Request processing failed;
                nested exception is java.lang.IllegalArgumentException:
                Can not change parameter with attribute id: aa105208-7fc7-493d-bb58-c892628ef0f7
                because dataset id locked: 27b1617e-56e9-4670-894e-767bc10e26fd
                at org.qubership.atp.dataset.service.rest.server.v2.ParameterControllerV2Test
                .update_notExistingVisibilityArea_internalServerError(ParameterControllerV2Test.java:98)
             */
            if (e.getCause() != null) {
                assertThat(e.getCause(), instanceOf(IllegalArgumentException.class));
                String causedByMessage = e.getCause().toString();
                assertThat(causedByMessage, matchesToRegExp(".*Can not change parameter with attribute id: .* " +
                        "because dataset id locked.*"));
            }
        }
    }

    @Test
    @Sql(scripts = "classpath:test_data/sql/parameter_controller_v2/parameterControllerV2.sql")
    void create_existingVisibilityArea_successCreating() throws Exception {
        UUID dataSet = UUID.fromString("c360900b-d558-4907-b4c4-d596df40d07c");
        UUID attribute = UUID.fromString("2782bcd4-76e1-4093-88db-088e5d4eb829");

        parameterService.remove(UUID.fromString("d0c2a5ce-2864-4f17-a3eb-d696f3b008ea"));
        ParameterRequest parameterRequest = new ParameterRequest();
        parameterRequest.setDataSetId(dataSet);
        parameterRequest.setValue("value");
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(parameterRequest);

        mockMvc.perform(put("/v2/parameter/ds/{dataSetId}/attribute/{attributeId}", dataSet, attribute)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
    }

    @Test
    void create_notExistingVisibilityArea_internalServerError() throws Exception {
        UUID dataSet = UUID.randomUUID();
        UUID attribute = UUID.randomUUID();
        ParameterRequest parameterRequest = new ParameterRequest();
        parameterRequest.setDataSetId(dataSet);
        parameterRequest.setValue("value");
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(parameterRequest);

        try {
            MvcResult response = mockMvc.perform(put("/v2/parameter/ds/{dataSetId}/attribute/{attributeId}", dataSet, attribute)
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isInternalServerError())
                    .andReturn();
        } catch (Throwable e) {
            /* Expected exception is (in case stubbed atp-auth-library):
                org.springframework.web.util.NestedServletException: Request processing failed;
                nested exception is org.springframework.aop.AopInvocationException:
                Null return value from advice does not match primitive return type for:
                public abstract boolean org.qubership.atp.dataset.db.jpa.repositories.JpaDataSetRepository
                .isLocked(java.util.UUID)
                at org.qubership.atp.dataset.service.rest.server.v2.ParameterControllerV2Test
                .create_notExistingVisibilityArea_internalServerError(ParameterControllerV2Test.java:144)
            */
            if (e.getCause() != null) {
                assertThat(e.getCause(), instanceOf(AopInvocationException.class));
                String causedByMessage = e.getCause().toString();
                assertThat(causedByMessage, StringContains.containsString("Null return value from advice " +
                        "does not match primitive return type for"));
            }
        }
    }

    @Test
    @Transactional
    @Sql(scripts = "classpath:test_data/sql/parameter_controller_v2/parameterControllerV2.sql")
    void bulkUpdateAttribute_existingVisibilityArea_successCreating() throws Exception {
        UUID param1 = UUID.fromString("5fe9e373-d709-4d96-b07d-c5a1d0b78ed3");
        UUID param2 = UUID.fromString("36b8f581-c378-47d7-adec-3ff80e9d8999");
        List<UUID> params = new ArrayList<>();
        params.add(param1);
        params.add(param2);

        ParameterRequest parameterRequest = new ParameterRequest();
        parameterRequest.setValue("new value");
        parameterRequest.setDataSetListId(UUID.fromString("ea7c10e7-7ab4-437e-b7f8-3961d3bf299d"));
        parameterRequest.setListIdsParametersToChange(params);
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(parameterRequest);

        mockMvc.perform(post("/v2/parameter/update/bulk")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void bulkUpdateAttribute_notExistingVisibilityArea_InternalServerError() throws Exception {
        UUID param1 = UUID.randomUUID();
        UUID param2 = UUID.randomUUID();
        List<UUID> params = new ArrayList<>();
        params.add(param1);
        params.add(param2);

        ParameterRequest parameterRequest = new ParameterRequest();
        parameterRequest.setValue("new value");
        parameterRequest.setDataSetListId(UUID.fromString("646b1bbb-e392-4364-910d-e54e8609ea8a"));
        parameterRequest.setListIdsParametersToChange(params);
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(parameterRequest);

        try {
            MvcResult response = mockMvc.perform(post("/v2/parameter/update/bulk")
                            .contentType(APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isInternalServerError())
                    .andReturn();
        } catch (Throwable e) {
            /* Expected exception is (in case stubbed atp-auth-library):
                org.springframework.web.util.NestedServletException: Request processing failed;
                nested exception is java.lang.NullPointerException: Cannot invoke
                "org.qubership.atp.dataset.service.jpa.delegates.Parameter.getDataSet()" because the return
                value of "org.qubership.atp.dataset.db.jpa.ModelsProvider.getParameterById(java.util.UUID)" is null
                at org.qubership.atp.dataset.service.rest.server.v2.ParameterControllerV2Test
                .bulkUpdateAttribute_notExistingVisibilityArea_InternalServerError(ParameterControllerV2Test.java:199)
             */
            /*
            System.out.println("Exception: " + e
                    + "\nMessage: " + e.getMessage()
                    + "\nCaused by: " + e.getCause());
             */
            if (e.getCause() != null) {
                assertThat(e.getCause(), instanceOf(NullPointerException.class));
                String causedByMessage = e.getCause().toString();
                assertThat(causedByMessage, matchesToRegExp(".+ because the return value of \".+\" is null"));
            }
        }
    }
}
