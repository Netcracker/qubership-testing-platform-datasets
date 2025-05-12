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

package org.qubership.atp.dataset.db.migration;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.qubership.atp.dataset.db.migration.classloader.ParentLastClassloader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MigrationRunner {

    @Autowired
    private LiquibaseFactory lbFactory;

    @Value("${jdbc_type:pg}")
    private String jdbcType;

    @Value("${lb.libs.path:../atp-dataset-migration/src/main/scripts}")
    private String sourcePath;

    @Value("${drop.database.for.tests:false}")
    private boolean isTests;

    @Value("${dataset.migration.module.launch.enabled:true}")
    private boolean migrationModuleLaunchEnabled;

    /**
     * For running Migration liquibase scripts in Migration module and backs Dataset module.
     */
    public void runMigration() {
        if (migrationModuleLaunchEnabled) {
            log.info("Migration module launch is Enabled. DB update has been launched.");
            try {
                Liquibase lb = lbFactory.create("install.xml");
                if (isTests) {
                    log.info("drop database because of flag (drop.database.for.tests)");
                    lb.dropAll();
                }
                lb.update(new Contexts(), new LabelExpression());
                lb.getDatabase().getConnection().close();
                Path path = Paths.get("atp-dataset-migration/target/scripts");
                URL[] urls = new URL[]{path.toUri().toURL()};
                URLClassLoader urlClassLoader = new URLClassLoader(urls);
                ParentLastClassloader customClassLoader = new ParentLastClassloader(
                        urlClassLoader, sourcePath, jdbcType);
                Map<String, Object> scopeValues = new HashMap<>();
                scopeValues.put(Scope.Attr.classLoader.name(), customClassLoader);
                Scope.child(scopeValues, () -> {
                    Liquibase liquibase = lbFactory.create("update.xml",
                            new ClassLoaderResourceAccessor());
                    liquibase.update(new Contexts(), new LabelExpression());
                    liquibase.getDatabase().getConnection().close();
                });
                log.info("DB update completed.");
            } catch (Throwable e) {
                log.error("Migration module launch scripts was failed, Uncaught exception", e);
            }
        } else {
            log.info("Migration module launch is Disabled.");
        }
    }
}
