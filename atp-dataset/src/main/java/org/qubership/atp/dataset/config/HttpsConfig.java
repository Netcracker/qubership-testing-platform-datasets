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

package org.qubership.atp.dataset.config;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(value = "service.https.enabled")
@Configuration
public class HttpsConfig {
    private static final Logger LOG = LoggerFactory.getLogger(HttpsConfig.class);
    @Value("${https.port}")
    private int httpsPort;
    @Value("${server.http.interface}")
    private String httpInterface;
    @Value("${server.ssl.key-store}")
    private String keyStoreLocation;
    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;
    @Value("${server.ssl.keyStoreType}")
    private String keyStoreType;

    /**
     * Create http port listener.
     */
    @Bean
    public WebServerFactoryCustomizer<UndertowServletWebServerFactory> containerCustomizer() {
        return factory -> factory.getBuilderCustomizers().add(builder -> {
            builder.addHttpsListener(httpsPort, httpInterface, sslContext());
        });
    }

    /**
     * Load KeyStore from file.
     */
    @Bean
    public KeyStore loadKeyStore() {
        try (InputStream stream = Files.newInputStream(Paths.get(keyStoreLocation))) {
            KeyStore loadedKeystore = KeyStore.getInstance(keyStoreType);
            loadedKeystore.load(stream, keyStorePassword.toCharArray());

            return loadedKeystore;
        } catch (Exception e) {
            LOG.error("Failed to load keystore.", e);
        }
        return null;
    }

    /**
     * Construct SSLContext.
     */
    @Bean
    public SSLContext sslContext() {
        KeyStore keyStore = loadKeyStore();
        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
            KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

            TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, null);

            return sslContext;
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct SSLContext.", e);
        }
    }
}
