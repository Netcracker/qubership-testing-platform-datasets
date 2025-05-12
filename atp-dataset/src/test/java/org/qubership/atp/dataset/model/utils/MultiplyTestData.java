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

import java.util.List;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.service.direct.helper.CreationFacade;

public class MultiplyTestData implements Supplier<VisibilityArea> {
    public static final String TEST_NAME = "Account: B2B, Subscription: Pro Start";
    public static final String TEST_ER = "{"
            + "  \"Account\" : {"
            + "    \"AccountNum\" : \"B2B1\","
            + "    \"TaxInclusiveBoo\" : \"FALSE\""
            + "  },"
            + "  \"Subscription\" : {"
            + "    \"ProductId\" : \"10002\","
            + "    \"TariffId\" : \"10036\""
            + "  }"
            + "}";
    public static final List<String> DS_NAMES = Lists.newArrayList("Account: Residential, Subscription: BASE Check 25",
            "Account: Residential, Subscription: Pro Start",
            "Account: Residential, Subscription: BASE 30",
            "Account: B2B, Subscription: BASE Check 25",
            TEST_NAME,
            "Account: B2B, Subscription: BASE 30");
    public static final List<String> ACCOUNT_NAMES = Lists.newArrayList("Residential", "B2B");
    public static final List<String> SUBSCRIPTION_NAMES = Lists.newArrayList("BASE Check 25", "Pro Start", "BASE 30");
    public static final String DS_NAME_W_PLACEHOLDERS = "Account: ${Account}, Subscription: ${Subscription}";

    public final VisibilityArea va;
    public final DataSetList accounts;
    public final DataSet resCA;
    public final Parameter resAccNum;
    public final Attribute accNum;
    public final Parameter resTax;
    public final Attribute tax;

    public final DataSet b2bCA;
    public final Parameter b2bAccNum;
    public final Parameter b2bTax;

    public final DataSetList subscriptions;

    public final DataSet baseCheck;
    public final Parameter baseCheckProductId;
    public final Attribute productId;
    public final Parameter baseCheckTariffId;
    public final Attribute tariffId;

    public final DataSet proStart;
    public final Parameter proStartProductId;
    public final Parameter proStartTariffId;

    public final DataSet base;
    public final Parameter baseProductId;
    public final Parameter baseTariffId;

    public final DataSetList requested;
    public final DataSet mix;

    public final Parameter mixAccount;
    public final Parameter mixSubscription;
    public final String expectedJson;


    public MultiplyTestData(CreationFacade create) {
        va = create.va("va");

        accounts = create.dsl(va, "Account");

        resCA = create.ds(accounts, ACCOUNT_NAMES.get(0));
        resAccNum = create.textParam(resCA, "AccountNum", "RES#RANDOMBETWEEN(100000000,999999999)");
        accNum = resAccNum.getAttribute();
        resTax = create.listParam(resCA, "TaxInclusiveBoo", "TRUE", "TRUE", "FALSE");
        tax = resTax.getAttribute();

        b2bCA = create.ds(accounts, ACCOUNT_NAMES.get(1));
        b2bAccNum = create.textParam(b2bCA, accNum, "B2B#RANDOMBETWEEN(1,1)");
        b2bTax = create.listParam(b2bCA, tax, "FALSE");

        subscriptions = create.dsl(va, "Subscription");

        baseCheck = create.ds(subscriptions, SUBSCRIPTION_NAMES.get(0));
        baseCheckProductId = create.textParam(baseCheck, "ProductId", "649");
        productId = baseCheckProductId.getAttribute();
        baseCheckTariffId = create.textParam(baseCheck, "TariffId", "1210");
        tariffId = baseCheckTariffId.getAttribute();

        proStart = create.ds(subscriptions, SUBSCRIPTION_NAMES.get(1));
        proStartProductId = create.textParam(proStart, productId, "10002");
        proStartTariffId = create.textParam(proStart, tariffId, "10036");

        base = create.ds(subscriptions, SUBSCRIPTION_NAMES.get(2));
        baseProductId = create.textParam(base, productId, "649");
        baseTariffId = create.textParam(base, tariffId, "1203");

        requested = create.dsl(va, "Requested");
        mix = create.ds(requested, DS_NAME_W_PLACEHOLDERS);

        mixAccount = create.schangeParam(mix, "Account", accounts, ChangeType.MULTIPLY, resCA.getId(),
                b2bCA.getId());
        mixSubscription = create.schangeParam(mix, "Subscription", subscriptions, ChangeType.MULTIPLY,
                baseCheck.getId(), proStart.getId(), base.getId());

        expectedJson = "{"
                + "    \"id\": \"" + mix.getDataSetList().getId() + "\","
                + "    \"name\": \"Requested\","
                + "    \"dataSets\": [{"
                + "            \"id\": \"" + mix.getId() + "\","
                + "            \"name\": \"Account: ${Account}, Subscription: ${Subscription}\","
                + "            \"locked\": false"
                + "        }"
                + "    ],"
                + "    \"attributes\": [{"
                + "            \"id\": \"" + mixAccount.getAttribute().getId() + "\","
                + "            \"name\": \"Account\","
                + "            \"type\": \"CHANGE\","
                + "            \"dataSetListReference\": \"" + accounts.getId() + "\","
                + "            \"parameters\": [{"
                + "                    \"dataSet\": \"" + mix.getId() + "\","
                + "                    \"value\": "
                + String.format("          \"MULTIPLY %s %s\"", resCA.getId(), b2bCA.getId())
                + "                    ,"
                + "                    \"overlap\": false"
                + "                }"
                + "            ]"
                + "        }, {"
                + "            \"id\": \"" + mixSubscription.getAttribute().getId() + "\","
                + "            \"name\": \"Subscription\","
                + "            \"type\": \"CHANGE\","
                + "            \"dataSetListReference\": \"" + subscriptions.getId() + "\","
                + "            \"parameters\": [{"
                + "                    \"dataSet\": \"" + mix.getId() + "\","
                + "                    \"value\": "
                + String.format("          \"MULTIPLY %s %s %s\"", baseCheck.getId(), proStart.getId(), base.getId())
                + "                    ,"
                + "                    \"overlap\": false"
                + "                }"
                + "            ]"
                + "        }"
                + "    ]"
                + "}";
    }


    @Override
    public VisibilityArea get() {
        return this.va;
    }
}
