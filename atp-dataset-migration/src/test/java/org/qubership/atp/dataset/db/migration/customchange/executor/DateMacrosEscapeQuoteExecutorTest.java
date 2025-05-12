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

package org.qubership.atp.dataset.db.migration.customchange.executor;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DateMacrosEscapeQuoteExecutorTest {

    private DateMacrosEscapeQuoteExecutor dateMacrosEscapeQuoteExecutor;

    @BeforeEach
    public void setUp(){
        dateMacrosEscapeQuoteExecutor = DateMacrosEscapeQuoteExecutor.builder().build();
    }

    private Map<String, String> dateCases = new LinkedHashMap<>();

    {
        dateCases.put("Date with # symbol, without T/Z #DATE(yyyy-mm-dd), return the same", "Date with # symbol, without T/Z #DATE(yyyy-mm-dd), return the same");
        dateCases.put("Date with $ symbol, without T/Z $DATE(yyyy-mm-dd), return the same", "Date with $ symbol, without T/Z $DATE(yyyy-mm-dd), return the same");
        dateCases.put("Date with T without quote #DATE(yyyy-mm-ddT), return the same", "Date with T without quote #DATE(yyyy-mm-ddT), return the same");
        dateCases.put("Date with T with quote #DATE(yyyy-mm-dd'T'), return escaped", "Date with T with quote #DATE(yyyy-mm-dd\\'T\\'), return escaped");
        dateCases.put("Date with T and different format #DATE(yyyy-MM-dd'T'HH:mm:ss), return escaped", "Date with T and different format #DATE(yyyy-MM-dd\\'T\\'HH:mm:ss), return escaped");
        dateCases.put("Date with T and different format #DATE(yyyy-MM-dd'T'HH:mm:ss.123+03:00), return escaped", "Date with T and different format #DATE(yyyy-MM-dd\\'T\\'HH:mm:ss.123+03:00), return escaped");
        dateCases.put("Date with T and different format #DATE(yyyy-MM-dd'T'HH:mm:ss.sss)Z, return escaped", "Date with T and different format #DATE(yyyy-MM-dd\\'T\\'HH:mm:ss.sss)Z, return escaped");
        dateCases.put("Date with T and different format #DATE(yyyy-MM-dd'T'HH:mm:ss+03:00), return escaped", "Date with T and different format #DATE(yyyy-MM-dd\\'T\\'HH:mm:ss+03:00), return escaped");
        dateCases.put("Date with T and Z with quote #DATE(yyyy-MM-dd'T'HH:mm:ss'Z'), return escaped", "Date with T and Z with quote #DATE(yyyy-MM-dd\\'T\\'HH:mm:ss\\'Z\\'), return escaped");
        dateCases.put("Date with T and Z different format #DATE(yyyy-MM-dd'T'HH:mm:ss.123'Z'), return escaped", "Date with T and Z different format #DATE(yyyy-MM-dd\\'T\\'HH:mm:ss.123\\'Z\\'), return escaped");
        dateCases.put("Date with T and Z different format #DATE(yyyy-MM-dd'T'HH:mm:ss.sss'Z'), return escaped", "Date with T and Z different format #DATE(yyyy-MM-dd\\'T\\'HH:mm:ss.sss\\'Z\\'), return escaped");
        dateCases.put("Date with T and Z two argument #DATE(-1d,yyyy-MM-dd'T'HH:mm:ss.sss'Z'), return escaped", "Date with T and Z two argument #DATE(-1d,yyyy-MM-dd\\'T\\'HH:mm:ss.sss\\'Z\\'), return escaped");
        dateCases.put("Date with T and Z two argument different format #DATE(-5h+20s, yyyy-MM-dd'T'HH:mm:ss), return escaped", "Date with T and Z two argument different format #DATE(-5h+20s, yyyy-MM-dd\\'T\\'HH:mm:ss), return escaped");
        dateCases.put("Date with T and Z two argument different format #DATE(+9m, yyyy-MM-dd'T'HH:mm:ss+03:00), return escaped", "Date with T and Z two argument different format #DATE(+9m, yyyy-MM-dd\\'T\\'HH:mm:ss+03:00), return escaped");
        dateCases.put("Date with T, parameter in quote #DATE('yyyy-mm-dd'T''), return only T escaped", "Date with T, parameter in quote #DATE('yyyy-mm-dd\\'T\\''), return only T escaped");
        dateCases.put("Date with T and Z, parameter in quote #DATE('yyyy-MM-dd'T'HH:mm:ss'Z''), return only T and Z escaped",
                "Date with T and Z, parameter in quote #DATE('yyyy-MM-dd\\'T\\'HH:mm:ss\\'Z\\''), return only T and Z escaped");
        dateCases.put("Date with T and Z, two argument, parameter in quote #DATE('+9h', 'yyyy-MM-dd'T'HH:mm:ss'Z''), return only T and Z escaped",
                "Date with T and Z, two argument, parameter in quote #DATE('+9h', 'yyyy-MM-dd\\'T\\'HH:mm:ss\\'Z\\''), return only T and Z escaped");
        dateCases.put("Date with T and Z, two argument, already escaped #DATE(-1d,yyyy-MM-dd\\'T\\'HH:mm:ss.sss\\'Z\\'), return the same",
                "Date with T and Z, two argument, already escaped #DATE(-1d,yyyy-MM-dd\\'T\\'HH:mm:ss.sss\\'Z\\'), return the same");
        dateCases.put("Some Date with T and Z, #DATE(yyyy-MM-dd'T'HH:mm:ss'Z') next date #DATE(yyyy-MM-dd'T'HH:mm:ss'Z') next date #DATE('+9h', 'yyyy-MM-dd'T'HH:mm:ss'Z''), return escaped",
                "Some Date with T and Z, #DATE(yyyy-MM-dd\\'T\\'HH:mm:ss\\'Z\\') next date #DATE(yyyy-MM-dd\\'T\\'HH:mm:ss\\'Z\\') next date #DATE('+9h', 'yyyy-MM-dd\\'T\\'HH:mm:ss\\'Z\\''), return escaped");
        dateCases.put("Date with T inside other macros #CUSTOM_MACROS(#DATE(yyyy-MM-dd'T')), return escaped", "Date with T inside other macros #CUSTOM_MACROS(#DATE(yyyy-MM-dd\\'T\\')), return escaped");
        dateCases.put("Date with T inside other macros Date #DATE('-4h', '#DATE('yyyy-MM-dd'T'')'), return escaped", "Date with T inside other macros Date #DATE('-4h', '#DATE('yyyy-MM-dd\\'T\\'')'), return escaped");
        dateCases.put("Date with T and other macros #CUSTOMEMACROS(#DATE(yyyy-MM-dd'T')) #CUSTOME('345'), return escaped", "Date with T and other macros #CUSTOMEMACROS(#DATE(yyyy-MM-dd\\'T\\')) #CUSTOME('345'), return escaped");
        dateCases.put("Date with T and Z, one quote escaped #DATE(yyyy-MM-dd'T\\'HH:mm:ss\\'Z'), return escaped",
                "Date with T and Z, one quote escaped #DATE(yyyy-MM-dd\\'T\\'HH:mm:ss\\'Z\\'), return escaped");
        dateCases.put("Date with T and Z, one quote escaped #DATE(yyyy-MM-dd\\'T'HH:mm:ss'Z\\'), return escaped",
                "Date with T and Z, one quote escaped #DATE(yyyy-MM-dd\\'T\\'HH:mm:ss\\'Z\\'), return escaped");
        dateCases.put("Date with T and Z, other format #DATE(-1d+3h, MM/dd/yyyy HH:mm:ss'Z'), return escaped",
                "Date with T and Z, other format #DATE(-1d+3h, MM/dd/yyyy HH:mm:ss\\'Z\\'), return escaped");
    }

    @Test
    public void escapeQuoteDateMacros() {
        for (Map.Entry<String, String> dateCase : dateCases.entrySet()) {
            String result = dateMacrosEscapeQuoteExecutor.execute(dateCase.getKey());
            Assertions.assertEquals(dateCase.getValue(), result);
        }
    }


}
