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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MainLoggerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainLoggerTest.class);
    private static final int MESSAGES_NUM = 10;
    private static final String MESSAGE_PATTERN = "Example message: [{}]";
    private static final Throwable SAMPLE_ERROR = new Exception("There are sample error");

    private MainLoggerTest() {
    }

    /**
     * Prints sequence of messages for all levels.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        for (int i = 1; i <= MESSAGES_NUM; i++) {
            doPrint(i);
        }
    }

    private static void doPrint(Object info) {
        LOGGER.trace(MESSAGE_PATTERN, info);
        LOGGER.debug(MESSAGE_PATTERN, info);
        LOGGER.info(MESSAGE_PATTERN, info);
        LOGGER.warn(MESSAGE_PATTERN, info, SAMPLE_ERROR);
        LOGGER.error(MESSAGE_PATTERN, info, SAMPLE_ERROR);
    }
}
