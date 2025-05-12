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

package org.qubership.atp.dataset.service.direct.macros.schange;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.db.DataSetListRepository;
import org.qubership.atp.dataset.db.DataSetRepository;
import org.qubership.atp.dataset.macros.MacroRegistry;
import org.qubership.atp.dataset.macros.cache.NoCache;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.MixInId;
import org.qubership.atp.dataset.service.direct.AliasWrapperService;
import org.qubership.atp.dataset.service.direct.macros.EvaluateDsParamStrategyImpl;

import com.google.common.base.Preconditions;

public class EvaluateDsStructureStrategyImpl implements EvaluateDsStructureStrategy {

    private final DataSetListRepository dslRepo;
    private final DataSetRepository dsRepo;
    private final AliasWrapperService aliasWrapperService;
    private final MacroRegistry registry;

    /**
     * Applies structure modifications to a data sets.
     */
    public EvaluateDsStructureStrategyImpl(DataSetListRepository dslRepo,
                                           DataSetRepository dsRepo,
                                           AliasWrapperService aliasWrapperService,
                                           MacroRegistry registry) {
        this.dslRepo = dslRepo;
        this.dsRepo = dsRepo;
        this.aliasWrapperService = aliasWrapperService;
        this.registry = registry;
    }

    @Nonnull
    @Override
    public Stream<DataSet> getDataSets(@Nonnull DataSetList parent, @Nonnull Stream<DataSet> children) {
        Collection<Attribute> structureChanges = parent.getAttributes(AttributeType.CHANGE);
        if (structureChanges.isEmpty()) {
            return children;
        }
        return children.flatMap(ds -> this.applyStructureChanges(structureChanges, parent, ds));
    }

    @Override
    public DataSet getDataSetById(MixInId dataSet) {
        DataSet ds = dsRepo.getById(dataSet.getUuid());
        Preconditions.checkNotNull(ds, "Data set not found by id " + dataSet);
        if (dataSet.getMetadata() == null) {
            return ds;
        }
        DataSetList dsl = ds.getDataSetList();
        Collection<Attribute> structureChanges = dsl.getAttributes(AttributeType.CHANGE);
        List<MultiplicationEntry> combination;
        try {
            combination = ChangeParameter.getCombination(ChangeParameter.getChanges(structureChanges, ds),
                    dataSet.getMetadata());
        } catch (Exception e) {
            throw new RuntimeException("Can not find a combination for " + dataSet, e);
        }
        return applyStructureChanges(dsl, ds, combination);
    }

    @Nonnull
    private Stream<DataSet> applyStructureChanges(@Nonnull Collection<Attribute> structureChanges,
                                                  DataSetList dsl,
                                                  DataSet ds) {
        Stream<ChangeParameter> changes = ChangeParameter.getChanges(structureChanges, ds);
        Stream<List<MultiplicationEntry>> combinations = ChangeParameter.getCombinations(changes);
        return combinations.map(combination -> applyStructureChanges(dsl, ds, combination));
    }

    private DataSet applyStructureChanges(DataSetList dsl, DataSet ds, List<MultiplicationEntry> combination) {
        DataSet dataSet = MultiplicationMixin.create(combination, dslRepo, dsRepo, dsl, ds);
        String name = evaluateName(dsl, dataSet);
        dataSet.setName(name);
        return dataSet;
    }

    private String evaluateName(DataSetList dsl, DataSet dataSet) {
        String name = org.qubership.atp.dataset.utils.Utils.replacePlaceholders(dataSet.getName(), macros ->
                "#REF_THIS(" + macros + ")");
        name = aliasWrapperService.wrapToAlias(name, dsl.getVisibilityArea(), dsl);
        EvaluateDsParamStrategyImpl macrosEval = new EvaluateDsParamStrategyImpl(registry, aliasWrapperService,
                NoCache.INSTANCE, false);
        return macrosEval.evaluateText(dataSet, name);
    }
}
