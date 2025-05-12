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

package org.qubership.atp.dataset.model.utils;

import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.MixInId;
import org.qubership.atp.dataset.service.direct.macros.DsEvaluator;

public class EvaluatorMock implements DsEvaluator {

    public static final EvaluatorMock INSTANCE = new EvaluatorMock();

    @Nonnull
    @Override
    public Stream<DataSet> getDataSets(@Nonnull DataSetList parent, @Nonnull Stream<DataSet> children) {
        return children;
    }

    @Nullable
    @Override
    public DataSet getDataSetById(MixInId dataSet) {
        return null;
    }

    @Override
    public void close() {
    }

    @Nonnull
    @Override
    public Optional<String> apply(@Nonnull OverlapItem.Reachable target) {
        return target.getValue();
    }

    @Nonnull
    @Override
    public String evaluateText(@Nonnull DataSet ds, @Nonnull String text) {
        return text;
    }
}
