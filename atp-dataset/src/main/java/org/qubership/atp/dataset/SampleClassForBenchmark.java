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

package org.qubership.atp.dataset;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public final class SampleClassForBenchmark {

    private static final Integer NUMBER_OF_ITERATIONS = 3;

    public Object[] testSimplestMethod() {
        return new Integer[]{1, 2};
    }

    /**
     * does the same as {@link #testSimplestMethod()} using streams, lambdas and functions.
     */
    public Object[] testLambdaMethod() {
        List<Integer> result = new ArrayList<>();
        IntStream.iterate(1, i -> i + 1).limit(2).forEach(result::add);
        return result.toArray();
    }

    /**
     * does the same as {@link #testSimplestMethod()} using 'for' cycle on array list.
     */
    public Object[] testMethod() {
        List<Integer> result = new ArrayList<>();
        for (int i = 1; i < NUMBER_OF_ITERATIONS; i++) {
            result.add(i);
        }
        return result.toArray();
    }
}
