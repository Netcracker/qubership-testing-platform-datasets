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

package org.qubership.atp.dataset.service.direct;

import org.qubership.atp.dataset.service.direct.macros.DsEvaluator;
import org.qubership.atp.dataset.service.jpa.JpaDataSetService;

/**
 * Facade for evaluation operations.
 *
 * @deprecated use {@link JpaDataSetService} instead.
 */
@Deprecated
public interface EvaluationService {

    /**
     * Returns processor which evaluates macroses/unwraps aliases.
     *
     * @param evaluate    if true - macroses will be evaluated. Else - unwraps aliases only.
     * @param acceptFails if true and some macros failed to be evaluated, returns error as
     *                    evaluation result.
     * @throws IllegalArgumentException if acceptFails is false and some macros failed to be
     *                                  evaluated.
     * @deprecated only for cases when the evaluate parameter equals true. Use
     * {@link JpaDataSetService} instead. Otherwise you can use this method.
     */
    @Deprecated
    DsEvaluator getEvaluator(boolean evaluate, boolean acceptFails);
}
