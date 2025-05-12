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

package org.qubership.atp.dataset.migration.model;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class FalloutReport implements AutoCloseable {

    private PrintWriter writer;

    public FalloutReport(String reportFile) throws IOException {
        writer = new PrintWriter(new BufferedWriter(new FileWriter(reportFile)));
        report("LOCATION", "PROBLEM PLACE", "SHORT_MESSAGE", "DETAILED MESSAGE");
    }

    public void report(String location, String problemPlace, String shortMessage, String detailedMessage) {
        writer.println(String.format("%s\t%s\t%s\t%s", location, problemPlace, shortMessage, detailedMessage));
    }

    @Override
    public void close() {
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }
}
