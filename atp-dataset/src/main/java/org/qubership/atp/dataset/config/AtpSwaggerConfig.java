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

package org.qubership.atp.dataset.config;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.SpringDocUtils;
import org.springdoc.core.converters.models.Pageable;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
public class AtpSwaggerConfig {

    /**
     * openApi definition.
     *
     * @return {@link OpenAPI} object
     */
    @Bean
    public OpenAPI openApiDefinition() {
        SpringDocUtils.getConfig()
                .replaceParameterObjectWithClass(org.springframework.data.domain.Pageable.class, Pageable.class)
                .replaceParameterObjectWithClass(org.springframework.data.domain.PageRequest.class, Pageable.class)
                .removeRequestWrapperToIgnore(Map.class);

        return new OpenAPI().addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createApiKeyScheme()))
                .info(new Info()
                        .title("ATP-Datasets")
                        .license(new License().name("(C) Copyright Qubership Technology")));
    }

    /**
     * Bean global operation customizer for OpenApi.
     */
    @Bean
    public OpenApiCustomiser globalOperationOpenApiCustomiser() {
        return openAPI -> openAPI.getPaths().values()
                .forEach(pathItem -> pathItem.readOperations()
                        .forEach(operation -> {
            if (StringUtils.isEmpty(operation.getSummary())) {
                // Swagger use method name as operationId, it should be unique
                // But method names could be same
                // if the swagger finds the same name,
                // swagger adds an index to it -> methodName, methodName_1, methodName_2...
                // Well trim index suffix and use method name as summary of current operation
                operation.setSummary(operation.getOperationId().split("_")[0]);
            }
        }));
    }

    /**
     * Bean for sorting schemas.
     */
    @Bean
    public OpenApiCustomiser sortSchemasAlphabetically() {
        return openApi -> {
            Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            openApi.getComponents().setSchemas(new TreeMap<>(schemas));
        };
    }

    /**
     * Add ability to specify jwt token in UI for authentication.
     */
    private SecurityScheme createApiKeyScheme() {
        return new SecurityScheme().type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer");
    }

}
