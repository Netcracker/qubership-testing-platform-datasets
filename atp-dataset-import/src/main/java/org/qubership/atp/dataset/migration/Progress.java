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

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Progress {

    private final Consumer<Integer> progressConsumer;
    private final long totalItems;
    private double rawProgress = 0;
    private int progress = 0;

    public Progress(Consumer<Integer> progressConsumer, long totalItems) {
        this.progressConsumer = progressConsumer;
        this.totalItems = totalItems;
    }

    /**
     * Wraps progress consumer into another one with 2 secs threshold between toggles.
     */
    public static Progress withTimeThreshold(Consumer<Integer> progressConsumer, long totalItems) {
        long[] lastNotifyTime = {-1};
        return new Progress(percent -> {
            long curNotifyTime = System.currentTimeMillis();
            if (curNotifyTime - lastNotifyTime[0] > TimeUnit.SECONDS.toMillis(2)) {
                progressConsumer.accept(percent);
                lastNotifyTime[0] = curNotifyTime;
            }
        }, totalItems);
    }

    /**
     * Increments progress by provided amount.
     */
    public void increment(double by) {
        //overflow is possible
        rawProgress += by;
        int newProgress = (int) (rawProgress * 100 / totalItems);
        if (progress != newProgress) {
            progressConsumer.accept(newProgress);
            progress = newProgress;
        }
    }
}
