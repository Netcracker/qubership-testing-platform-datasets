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

package org.qubership.atp.dataset.macros.processor;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;

import org.qubership.atp.dataset.macros.EvalContextImpl;
import org.qubership.atp.dataset.macros.MacroRegistry;
import org.qubership.atp.dataset.macros.cache.NoCache;
import org.qubership.atp.dataset.macros.exception.EvalException;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.service.direct.AliasWrapperService;

public class MacrosEvaluator implements Evaluator {

    private final MacroRegistry registry;
    private final AliasWrapperService wrapperService;

    @Autowired
    public MacrosEvaluator(MacroRegistry registry, AliasWrapperService wrapperService) {
        this.registry = registry;
        this.wrapperService = wrapperService;
    }

    @Override
    public String evaluate(String inputText, DataSetList dataSetList, DataSet dataSet)
            throws EvalException {
        if (inputText.isEmpty()) {
            return inputText;
        }
        return new EvalContextImpl(null, registry, wrapperService, NoCache.INSTANCE,
                dataSetList, dataSet, true, Collections.emptyList(), null)
                .evaluate(inputText);
    }
}
