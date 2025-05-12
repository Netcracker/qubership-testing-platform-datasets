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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bson.Document;
import org.bson.UuidRepresentation;
import org.qubership.atp.dataset.db.GridFsRepository;
import org.qubership.atp.dataset.db.GridFsRepositoryImpl;
import org.qubership.atp.dataset.model.impl.file.FileData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Indexes;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsConnectionPoolListener;

@Configuration
public class GridFsConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(GridFsConfiguration.class);

    @Value("${gridfs.database:#{null}}")
    private String database;
    @Value("${gridfs.host:#{null}}")
    private String host;
    @Value("${gridfs.port:#{null}}")
    private String port;
    @Value("${gridfs.user:#{null}}")
    private String user;
    @Value("${gridfs.password:#{null}}")
    private String password;

    /**
     * Provides stub repository if gridFs properties are not provided. Used in cases when file
     * parameters are not needed.
     */
    @Bean
    public GridFsRepository provideRepo(MeterRegistry meterRegistry) {
        try {
            GridFSBucket gridFsBucket = provideGridFileSystemBuckets(meterRegistry);
            return new GridFsRepositoryImpl(gridFsBucket);
        } catch (Exception e) {
            String message = "Can not initialize grid fs module, will use mock instead";
            LOG.warn(message, e);
            UnsupportedOperationException exception = new UnsupportedOperationException(message, e);
            return new GridFsRepository() {
                @Override
                public void remove(UUID attachmentUuid) {
                }

                @Override
                public void save(FileData fileData, InputStream fileInputStream) {
                    throw exception;
                }

                @Override
                public Optional<GridFSFile> getGridFsFile(UUID attachmentUuid) {
                    return Optional.empty();
                }

                @Override
                public Optional<InputStream> get(UUID parameterUuid) {
                    return Optional.empty();
                }

                @Override
                public Map<UUID, Optional<InputStream>> getAll(List<UUID> parametersUuids) {
                    return new HashMap<>();
                }

                @Override
                public Optional<FileData> getFileInfo(UUID parameterUuid) {
                    return Optional.empty();
                }

                @Override
                public void onDeleteCascade(List<UUID> parameters) {
                }

                @Override
                public void dropLocalThreadCache() {
                }
            };
        }
    }

    /**
     * Provides {@link GridFSBucket} for getting files from database.
     *
     * @return GridFSBucket by specified parameters.
     */
    private GridFSBucket provideGridFileSystemBuckets(MeterRegistry meterRegistry) {
        String mongoClientUri = "mongodb://" + user + ":" + password
                + "@" + host + ":" + port + "/?authSource"
                + "=" + database;
        ConnectionString connectionString = new ConnectionString(mongoClientUri);
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .uuidRepresentation(UuidRepresentation.JAVA_LEGACY)
                .addCommandListener(new MongoMetricsCommandListener(meterRegistry))
                .applyConnectionString(connectionString)
                .applyToConnectionPoolSettings(builder ->
                        builder.addConnectionPoolListener(new MongoMetricsConnectionPoolListener(meterRegistry)))
                .build();
        MongoClient mongo = MongoClients.create(mongoClientSettings);
        MongoDatabase db = mongo.getDatabase(database);
        GridFSBucket gridFsBucket = GridFSBuckets.create(db);
        MongoCollection<Document> filesCollection = db.getCollection("fs.files");
        filesCollection.createIndex(Indexes.descending("metadata.attachmentUuid"));
        return gridFsBucket;
    }
}
