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

package org.qubership.atp.dataset.service.direct.macros;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.MixInId;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.utils.OverlapItem;
import org.qubership.atp.dataset.model.utils.OverlapIterator;
import org.qubership.atp.dataset.service.direct.macros.schange.EvaluateDsStructureStrategy;

public interface DsEvaluator extends EvaluateDsParamStrategy, EvaluateDsStructureStrategy, AutoCloseable {

    @Nonnull
    Stream<DataSet> getDataSets(@Nonnull DataSetList parent, @Nonnull Stream<DataSet> children);

    @Nullable
    DataSet getDataSetById(MixInId dataSet);

    /**
     * Used when parameter lays directly under it's data set. May be an overlap.
     *
     * @deprecated use {@link #apply(OverlapItem.Reachable)} because it is forces the usage of
     * {@link OverlapIterator}. And ensures that you provide the context.
     */
    @Deprecated
    default Optional<String> apply(@Nonnull Parameter parameter) {
        return apply(new OverlapItem.DefaultInitialized(
                parameter.getDataSet(),
                Collections.emptyList(),
                parameter.getDataSet(),
                parameter));
    }

    @Override
    void close();
}
