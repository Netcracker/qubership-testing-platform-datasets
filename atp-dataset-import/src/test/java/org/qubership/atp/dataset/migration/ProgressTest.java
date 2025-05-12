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

package org.qubership.atp.dataset.migration;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProgressTest {

    @Test
    public void progressIncrements_By10_Got10Percents() {
        AtomicInteger curProgress = new AtomicInteger(0);
        Progress instance = new Progress(curProgress::set, 100);
        instance.increment(10);
        Assertions.assertEquals(10, curProgress.get());
    }

    @Test
    public void progressIncrements_By09_DidNotGotAnyPercents() {
        Progress instance = new Progress(percent -> {
            throw new IllegalStateException("Should not be invoked");
        }, 100);
        instance.increment(0.9);
    }

    @Test
    public void progressIncrements_ThreeTimesBy04_Got1Percent() {
        AtomicInteger curProgress = new AtomicInteger(0);
        Progress instance = new Progress(curProgress::set, 100);
        instance.increment(0.4);
        instance.increment(0.4);
        instance.increment(0.4);
        Assertions.assertEquals(1, curProgress.get());
    }

    @Test
    public void progressIncrements_By100And01_got100Percents() {
        AtomicInteger curProgress = new AtomicInteger(0);
        Progress instance = new Progress(curProgress::set, 100);
        instance.increment(99);
        instance.increment(0.4);
        instance.increment(0.7);
        Assertions.assertEquals(100, curProgress.get());
    }

    @Test
    public void progressIncrements_By1000_got1000Percents() {
        AtomicInteger curProgress = new AtomicInteger(0);
        Progress instance = new Progress(curProgress::set, 100);
        instance.increment(1000);
        Assertions.assertEquals(1000, curProgress.get());
    }
}
