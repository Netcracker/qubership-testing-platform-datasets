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

package org.qubership.atp.dataset.ui.api;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

public class RestClientConfig extends ClientConfig {

    public RestClientConfig() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        super();
        super.property(ClientProperties.READ_TIMEOUT, 300000);
        super.property(ClientProperties.CONNECT_TIMEOUT, 10000);
        //super.property(ClientProperties.PROXY_URI, "http://localhost:8888");

        SSLContext context = SSLContexts.custom()
                .loadTrustMaterial(TrustSelfSignedStrategy.INSTANCE)
                .build();

        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", new SSLConnectionSocketFactory(context, NoopHostnameVerifier.INSTANCE))
                .build();

        //connectionManager.setDefaultConnectionConfig(ConnectionConfig.custom().);
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);

        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(50);
        //connectionManager.setMaxPerRoute(new HttpRoute(new HttpHost("localhost")), 40);
        super.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);
        ApacheConnectorProvider connectorProvider = new ApacheConnectorProvider();
        super.connectorProvider(connectorProvider);
    }
}
