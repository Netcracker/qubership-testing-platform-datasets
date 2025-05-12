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

package org.qubership.atp.dataset.macros.args;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;

public interface MacroArgFactory {

    TextArg.WithSignature text();

    /**
     * Finds dsl by name in context Va.
     */
    RefArg.Signature<DataSetList> dsl();

    /**
     * Finds ds by name in context dsl.
     */
    RefArg.Signature<DataSet> ds();

    /**
     * Finds attr by name in context dsl.
     */
    RefArg.Signature<Attribute> attr();
}
