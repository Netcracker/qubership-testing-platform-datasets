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

package org.qubership.atp.dataset.macros;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runners.Suite;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.qubership.atp.dataset.macros.impl.DateMacrosTest;
import org.qubership.atp.dataset.macros.impl.InnMacrosTest;
import org.qubership.atp.dataset.macros.impl.LookupMacrosTest;
import org.qubership.atp.dataset.macros.impl.RandomBetweenMacrosTest;
import org.qubership.atp.dataset.macros.impl.RandomCharMacrosTest;
import org.qubership.atp.dataset.macros.impl.RandomCharUpperCaseMacrosTest;
import org.qubership.atp.dataset.macros.impl.ReferenceMacrosTest;
import org.qubership.atp.dataset.macros.impl.ReferenceThisDSMacrosTest;
import org.qubership.atp.dataset.macros.impl.ReferenceToDslMacrosTest;
import org.qubership.atp.dataset.macros.impl.SumMacrosTest;
import org.qubership.atp.dataset.macros.impl.UUIDMacrosTest;
import org.qubership.atp.dataset.macros.impl.UUIDUpperCaseMacrosTest;
import org.qubership.atp.dataset.macros.parser.ParsingStateTest;
import org.qubership.atp.dataset.macros.parser.TokensIteratorTest;
import org.qubership.atp.dataset.macros.processor.AbstractMacroProcessorTest;
import org.qubership.atp.dataset.macros.processor.RefAliasProcessorTest;

@ExtendWith(SpringExtension.class)
@Suite.SuiteClasses({
        ParsingStateTest.class,
        TokensIteratorTest.class,
        AbstractMacroProcessorTest.class,
        RefAliasProcessorTest.class,
        UUIDMacrosTest.class,
        UUIDUpperCaseMacrosTest.class,
        ReferenceMacrosTest.class,
        SumMacrosTest.class,
        RandomBetweenMacrosTest.class,
        DateMacrosTest.class,
        RandomCharMacrosTest.class,
        RandomCharUpperCaseMacrosTest.class,
        ReferenceToDslMacrosTest.class,
        ReferenceThisDSMacrosTest.class,
        LookupMacrosTest.class,
        InnMacrosTest.class
})
public class MacrosTestSuit {

}
