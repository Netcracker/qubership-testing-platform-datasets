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

package org.qubership.atp.dataset.migration;

import java.io.IOException;
import java.util.Properties;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.qubership.atp.dataset.migration.config.MigrationConfig;
import org.qubership.atp.dataset.migration.model.ImportResources;
import org.qubership.atp.dataset.migration.model.Settings;
import org.qubership.atp.dataset.migration.repo.DsServicesFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@Import(MigrationConfig.class)
public class DataSetImporter implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSetImporter.class);
    private Settings settings;
    private DsServicesFacade dsServicesFacade;

    @Autowired
    public DataSetImporter(Properties properties, DsServicesFacade dsServicesFacade) {
        settings = Settings.getSettings(properties);
        this.dsServicesFacade = dsServicesFacade;
    }

    public DataSetImporter(Settings settings, DsServicesFacade dsServicesFacade) {
        this.settings = settings;
        this.dsServicesFacade = dsServicesFacade;
    }

    /**
     * main entry point.
     *
     * @param args list of args
     * @throws Exception some error occured
     */
    public static void main(String[] args) throws Exception {
        LOGGER.info("Importer version: " + DataSetImporter.class.getPackage().getImplementationVersion());
        try {
            SpringApplication.run(DataSetImporter.class, args);
        } catch (Exception e) {
            LOGGER.error("Failed to import excel files: " + e.getMessage(), e);
        }
    }

    @Override
    public void run(String... args) throws IOException, InvalidFormatException {
        String vaName = settings.getVisibilityArea();
        String dslName = settings.getDslName();
        String excelFolder = settings.getExcelFolder();
        String parentFileName = settings.getParentExcelDatasetLocation();
        String groupDataSetName = settings.getDefaultGroupDataSetName();
        Boolean rootAttrFlag = settings.getRootAttrFlag();
        if (parentFileName != null && !parentFileName.isEmpty()) {
            ParentDsImporter.process(ImportResources.create(
                    dsServicesFacade, vaName, excelFolder, parentFileName, groupDataSetName), rootAttrFlag);
        } else {
            LOGGER.warn("PARENT FILE PROCESSING SKIPPED, because parent file is not specified");
        }
        String childFileName = settings.getChildExcelDatasetLocation();
        if (childFileName != null && !childFileName.isEmpty()) {
            ChildDsImporter.process(ImportResources.create(dsServicesFacade, vaName, excelFolder,
                    settings.getChildExcelDatasetLocation(),
                    groupDataSetName), dslName, rootAttrFlag);
        } else {
            LOGGER.warn("CHILD FILE PROCESSING SKIPPED, because child file is not specified");
        }
    }
}
