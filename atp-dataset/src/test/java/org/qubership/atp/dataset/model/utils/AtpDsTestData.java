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

import java.util.function.Supplier;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.service.direct.helper.CreationFacade;

public class AtpDsTestData {
    public static class EmptyParameters implements Supplier<VisibilityArea> {
        public final VisibilityArea va;
        public final DataSetList dsl;
        public final DataSet ds;
        public final Parameter param1;
        public final Attribute emptyParam2;
        public final Attribute emptyParam3;
        public final DataSet refDs;
        public final Attribute dslRef;
        public final String erJson;
        public final String erJson2;

        public EmptyParameters(CreationFacade create) {
            va = create.va("ATPII-3009");
            dsl = create.dsl(va, "EmptyParameters");
            ds = create.ds(dsl, "DS");
            param1 = create.textParam(ds, "param1", "#Context(TEST_RUN_NAME)");
            emptyParam2 = create.fileAttr(ds.getDataSetList(), "param2");
            emptyParam3 = create.listAttr(ds.getDataSetList(), "param3", "Disconnected");
            refDs = create.ds(va, "Modify Internet + Phone", "Default");
            dslRef = create.refAttr(ds.getDataSetList(), refDs.getDataSetList().getName(), refDs.getDataSetList());
            erJson = "{"
                    + "  \"parameters\": {"
                    + "    \"param1\": {"
                    + "      \"type\": \"TEXT\","
                    + "      \"value\": \"#Context(TEST_RUN_NAME)\""
                    + "    },"
                    + "    \"param2\": {"
                    + "      \"type\": \"FILE\""
                    + "    },"
                    + "    \"param3\": {"
                    + "      \"type\": \"LIST\""
                    + "    }"
                    + "  },"
                    + "  \"groups\": {"
                    + "    \"Modify Internet + Phone\": {"
                    + "      \"type\": \"DSL\""
                    + "      }"
                    + "    }"
                    + "  }"
                    + "}";

            erJson2 = "{"
                    + "  \"parameters\": {"
                    + "    \"param1\": {"
                    + "      \"type\": \"TEXT\","
                    + "      \"value\": \"#Context(TEST_RUN_NAME)\""
                    + "    },"
                    + "    \"param2\": {"
                    + "      \"type\": \"FILE\""
                    + "    },"
                    + "    \"param3\": {"
                    + "      \"type\": \"LIST\""
                    + "    }"
                    + "  },"
                    + "  \"groups\": {"
                    + "    \"Modify Internet + Phone\": {"
                    + "      \"type\": \"DSL\","
                    + "      \"dsl\" : \"Modify Internet + Phone\""
                    + "      }"
                    + "    }"
                    + "  }"
                    + "}";
        }


        @Override
        public VisibilityArea get() {
            return va;
        }
    }

    public static class ShuffleGroups implements Supplier<VisibilityArea> {
        public final VisibilityArea va;
        public final DataSetList dsl;
        public final DataSet ds;
        public final Parameter param1;
        public final Parameter param2;
        public final Parameter param3;
        public final DataSet refDs1;
        public final Parameter dsRef1;
        public final Parameter ref1Param1;
        public final Parameter ref1Param2;
        public final DataSet refDs2;
        public final Parameter dsRef2;
        public final Parameter ref2Param1;
        public final String erJson;

        public ShuffleGroups(CreationFacade create) {
            va = create.va("ATPII-3009");
            dsl = create.dsl(va, "ShuffleGroups");
            ds = create.ds(dsl, "DS");
            param1 = create.textParam(ds, "param1", "abc");

            refDs1 = create.ds(va, "Modify Internet + Phone", "Default");
            dsRef1 = create.refParam(ds, refDs1);
            ref1Param1 = create.textParam(refDs1, "param1", "abc");
            ref1Param2 = create.fileParam(refDs1, "param2", "putty.exe", "application/x-msdownload");

            param2 = create.fileParam(ds, "param2", "rest.txt", "plain/text");

            refDs2 = create.ds(va, "Files", "Default files");
            dsRef2 = create.refParam(ds, refDs2);
            ref2Param1 = create.fileParam(refDs2, "param1", "putty.exe", "application/x-msdownload");

            param3 = create.listParam(ds, "param3", "Disconnected", "Disconnected");

            erJson = "{"
                    + "  \"parameters\": {"
                    + "    \"param1\": {"
                    + "      \"type\": \"TEXT\","
                    + "      \"value\": \"abc\""
                    + "    },"
                    + "    \"param2\": {"
                    + "      \"type\": \"FILE\","
                    + "      \"value\": \"" + param2.getId() + ".txt\","
                    + "      \"valueRef\": {"
                    + "        \"contentType\": \"plain/text\","
                    + "        \"url\": \"/attachment/" + param2.getId() + "\""
                    + "      }"
                    + "    },"
                    + "    \"param3\": {"
                    + "      \"type\": \"LIST\","
                    + "      \"value\": \"Disconnected\""
                    + "    }"
                    + "  },"
                    + "  \"groups\": {"
                    + "    \"Modify Internet + Phone\": {"
                    + "      \"type\": \"DSL\","
                    + "      \"value\": \"Default\","
                    + "      \"parameters\": {"
                    + "        \"param1\": {"
                    + "          \"type\": \"TEXT\","
                    + "          \"value\": \"abc\""
                    + "        },"
                    + "        \"param2\": {"
                    + "          \"type\": \"FILE\","
                    + "          \"value\": \"" + ref1Param2.getId() + ".exe\","
                    + "          \"valueRef\": {"
                    + "            \"contentType\": \"application/x-msdownload\","
                    + "            \"url\": \"/attachment/" + ref1Param2.getId() + "\""
                    + "          }"
                    + "        }"
                    + "      }"
                    + "    },"
                    + "    \"Files\": {"
                    + "      \"type\": \"DSL\","
                    + "      \"value\": \"Default files\","
                    + "      \"parameters\": {"
                    + "        \"param1\": {"
                    + "          \"type\": \"FILE\","
                    + "          \"value\": \"" + ref2Param1.getId() + ".exe\","
                    + "          \"valueRef\": {"
                    + "            \"contentType\": \"application/x-msdownload\","
                    + "            \"url\": \"/attachment/" + ref2Param1.getId() + "\""
                    + "          }"
                    + "        }"
                    + "      }"
                    + "    }"
                    + "  }"
                    + "}";
        }

