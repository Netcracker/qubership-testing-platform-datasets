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

package org.qubership.atp.dataset.macros.impl;

import java.util.function.Supplier;

import org.junit.jupiter.api.parallel.Isolated;

import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.service.direct.helper.CreationFacade;

@Isolated
public class ReferenceMacrosTestData implements Supplier<VisibilityArea> {
    public final String expectedValue = "Passed";
    public final VisibilityArea va;
    public final DataSetList externalDsl;
    public final DataSet externalDs;
    public final Parameter externalParam;
    public final DataSetList dsl;
    public final DataSet ds;
    public final Parameter externalDsRef;
    public final Parameter param;

    public ReferenceMacrosTestData(CreationFacade create) {
        this.va = create.va("RefMacrosTest");
        this.externalDsl = create.dsl(va, "ExternalDSL");
        this.externalDs = create.ds(externalDsl, "ExternalDS");
        this.externalParam = create.textParam(externalDs, "ExternalAttr", expectedValue);
        this.dsl = create.dsl(va, "DSL");
        this.ds = create.ds(dsl, "DS");
        this.externalDsRef = create.refParam(ds, "DS_ExternalDS", externalDs);
        this.param = create.textParam(ds, "Attr", expectedValue);
    }

    @Override
    public VisibilityArea get() {
        return va;
    }
}
