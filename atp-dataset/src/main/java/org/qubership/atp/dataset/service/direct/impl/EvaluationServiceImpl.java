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

package org.qubership.atp.dataset.service.direct.impl;

import org.qubership.atp.dataset.db.DataSetListRepository;
import org.qubership.atp.dataset.db.DataSetRepository;
import org.qubership.atp.dataset.macros.MacroRegistry;
import org.qubership.atp.dataset.macros.cache.SimpleCache;
import org.qubership.atp.dataset.service.direct.AliasWrapperService;
import org.qubership.atp.dataset.service.direct.EvaluationService;
import org.qubership.atp.dataset.service.direct.macros.CachingParamEvaluator;
import org.qubership.atp.dataset.service.direct.macros.DsEvaluator;
import org.qubership.atp.dataset.service.direct.macros.EvaluateDsParamStrategy;
import org.qubership.atp.dataset.service.direct.macros.EvaluateDsParamStrategyImpl;
import org.qubership.atp.dataset.service.direct.macros.UnwrapAlias;
import org.qubership.atp.dataset.service.direct.macros.schange.EvaluateDsStructureStrategy;
import org.qubership.atp.dataset.service.direct.macros.schange.EvaluateDsStructureStrategyImpl;
import org.qubership.atp.dataset.service.direct.macros.schange.NoStructureChange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EvaluationServiceImpl implements EvaluationService {

    private DataSetListRepository dslRepo;
    private DataSetRepository dsRepo;
    private AliasWrapperService aliasWrapperService;
    private MacroRegistry registry;

    @Autowired
    public void setDslRepo(DataSetListRepository dslRepo) {
        this.dslRepo = dslRepo;
    }

    @Autowired
    public void setDsRepo(DataSetRepository dsRepo) {
        this.dsRepo = dsRepo;
    }

    @Autowired
    public void setAliasWrapperService(AliasWrapperService aliasWrapperService) {
        this.aliasWrapperService = aliasWrapperService;
    }

    @Autowired
    public void setRegistry(MacroRegistry registry) {
        this.registry = registry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DsEvaluator getEvaluator(boolean evaluate, boolean acceptFails) {
        CachingParamEvaluator result;
        if (evaluate) {
            EvaluateDsParamStrategy dsParamStrategy = new EvaluateDsParamStrategyImpl(registry,
                    aliasWrapperService, new SimpleCache(), acceptFails);
            EvaluateDsStructureStrategy dsStructureStrategy = new EvaluateDsStructureStrategyImpl(dslRepo, dsRepo,
                    aliasWrapperService, registry);
            result = new CachingParamEvaluator(dsParamStrategy, dsStructureStrategy);
        } else {
            EvaluateDsParamStrategy dsParamStrategy = new UnwrapAlias(aliasWrapperService);
            EvaluateDsStructureStrategy dsStructureStrategy = new NoStructureChange(dsRepo);
            result = new CachingParamEvaluator(dsParamStrategy, dsStructureStrategy);
        }
        return result;
    }
}
