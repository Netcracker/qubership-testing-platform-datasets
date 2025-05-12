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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import org.qubership.atp.dataset.macros.MacroRegistry;
import org.qubership.atp.dataset.macros.processor.Evaluator;
import org.qubership.atp.dataset.macros.processor.MacrosEvaluator;
import org.qubership.atp.dataset.service.direct.AliasWrapperService;


@Configuration
@Import({TestConfiguration.class})
public class TestMacrosConfiguration {

    @Bean
    public Evaluator evaluator(MacroRegistry macroRegistry, AliasWrapperService aliasWrapperService) {
        return new MacrosEvaluator(macroRegistry, aliasWrapperService);
    }
}
