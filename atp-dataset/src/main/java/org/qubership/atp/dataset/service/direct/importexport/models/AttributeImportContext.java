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

import static org.qubership.atp.dataset.service.direct.importexport.utils.StreamUtils.toEntityMap;
import static org.qubership.atp.dataset.service.direct.importexport.utils.StreamUtils.toNameIdEntityMap;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.logging.log4j.util.Strings;
import org.qubership.atp.dataset.db.jpa.entities.ListValueEntity;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.service.direct.importexport.utils.ImportUtils;
import org.qubership.atp.dataset.service.direct.importexport.utils.StreamUtils;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.AttributeKey;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class AttributeImportContext {

    private boolean isJavers;
    private UUID targetDslId;

    // datasets
    private Map<UUID, DataSet> datasetsMap = new HashMap<>();
    private Map<String, UUID> datasetsNameIdMap = new HashMap<>();
    private Map<String, DataSet> refDatasetsMap = new HashMap<>();
    private Map<String, UUID> refDatasetsNameIdMap = new HashMap<>();
    private Map<String, Integer> datasetsCellIndexMap = new LinkedHashMap<>();
    private Map<String, Set<UUID>> refDslDatasetIdsMap = new HashMap<>();

    // attributes
    private Map<UUID, Attribute> attributesIdMap = new HashMap<>();
    private Map<String, Attribute> attributesNameMap = new HashMap<>();

    // parameters
    private Map<String, Parameter> attributeParametersMap = new HashMap<>();
    private Map<String, String> targetDslOverlaps = new HashMap<>();

    /**
     * Set datasets to context.
     */
    public void setOverlaps(Collection<AttributeKey> overlaps) {
        if (!isEmpty(overlaps)) {
            try {
                AtomicReference<AttributeKey> attributeKeyTemp = new AtomicReference<>();
                this.targetDslOverlaps = overlaps.stream()
                        .collect(Collectors.toMap(attributeKey -> ImportUtils.getOverlapKey(
                                        attributeKey, this),
                                attributeKey -> getValueOverlaps(attributeKeyTemp, attributeKey),
                                (firstValueFound, anyNextValue) -> {
                                    log.warn("Overlaps prepare is duplicate map key: '{}', for values in map '{}' and "
                                                    + "other '{}'",
                                            ImportUtils.getOverlapKey(attributeKeyTemp.get(), this),
                                            firstValueFound, anyNextValue);

                                    return firstValueFound;
                                })
                        );
            } catch (RuntimeException e) {
                log.warn("Error set Overlaps message: '{}', Cause: '{}'", e.getMessage(), e.getCause());
            }
        }
    }

    private String getValueOverlaps(AtomicReference<AttributeKey> attributeKeyTemp, AttributeKey attributeKey) {
        attributeKeyTemp.set(attributeKey);
        String valueTextParameter = Strings.EMPTY;

        org.qubership.atp.dataset.service.jpa.delegates.Parameter parameter = attributeKey.getParameter();
        if (Objects.nonNull(parameter)) {
            String textValue = parameter.getStringValue();
            if (Strings.isNotBlank(textValue)) {
                return textValue;
            }
            DataSet dataset = parameter.getDataSetReferenceValue();
            if (Objects.nonNull(dataset)
                    && Strings.isNotBlank(dataset.getName())) {
                return dataset.getName();
            }
            ListValueEntity listValueEnt = parameter.getListValue();
            if (Objects.nonNull(listValueEnt)
                    && Strings.isNotBlank(listValueEnt.getText())) {
                return listValueEnt.getText();
            }
        }

        return valueTextParameter;
    }

    /**
     * Set datasets to context.
     */
    public void setDatasets(Collection<DataSet> datasets) {
        if (!isEmpty(datasets)) {
            this.datasetsNameIdMap = toNameIdEntityMap(datasets, DataSet::getName, DataSet::getId);
            this.datasetsMap = toEntityMap(datasets, DataSet::getId);
        }
    }

    public void setDataset(DataSet dataset) {
        this.datasetsNameIdMap.put(dataset.getName(), dataset.getId());
        this.datasetsMap.put(dataset.getId(), dataset);
    }

    /**
     * Set referenced datasets to context.
     */
    public void setRefDatasets(Collection<DataSet> refDatasets) {
        if (!isEmpty(refDatasets)) {
            this.refDatasetsNameIdMap = toNameIdEntityMap(refDatasets, ImportUtils::getDatasetKey, DataSet::getId);
            this.refDatasetsMap = toEntityMap(refDatasets, ImportUtils::getDatasetKeyPathName);
        }
    }

    /**
     * Set attributes to context.
     */
    public void setAttributes(String prefix, Collection<Attribute> attributes) {
        if (!isEmpty(attributes)) {
            this.attributesNameMap.putAll(StreamUtils.toNameEntityMap(attributes,
                    attribute -> prefix + ImportUtils.DSL_ATTRIBUTE_REF_DELIMITER + attribute.getName()));
            this.attributesIdMap.putAll(toEntityMap(attributes, Attribute::getId));
        }
    }

    /**
     * Set attributes to context.
     */
    public void setAttributes(Collection<Attribute> attributes) {
        if (!isEmpty(attributes)) {
            this.attributesNameMap.putAll(StreamUtils.toNameEntityMap(attributes, Attribute::getName));
            this.attributesIdMap.putAll(toEntityMap(attributes, Attribute::getId));
        }
    }

    /**
     * Set attributes parameters to context.
     */
    public void setAttributeParameters(Collection<Parameter> parameters) {
        if (!isEmpty(parameters)) {
            this.attributeParametersMap =
                    toEntityMap(parameters, ImportUtils::getDsParameterKey);
        }
    }

    public Parameter getRefParameter(String datasetParameterKey) {
        return this.attributeParametersMap.get(datasetParameterKey);
    }

    public void clearParametersContext() {
        this.attributeParametersMap.clear();
    }

    public DataSet getDataset(UUID datasetId) {
        return datasetsMap.get(datasetId);
    }

    public UUID getDatasetId(String datasetName) {
        return datasetsNameIdMap.get(datasetName);
    }

    public boolean containsRefDataset(String datasetName) {
        return refDatasetsMap.containsKey(datasetName);
    }

    public UUID getRefDatasetId(String datasetKey) {
        return refDatasetsNameIdMap.get(datasetKey);
    }

    public UUID getAttributeId(String attributeKey) {
        return attributesNameMap.get(attributeKey).getId();
    }

    public Attribute getAttribute(String attributeKey) {
        return attributesNameMap.get(attributeKey);
    }

    public Attribute getAttribute(UUID id) {
        return attributesIdMap.get(id);
    }

    /**
     * Get attribute DSL reference.
     *
     * @param attributeKey attribute key
     * @return attribute DSL reference
     */
    public String getAttributeDslRef(String attributeKey) {
        final Attribute parentAttribute = getAttribute(attributeKey);
        final DataSetList parentDatasetListRef = parentAttribute.getTypeDataSetList();

        return parentDatasetListRef.getName();
    }

    public boolean isOverlapExist(String overlapKey) {
        return targetDslOverlaps.containsKey(overlapKey);
    }
}
