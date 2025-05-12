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

package org.qubership.atp.dataset.service.direct.importexport.models;

import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.service.direct.importexport.exceptions.ImportFailedException;
import org.qubership.atp.dataset.service.direct.importexport.utils.StreamUtils;

import joptsimple.internal.Strings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class AttributeImportModel {
    private UUID id;
    private String name;
    private String key;
    private AttributeType type;
    private String rootDatasetListName;
    private String datasetListReference;
    private List<UUID> path = new ArrayList<>();
    private List<DatasetParameterValue> datasetParameterValues = new ArrayList<>();
    @ToString.Exclude
    private AttributeImportModel parent;
    private List<AttributeImportModel> children = new ArrayList<>();

    /**
     * AttributeImportModel constructor.
     */
    public AttributeImportModel(UUID id, String name, String key, AttributeType type) {
        this.id = id;
        this.name = name;
        this.key = key;
        this.type = type;
    }

    /**
     * Gets dataset parameter value by dataset name.
     *
     * @param datasetName dataset name
     * @return dataset parameter value
     */
    public DatasetParameterValue getDatasetParameterValue(String datasetName) {
        return datasetParameterValues.stream()
                .filter(datasetParameterValue -> datasetParameterValue.getDatasetName().equals(datasetName))
                .findAny()
                .orElseThrow(() -> {
                    String errorMessage = "Failed to find dataset parameter value by dataset name: " + datasetName;
                    log.debug(errorMessage);
                    return new ImportFailedException(errorMessage);
                });
    }

    public String getDatasetParameterValuesDslReference() {
        return datasetParameterValues.stream()
                .map(DatasetParameterValue::getDatasetListReference)
                .findAny()
                .orElse(Strings.EMPTY);
    }

    /**
     * Add atttribute path.
     *
     * @param parentModel parent model with its path
     */
    public void addPath(AttributeImportModel parentModel) {
        final List<UUID> parentAttributePath = parentModel.getPath();
        if (!isEmpty(parentAttributePath)) {
            this.path.addAll(parentAttributePath);
        }

        this.path.add(parentModel.getId());
    }

    public void addChildren(AttributeImportModel child) {
        this.children.add(child);
    }

    /**
     * Fill parent dataset list references.
     *
     * @param parentModel parent import model
     */
    public void fillParentDslReferences(AttributeImportModel parentModel) {
        Map<String, DatasetParameterValue> parentDatasetParametersMap =
                StreamUtils.toEntityMap(parentModel.getDatasetParameterValues(), DatasetParameterValue::getDatasetName);

        datasetParameterValues.forEach(childParentDatasetParameterValue -> {
            DatasetParameterValue parentDatasetParameterValue =
                    parentDatasetParametersMap.get(childParentDatasetParameterValue.getDatasetName());

            String parentDatasetListReference = parentDatasetParameterValue.getDatasetListReference();
            childParentDatasetParameterValue.setDatasetListReference(parentDatasetListReference);

            String parentDatasetReference = parentDatasetParameterValue.getDatasetReference();
            childParentDatasetParameterValue.setDatasetReference(parentDatasetReference);
        });
    }
}
