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

package org.qubership.atp.dataset.service.direct.impl;

import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.test.context.ContextConfiguration;

import org.qubership.atp.dataset.config.MockJaversCommitEntityServiceConfiguration;
import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.service.AbstractTest;
import org.qubership.atp.dataset.service.direct.helper.CreationFacade;

@Isolated
@ContextConfiguration(classes = {TestConfiguration.class, MockJaversCommitEntityServiceConfiguration.class})
public class ReferenceResolveServiceImplTest extends AbstractTest {

    private TestData data;

    @BeforeEach
    public void setUp() {
        data = createTestDataInstance(TestData::new);
    }

    @Test
    public void testRefDslWrapperReturnsWrappedToAliasInsteadOfNames() {
        String resolve = wrapperService.wrapToAlias("dasdasd #REF_DSL(DSL.DS.ATTR)", data.va, data.dsl);
        resolve = resolve.substring(17, resolve.length() - 1);
        String[] split = resolve.split("\\.");
        String dslAliasId = split[0];
        String dsAliasId = split[1];
        String attrAliasId = split[2];
        Assertions.assertNotNull(dslAliasId);
        Assertions.assertNotNull(dsAliasId);
        Assertions.assertNotNull(split[2]);
        Assertions.assertEquals(data.attr.getId(), UUID.fromString(attrAliasId));
    }

    @Test
    public void testRefDslWrapperReturnsUnwrappedNamesOfObjects() {
        String expected = "dasdasd #REF_DSL(DSL.DS.ATTR)";
        String wrapped = wrapperService.wrapToAlias(expected, data.va, data.dsl);
        String unwrapped = wrapperService.unWrapAlias(wrapped);
        Assertions.assertEquals(expected, unwrapped);
    }

    @Test
    public void testRefWrapperReturnsWrappedToAliasInsteadOfNames() {
        String resolve = wrapperService.wrapToAlias("dasdasd #REF(DS.ATTR)", data.va, data.dsl);
        resolve = resolve.substring(13, resolve.length() - 1);
        String[] split = resolve.split("\\.");
        String dsAliasId = split[0];
        String attrAliasId = split[1];
        Assertions.assertNotNull(dsAliasId);
        Assertions.assertNotNull(attrAliasId);
        Assertions.assertEquals(data.attr.getId(), UUID.fromString(attrAliasId));
    }

    @Test
    public void testRefWrapperReturnsUnwrappedNamesOfObjects() {
        String expected = "dasdasd #REF(DS.ATTR)";
        String wrapped = wrapperService.wrapToAlias(expected, data.va, data.dsl);
        String unwrapped = wrapperService.unWrapAlias(wrapped);
        Assertions.assertEquals(expected, unwrapped);
    }

    @Test
    public void testRefAndRefDslWrapperReturnsUnwrappedNamesOfObjects() {
        String expected = "dasdasd #REF(DS.ATTR) 123 #REF_DSL(DSL.DS.ATTR)";
        String wrapped = wrapperService.wrapToAlias(expected, data.va, data.dsl);
        String unwrapped = wrapperService.unWrapAlias(wrapped);
        Assertions.assertEquals(expected, unwrapped);
    }

    @Test
    public void testWrapperReturnsSourceValueIfError() {
        String expected = "dasdasd #REF(DS1.ATTR) 123 #REF_DSL(D3SL.DS.ATTR)";
        String wrapped = wrapperService.wrapToAlias(expected, data.va, data.dsl);
        Assertions.assertEquals(expected, wrapped);
        String unwrapped = wrapperService.unWrapAlias(wrapped);
        Assertions.assertEquals(expected, unwrapped);
    }

    @Test
    public void testNestedMacro_withUnion() {
        String source = "dasdasd #REF_DSL(DSL.#REF(DS.ATTR).#REF_THIS(ATTR)) 123 #REF_DSL(DSL.DS.ATTR)";
        String wrapped = wrapperService.wrapToAlias(source, data.va, data.dsl);
        UUID dslRef = data.dsl.getId();
        UUID dsRef = data.ds.getId();
        UUID attrRef = data.attr.getId();
        String expected = String.format("dasdasd #REF_DSL(%s.#REF(%s.%s).#REF_THIS(%s)) 123 #REF_DSL(%s.%s.%s)",
                dslRef, dsRef, attrRef, attrRef, dslRef, dsRef, attrRef);
        Assertions.assertEquals(expected, wrapped);
        String unwrapped = wrapperService.unWrapAlias(wrapped);
        Assertions.assertEquals(source, unwrapped);
    }

    @Test
    public void testNestedMacro_withAdditionalArgs() {
        String source = "dasdasd #REF_DSL(DSL.#REF(DS.ATTR).ATTR) 123 #REF_DSL(DSL.DS.ATTR)";
        String wrapped = wrapperService.wrapToAlias(source, data.va, data.dsl);
        UUID dslRef = data.dsl.getId();
        UUID dsRef = data.ds.getId();
        UUID attrRef = data.attr.getId();
        String expected = String.format("dasdasd #REF_DSL(%s.#REF(%s.%s).ATTR) 123 #REF_DSL(%s.%s.%s)",
                dslRef, dsRef, attrRef, dslRef, dsRef, attrRef);
        Assertions.assertEquals(expected, wrapped);
        String unwrapped = wrapperService.unWrapAlias(wrapped);
        Assertions.assertEquals(source, unwrapped);
    }

    public static class TestData implements Supplier<VisibilityArea> {

        private final VisibilityArea va;
        private final DataSetList dsl;
        private final DataSet ds;
        private final Attribute attr;
        private final Parameter param;

        public TestData(CreationFacade create) {
            va = create.va("ReferenceResolveServiceImplTestVa");
            dsl = create.dsl(va, "DSL");
            ds = create.ds(dsl, "DS");
            attr = create.textAttr(dsl, "ATTR");
            param = create.textParam(ds, attr, "txt");
        }

        @Override
        public VisibilityArea get() {
            return va;
        }
    }
}
