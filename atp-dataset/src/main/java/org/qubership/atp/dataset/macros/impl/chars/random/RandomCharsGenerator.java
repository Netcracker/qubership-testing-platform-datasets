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

package org.qubership.atp.dataset.macros.impl.chars.random;

import java.util.Random;

public class RandomCharsGenerator {

    private static Random random = new Random();

    /**
     * Returns random char sequence.
     *
     * @param count how many chars will be generated
     * @return random characters sequence
     */
    public static String randomChars(int count) {
        StringBuilder builder = new StringBuilder(count);
        int min = 'a';
        int max = 'z';
        for (int index = 0; index < count; index++) {
            builder.append((char) (random.nextInt(max + 1 - min) + min)); //+1 to avoid situation [min;max)
        }
        return builder.toString();
    }
}
