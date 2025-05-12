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

package org.qubership.atp.dataset.ui;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.qubership.atp.dataset.config.IntegrationTestConfiguration;
import org.qubership.atp.dataset.config.MockJaversCommitEntityServiceConfiguration;
import org.qubership.atp.dataset.service.AbstractTest;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {IntegrationTestConfiguration.class, MockJaversCommitEntityServiceConfiguration.class})
public class AbstractIntegrationCase extends AbstractTest {

    protected static String serverBaseUrl;
    protected String httpsPort;
    protected String httpPort;

    @Value("${service.https.enabled}")
    private String httpsEnabled;

    @Autowired
    public Properties properties;
    @Autowired
    private Environment serverEnvironment;

    @BeforeEach
    public void before() throws UnknownHostException {
        if ((serverBaseUrl = properties.getProperty("server.url")) == null) {
            httpsPort = serverEnvironment.getProperty("https.port");
            httpPort = serverEnvironment.getProperty("local.server.port");
            String hostName = System.getProperty("COMPUTERNAME") != null
                    ? System.getProperty("COMPUTERNAME") :
                    InetAddress.getLocalHost().getHostAddress();
            serverBaseUrl = Boolean.parseBoolean(httpsEnabled)
                    ? "https://" + hostName + ":" + httpsPort
                    : "http://" + hostName + ":" + httpPort;
        }
    }
}
