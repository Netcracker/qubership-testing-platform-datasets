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

package org.qubership.atp.dataset.benchmarks;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.utils.EvaluatorMock;
import org.qubership.atp.dataset.model.utils.TestData;
import org.qubership.atp.dataset.model.utils.Utils;
import org.qubership.atp.dataset.service.direct.helper.SimpleCreationFacade;
import org.qubership.atp.dataset.service.direct.macros.DsEvaluator;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
public class Converting {

    @Benchmark
    public String uiDataSetListConverting(Converting.Data data) throws JsonProcessingException {
        return data.mapper.writeValueAsString(Utils.doUiDs(data.toConvert, data.eval, true));
    }

    @Benchmark
    public String flatDataConverting(Converting.Data data) throws JsonProcessingException {
        return data.mapper.writeValueAsString(Utils.doFlatData(data.toConvert));
    }

    @State(Scope.Benchmark)
    public static class Data {

        private ObjectMapper mapper;
        private DataSetList toConvert;
        private DsEvaluator eval = EvaluatorMock.INSTANCE;

        @Setup
        public void setup() {
            mapper = new ObjectMapper();
            toConvert = TestData.createBensCases(SimpleCreationFacade.INSTANCE, "va");
        }
    }
}