        @Override
        public VisibilityArea get() {
            return va;
        }
    }

    public static class GroupInGroup implements Supplier<VisibilityArea> {
        public final VisibilityArea va;
        public final DataSetList dsl;
        public final DataSet ds;
        public final Parameter param1;
        public final Parameter param2;
        public final Parameter param3;
        public final DataSet refDs;
        public final Parameter dsRef;
        public final Parameter refParam1;
        public final Parameter refParam2;
        public final DataSet refRefDs;
        public final Parameter dsRefRef;
        public final Parameter refRefParam1;
        public final String erJson;

        public GroupInGroup(CreationFacade create) {
            va = create.va("ATPII-3009");
            dsl = create.dsl(va, "GroupInGroup");
            ds = create.ds(dsl, "DS");
            param1 = create.textParam(ds, "param1", "abc");
            param2 = create.fileParam(ds, "param2", "rest.txt", "plain/text");
            param3 = create.listParam(ds, "param3", "Disconnected", "Disconnected");
            refDs = create.ds(va, "Modify Internet + Phone", "Default");
            dsRef = create.refParam(ds, refDs);
            refParam1 = create.textParam(refDs, "param1", "abc");
            refParam2 = create.fileParam(refDs, "param2", "putty.exe", "application/x-msdownload");
            refRefDs = create.ds(va, "Files", "Default files");
            dsRefRef = create.refParam(refDs, refRefDs);
            refRefParam1 = create.fileParam(refRefDs, "param1", "putty.exe", "application/x-msdownload");
            erJson = "{"
                    + "  \"parameters\": {"
                    + "    \"param1\": {"
                    + "      \"type\": \"TEXT\","
                    + "      \"value\": \"abc\""
                    + "    },"
                    + "    \"param2\": {"
                    + "      \"type\": \"FILE\","
                    + "      \"value\": \"" + param2.getId() + ".txt\","
                    + "      \"valueRef\": {"
                    + "        \"contentType\": \"plain/text\","
                    + "        \"url\": \"/attachment/" + param2.getId() + "\""
                    + "      }"
                    + "    },"
                    + "    \"param3\": {"
                    + "      \"type\": \"LIST\","
                    + "      \"value\": \"Disconnected\""
                    + "    }"
                    + "  },"
                    + "  \"groups\": {"
                    + "    \"Modify Internet + Phone\": {"
                    + "      \"type\": \"DSL\","
                    + "      \"value\": \"Default\","
                    + "      \"parameters\": {"
                    + "        \"param1\": {"
                    + "          \"type\": \"TEXT\","
                    + "          \"value\": \"abc\""
                    + "        },"
                    + "        \"param2\": {"
                    + "          \"type\": \"FILE\","
                    + "          \"value\": \"" + refParam2.getId() + ".exe\","
                    + "          \"valueRef\": {"
                    + "            \"contentType\": \"application/x-msdownload\","
                    + "            \"url\": \"/attachment/" + refParam2.getId() + "\""
                    + "          }"
                    + "        }"
                    + "      },"
                    + "      \"groups\": {"
                    + "          \"Files\": {"
                    + "          \"type\": \"DSL\","
                    + "          \"value\": \"Default files\","
                    + "          \"parameters\": {"
                    + "            \"param1\": {"
                    + "              \"type\": \"FILE\","
                    + "              \"value\": \"" + refRefParam1.getId() + ".exe\","
                    + "              \"valueRef\": {"
                    + "                \"contentType\": \"application/x-msdownload\","
                    + "                \"url\": \"/attachment/" + refRefParam1.getId() + "\""
                    + "              }"
                    + "            }"
                    + "          }"
                    + "        }"
                    + "      }"
                    + "    }"
                    + "  }"
                    + "}";
        }

        @Override
        public VisibilityArea get() {
            return va;
        }
    }
}
