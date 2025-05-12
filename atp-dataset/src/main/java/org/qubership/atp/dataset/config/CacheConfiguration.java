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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.qubership.atp.dataset.constants.CacheEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientConnectionStrategyConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableCaching
@Slf4j
public class CacheConfiguration {

    public static final String CACHE_CLIENT_NAME = "ATP_DATASETS_";
    public static final String CACHE_SERVER_NAME = "ATP-DATASETS-SERVER";
    public static final UUID CACHE_ID = UUID.randomUUID();

    @Value("${spring.cache.hazelcast.cluster-name}")
    private String cacheClusterName;
    @Value("${spring.cache.hazelcast.server.address}")
    private String hazelcastServerAddress;
    @Value("${spring.cache.hazelcast.server.enable}")
    private boolean hazelcastServerEnable;
    @Value("${spring.cache.hazelcast.server.port}")
    private int hazelcastServerPort;
    @Value("${server.port}")
    private String serverPort;


    /**
     * Generates hazelcast instance client.
     *
     * @return instance
     */
    @Bean
    @ConditionalOnProperty(name = "spring.cache.hazelcast.client.enable", havingValue = "true")
    public CacheManager hazelcastCacheManager() {
            ClientConfig clientConfig = new ClientConfig();
            clientConfig.setInstanceName(CACHE_CLIENT_NAME + "on_port_" + serverPort + "_with_id_" + CACHE_ID);
            clientConfig.setClusterName(cacheClusterName);
            clientConfig.getNetworkConfig().addAddress(hazelcastServerAddress + ":" + hazelcastServerPort);
            clientConfig.getConnectionStrategyConfig()
                    .setReconnectMode(ClientConnectionStrategyConfig.ReconnectMode.ASYNC);
            if (hazelcastServerEnable) {
                startCacheServer();
            }
                log.debug("Connect to HAZELCAST as client");
                HazelcastInstance hzInstanceClient = HazelcastClient.newHazelcastClient(clientConfig);
                for (CacheEnum key : CacheEnum.values()) {
                    String name = key.getKey();
                    try {
                        log.debug("Try to create config for map {}", name);
                        hzInstanceClient.getConfig().addMapConfig(
                                new MapConfig(name).setTimeToLiveSeconds(key.getTimeToLiveSec()));
                    } catch (Exception failedCreate) {
                        log.warn("Map {} already created. Not possible to change map config: {}", name, failedCreate);
                    }
                }
                return new HazelcastCacheManager(hzInstanceClient);
    }

    /**
     * Start local hazelcast instance client.
     */
    private void startCacheServer() {
            log.info("Get or start cache config on address " + hazelcastServerAddress + ":" + hazelcastServerPort);
            Config config = new Config(CACHE_SERVER_NAME);
            NetworkConfig network = config.getNetworkConfig()
                    .setPort(hazelcastServerPort)
                    .setPortCount(1)
                    .setPortAutoIncrement(false)
                    .setReuseAddress(true);
            network.getJoin().getMulticastConfig().setEnabled(true);
            for (CacheEnum key : CacheEnum.values()) {
                config.addMapConfig(new MapConfig(key.getKey()).setTimeToLiveSeconds(key.getTimeToLiveSec()));
            }
            config.setClusterName(cacheClusterName);
            try {
                Hazelcast.getOrCreateHazelcastInstance(config);
            } catch (Exception e) {
                log.warn("HazelCast server already started: {}", e.getMessage());
            }
    }

    /**
     * Caffeine cache manager.
     * @return Caffeine cache manager.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.cache.hazelcast.client.enable", havingValue = "false")
    public CacheManager caffeineCacheManager() {
        log.info("Create CAFFEINE cache manager");
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        List<CaffeineCache> caches = new ArrayList<>();

        for (CacheEnum key : CacheEnum.values()) {
            caches.add(new CaffeineCache(key.getKey(),
                    Caffeine.newBuilder()
                            .expireAfterWrite(key.getTimeToLiveSec(), TimeUnit.SECONDS)
                            .recordStats()
                            .maximumSize(100).build(),
                    true));
        }
        cacheManager.setCaches(caches);
        return cacheManager;
    }
}
