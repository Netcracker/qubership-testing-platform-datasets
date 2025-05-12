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

package org.qubership.atp.dataset.db;

import static org.qubership.atp.dataset.config.TransactionTestConfiguration.DbUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.qubership.atp.crypt.api.Decryptor;
import org.qubership.atp.crypt.api.Encryptor;
import org.qubership.atp.dataset.config.TransactionTestConfiguration;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.service.direct.AttributeService;
import org.qubership.atp.dataset.service.direct.DataSetListService;
import org.qubership.atp.dataset.service.direct.VisibilityAreaService;

@Disabled
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TransactionTestConfiguration.class})
public class TransactionInterceptorTest {

    // TODO ATPII-39305 resolve PSQLException
    private final String dslName = "TransactionInterceptorTestDsl";
    private final String dslNameRenamed = "TransactionInterceptorTestDslAfterRename";
    @Autowired
    private VisibilityAreaService visibilityAreaService;
    @Autowired
    private DataSetListService dataSetListService;
    @Autowired
    private AttributeService attributeService;
    @Autowired
    private DbUtils dbUtils;
    @MockBean
    protected Encryptor encryptor;
    @MockBean
    protected Decryptor decryptor;

    private VisibilityArea va;
    private DataSetList dsl;
    private Attribute attributeOne;
    private Attribute attributeSecond;

    @BeforeEach
    public void setUp() {
        this.va = visibilityAreaService.create("TransactionInterceptorTestVa");
        this.dsl = dataSetListService.create(this.va.getId(), dslName, null);
        this.attributeOne = attributeService.create(
                dsl.getId(), 0, "TransactionInterceptorTestAttrOne", AttributeType.TEXT, null, null
        );
        this.attributeSecond = attributeService.create(
                dsl.getId(), 1, "TransactionInterceptorTestAttrSecond", AttributeType.TEXT, null, null
        );
    }

    @Test
    public void whenOperationsPassedThenCommitHappens() {
        Assertions.assertNotNull(this.attributeOne);
        try {
            operationsInTransaction();
        } catch (Exception e) {
            /*Unexpected Exception, do nothing*/
        }
        Attribute attribute = attributeService.get(this.attributeOne.getId());
        Assertions.assertNull(attribute);
        String dslNameExpected = dataSetListService.get(this.dsl.getId()).getName();
        Assertions.assertEquals(dslNameExpected, this.dslNameRenamed);
    }

    public void operationsInTransaction() {
        dbUtils.operationsInTransaction(this.attributeOne.getId(), this.dsl.getId(), dslNameRenamed);
    }

    @AfterEach
    public void tearDown() {
        visibilityAreaService.delete(this.va.getId());
    }
}
