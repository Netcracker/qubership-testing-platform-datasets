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

import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.MixInId;
import org.qubership.atp.dataset.service.rest.QueryParamFlag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

@Configuration
public class ConverterConfig {

    /**
     * Converter for creating MixInId from string.
     */
    @Bean
    public Converter<String, MixInId> mixInIdConverter() {
        return new Converter<String, MixInId>() {
            @Override
            public MixInId convert(String s) {
                return MixInId.fromString(s);
            }
        };
    }

    /**
     * Converter for creating QueryParamFlag from string.
     */
    @Bean
    public Converter<String, QueryParamFlag> queryParamFlagConverter() {
        return new Converter<String, QueryParamFlag>() {
            @Override
            public QueryParamFlag convert(String s) {
                return new QueryParamFlag(s);
            }
        };
    }

    /**
     * Converter for creating AttributeType from string.
     */
    @Bean
    public Converter<String, AttributeType> attributeTypeConverter() {
        return new Converter<String, AttributeType>() {
            @Override
            public AttributeType convert(String id) {
                return AttributeType.from(Short.parseShort(id));
            }
        };
    }
}
