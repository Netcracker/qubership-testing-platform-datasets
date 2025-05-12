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

package org.qubership.atp.dataset.migration.model;

import java.util.Objects;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Settings {

    private static final Logger LOG = LoggerFactory.getLogger(Settings.class);
    private String excelFolder;
    private String parentFileName;
    private String childFileName;
    private String dslName;
    private String vaName;
    private String defaultGroupDataSetName;
    private String rootAttr;

    public Settings() {
    }

    /**
     * Settings of an import.
     *
     * @param excelFolder             relative or absolute path to the folder with xlsx files.
     * @param parentFileName          name of a xlsx file with "parent" data sets.
     * @param childFileName           name of a xlsx file with "child" data sets.
     * @param vaName                  name of a visibility area, to migrate data into.
     * @param dslName                 dame of a data set list, to migrate "child" data sets into.
     * @param defaultGroupDataSetName name of a data set to be used for group "parent" data sets.
     * @param rootAttr                if TRUE, all parameters under "default" group will be migrated
     *                                into root of data set list: no "default" group will be
     *                                created. See ATPII-3798.
     */
    public Settings(@Nonnull String excelFolder,
                    @Nullable String parentFileName,
                    @Nullable String childFileName,
                    @Nonnull String vaName,
                    @Nonnull String dslName,
                    @Nonnull String defaultGroupDataSetName,
                    @Nullable String rootAttr) {
        this.excelFolder = excelFolder;
        this.parentFileName = parentFileName;
        this.childFileName = childFileName;
        this.dslName = dslName;
        this.vaName = vaName;
        this.defaultGroupDataSetName = defaultGroupDataSetName;
        this.rootAttr = rootAttr;
    }

    /**
     * Parses settings from supplied properties.
     *
     * @throws IllegalArgumentException if some mandatory argument is not specified.
     */
    public static Settings getSettings(Properties properties) throws IllegalArgumentException {
        Settings settings = new Settings();
        try {
            settings.excelFolder = getMandatorySetting(properties, "excel_folder");
            if (!settings.excelFolder.endsWith("/")) {
                settings.excelFolder += "/";
            }
            settings.parentFileName = getOptionalSetting(properties, "parent_file_name");
            settings.childFileName = getOptionalSetting(properties, "child_file");
            settings.vaName = getMandatorySetting(properties, "va_name");
            settings.dslName = getMandatorySetting(properties, "dsl_name");
            settings.defaultGroupDataSetName =
                    getMandatorySetting(properties, "default_group_dataset_name");
            settings.rootAttr = getOptionalSetting(properties, "root_attr");
        } catch (IllegalArgumentException e) {
            printHelp();
            throw new IllegalArgumentException("One of arguments in not specified: " + e.getMessage(), e);
        }
        return settings;
    }

    private static void printHelp() {
        LOG.error("ImporterDT "
                + " -Dexcel_folder=folder_with_all_excel_files"
                + " -Dparent_file_name=PARENT.xlsx "
                + " -Dchild_file_name=CHILD.xlsx "
                + " -Dva_name=visibility_name_to_store_data "
                + " -Ddsl_name=data_set_list_name_to_store_data_sets_from_child_excel"
                + " -Ddefault_group_dataset_name=group_data_set_default_name_for_groups_parameters");
    }

    private static String getMandatorySetting(Properties properties, String propertyName) {
        String value = getOptionalSetting(properties, propertyName);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Mandatory property is not specified: " + propertyName);
        }
        return value;
    }

    private static String getOptionalSetting(Properties properties, String propertyName) {
        return properties.getProperty(propertyName);
    }

    public String getExcelFolder() {
        return excelFolder;
    }

    public String getParentExcelDatasetLocation() {
        return getExcelDatasetLocation(parentFileName);
    }

    public String getChildExcelDatasetLocation() {
        return getExcelDatasetLocation(childFileName);
    }

    private String getExcelDatasetLocation(String fileName) {
        if (Objects.isNull(fileName)) {
            return null;
        }
        String trimmedFileName = fileName.trim();
        if (trimmedFileName.isEmpty()) {
            return null;
        }
        return excelFolder + fileName;
    }

    public String getVisibilityArea() {
        return vaName;
    }

    public String getDslName() {
        return dslName;
    }

    public String getDefaultGroupDataSetName() {
        return defaultGroupDataSetName;
    }

    public Boolean getRootAttrFlag() {
        return Boolean.parseBoolean(rootAttr);
    }
}
