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

public class TestUtils {
    public static boolean onlyOneIsNotNull(Object... args) {
        boolean result = false;
        for (Object argument : args) {
            if (argument != null) {
                if (result) {
                    result = false;
                    break;
                } else {
                    result = true;
                }
            }
        }
        return result;
    }

    public static void turnPactMetricsOff() {
        // Turn OFF sending of anonymous Pact metrics due to inconsistent versions of httpclient5
        // used in Pact 4.6.15 library (httpclient5:5.3.x) and in the entire project (httpclient5:5.4.4)
        // Error doesn't fail tests but spams logs with such errors:
        //     Exception in thread "Thread-5"
        //         java.lang.NoClassDefFoundError: org/apache/hc/client5/http/impl/compat/ClassicToAsyncAdaptor
        //     at org.apache.hc.client5.http.fluent.Request.execute(Request.java:206)
        //     at au.com.dius.pact.core.support.Metrics.sendMetrics$lambda$2(Metrics.kt:128)
        //     at java.base/java.lang.Thread.run(Thread.java:1583)
        //     Caused by: java.lang.ClassNotFoundException: org.apache.hc.client5.http.impl.compat.ClassicToAsyncAdaptor
        //     at java.base/jdk.internal.loader.BuiltinClassLoader.loadClass(BuiltinClassLoader.java:641)
        //     at java.base/jdk.internal.loader.ClassLoaders$AppClassLoader.loadClass(ClassLoaders.java:188)
        //     at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:526)
        System.setProperty("pact_do_not_track", "true");
    }
}
