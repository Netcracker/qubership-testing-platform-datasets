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

package org.qubership.atp.dataset.service.direct.impl;

import static java.util.stream.Collectors.toList;
import static org.qubership.atp.dataset.model.enums.DetailedComparisonStatus.EQUAL;
import static org.qubership.atp.dataset.model.enums.DetailedComparisonStatus.INCOMPATIBLE_TYPES;
import static org.qubership.atp.dataset.model.enums.DetailedComparisonStatus.NOT_EQUAL;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.db.jpa.entities.ListValueEntity;
import org.qubership.atp.dataset.exception.attribute.AttributeDslCopyException;
import org.qubership.atp.dataset.model.api.DetailedComparisonDsRequest;
import org.qubership.atp.dataset.model.api.DetailedComparisonDsResponse;
import org.qubership.atp.dataset.model.enums.CompareStatus;
import org.qubership.atp.dataset.model.enums.DetailedComparisonStatus;
import org.qubership.atp.dataset.model.impl.ComparedAttribute;
import org.qubership.atp.dataset.model.impl.file.FileData;
import org.qubership.atp.dataset.service.direct.CompareService;
import org.qubership.atp.dataset.service.direct.EncryptionService;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.AttributeKey;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompareDatasetServiceImpl implements CompareService {

    private final ModelsProvider modelsProvider;
    private final EncryptionService encryptionService;

    @Override
    public CompareStatus compare(UUID leftDsCaseId, UUID rightDsCaseId) {
        log.info("Started compare dataset {} with {}", leftDsCaseId, rightDsCaseId);
        if (leftDsCaseId.equals(rightDsCaseId)) {
            return CompareStatus.OK;
        }
        List<Parameter> leftParameters = modelsProvider.getSortedParameterByDataSetId(leftDsCaseId);
        List<Parameter> rightParameters = modelsProvider.getSortedParameterByDataSetId(rightDsCaseId);

        List<Parameter> leftOverlapParameters = modelsProvider.getSortedOverlapParametersByDataSetId(leftDsCaseId);
        List<Parameter> rightOverlapParameters = modelsProvider.getSortedOverlapParametersByDataSetId(rightDsCaseId);

        int countAttrLeft = modelsProvider.countAttributes(leftDsCaseId);
        int countAttrRight = modelsProvider.countAttributes(rightDsCaseId);
        if (countAttrLeft != countAttrRight) {
            return CompareStatus.WARNING;
        }

        if (rightParameters.size() != leftParameters.size()) {
            return CompareStatus.WARNING;
        }

        if (rightOverlapParameters.size() != leftOverlapParameters.size()) {
            return CompareStatus.WARNING;
        }

        for (int i = 0; i < leftParameters.size(); i++) {
            Parameter leftParameter = leftParameters.get(i);
            Parameter rightParameter = rightParameters.get(i);

            boolean hasDiff = hasParameterDiff(leftParameter, rightParameter);
            if (hasDiff) {
                return CompareStatus.WARNING;
            }
        }

        for (int i = 0; i < leftOverlapParameters.size(); i++) {
            Parameter leftOverlapParameter = leftOverlapParameters.get(i);
            Parameter rightOverlapParameter = rightOverlapParameters.get(i);

            boolean hasDiff = hasParameterDiff(leftOverlapParameter, rightOverlapParameter);
            if (hasDiff) {
                return CompareStatus.WARNING;
            }
        }

        log.info("Finished compare dataset {} with {}", leftDsCaseId, rightDsCaseId);
        return CompareStatus.OK;
    }

    private boolean hasParameterDiff(Parameter leftParameter, Parameter rightParameter) {
        Attribute leftAttribute = leftParameter.getAttribute();
        Attribute rightAttribute = rightParameter.getAttribute();

        if (leftParameter.isOverlap()) {
            leftAttribute = leftParameter.getAttributeKey().getAttribute();
            rightAttribute = leftParameter.getAttributeKey().getAttribute();
        }
        AttributeTypeName attrTypeLeft = leftAttribute.getAttributeType();
        AttributeTypeName attrTypeRight = rightAttribute.getAttributeType();

        if (!leftAttribute.getName().equals(rightAttribute.getName())) {
            return true;
        }

        if (attrTypeLeft != attrTypeRight) {
            return true;
        }

        switch (attrTypeLeft) {
            case CHANGE:
            case TEXT:
                return hasTextDiff(leftParameter, rightParameter);
            case ENCRYPTED:
                return hasEncryptDiff(leftParameter, rightParameter);
            case DSL:
                return hasDslDiff(leftParameter, rightParameter);
            case LIST:
                return hasListDiff(leftParameter, rightParameter);
            case FILE:
                return hasFileDiff(leftParameter, rightParameter);
            default:
                throw new IllegalStateException("Unexpected value: " + attrTypeLeft);
        }
    }

    private DetailedComparisonStatus compareParameters(Parameter leftParameter, Parameter rightParameter) {
        log.debug("Compare parameters (leftParameterId: {}, rightParameterId: {})",
                leftParameter.getId(), rightParameter.getId());
        AttributeTypeName attrTypeLeft;
        AttributeTypeName attrTypeRight;

        if (leftParameter.isOverlap() && rightParameter.isOverlap()) {
            attrTypeLeft = leftParameter.getAttributeKey().getAttributeType();
            attrTypeRight = rightParameter.getAttributeKey().getAttributeType();
        } else {
            attrTypeLeft = leftParameter.getAttribute().getAttributeType();
            attrTypeRight = rightParameter.getAttribute().getAttributeType();
        }

        if (attrTypeLeft != attrTypeRight) {
            log.trace("INCOMPATIBLE TYPES (leftAttrType: {}, rightAttrType: {})", attrTypeLeft, attrTypeRight);
            return INCOMPATIBLE_TYPES;
        }
        log.trace("Compare parameters by attribute type (AttrType: {})", attrTypeLeft);
        switch (attrTypeLeft) {
            case CHANGE:
            case TEXT:
                return hasTextDiff(leftParameter, rightParameter) ? NOT_EQUAL : EQUAL;
            case ENCRYPTED:
                return hasEncryptDiff(leftParameter, rightParameter) ? NOT_EQUAL : EQUAL;
            case DSL:
                return hasDslDiff(leftParameter, rightParameter) ? NOT_EQUAL : EQUAL;
            case LIST:
                return hasListDiff(leftParameter, rightParameter) ? NOT_EQUAL : EQUAL;
            case FILE:
                return hasFileDiff(leftParameter, rightParameter) ? NOT_EQUAL : EQUAL;
            default:
                throw new IllegalStateException("Unexpected value: " + attrTypeLeft);
        }
    }

    private boolean hasTextDiff(Parameter leftParameter, Parameter rightParameter) {
        return !leftParameter.getStringValue().equals(rightParameter.getStringValue());
    }

    private boolean hasEncryptDiff(Parameter leftParameter, Parameter rightParameter) {
        String leftValue = leftParameter.getStringValue();
        String rightValue = rightParameter.getStringValue();

        if (Objects.isNull(leftValue) || Objects.isNull(rightValue)) {
            return !(Objects.isNull(leftValue) && Objects.isNull(rightValue));
        }
        try {
            return !encryptionService.decrypt(leftValue).equals(encryptionService.decrypt(rightValue));
        } catch (AtpDecryptException ex) {
            throw new RuntimeException("Failed to decrypt encrypted parameter", ex);
        }
    }

    private boolean hasFileDiff(Parameter leftParameter, Parameter rightParameter) {
        FileData leftFile = leftParameter.getFileDataIfExist();
        FileData rightFile = rightParameter.getFileDataIfExist();
        if (leftFile != null && rightFile != null) {
            return !leftFile.getFileName().equals(rightFile.getFileName());
        } else {
            return leftFile != null || rightFile != null;
        }
    }

    private boolean hasListDiff(Parameter leftParameter, Parameter rightParameter) {
        ListValueEntity leftList = leftParameter.getListValue();
        ListValueEntity rightList = rightParameter.getListValue();

        if (leftList == null && rightList == null) {
            return false;
        }

        if (leftList != null && rightList != null) {
            return !leftList.getText().equals(rightList.getText());
        } else {
            return true;
        }
    }

    private boolean hasDslDiff(Parameter leftParameter, Parameter rightParameter) {
        return !leftParameter.getDataSetReferenceId().equals(rightParameter.getDataSetReferenceId());
    }

    /**
     * Detailed comparison two datasets.
     */
    public DetailedComparisonDsResponse detailedComparison(DetailedComparisonDsRequest request) {
        log.debug("Detailed comparison (request: {})", request);
        UUID leftDsId = request.getLeftDatasetId();
        UUID rightDsId = request.getRightDatasetId();
        DataSet leftDs = modelsProvider.getDataSetById(leftDsId);
        DataSet rightDs = modelsProvider.getDataSetById(rightDsId);

        List<Parameter> parameters = leftDs.getParameters();
        parameters.addAll(rightDs.getParameters());
        // Group parameters by attribute name
        // The list of grouped parameters must not contain more than two entries

        Map<String, List<Parameter>> groupedParameters = parameters.stream()
                .filter(p -> {
                    Attribute attr = p.getAttribute();
                    return Objects.nonNull(attr) && !attr.getEntity().isAttributeKey();
                })
                .collect(Collectors.groupingBy(p -> p.getAttribute().getName(), HashMap::new, toList()));

        Map<String, List<Parameter>> groupedParametersOverlap = parameters.stream()
                .filter(p -> {
                    AttributeKey attr = p.getAttributeKey();
                    return Objects.nonNull(attr) && attr.getEntity().isAttributeKey();
                })
                .collect(Collectors.groupingBy(p ->
                                p.getAttributeKey().getPathNames() + "." + p.getAttributeKey().getAttribute().getName(),
                        HashMap::new, toList()));

        List<ComparedAttribute> comparedAttributes = compareAttributes(leftDsId, rightDsId, groupedParameters);
        comparedAttributes.addAll(compareAttributes(leftDsId, rightDsId, groupedParametersOverlap));

        // Add to the compared attributes the ones that were not included in the list
        // because the parameter was not present.
        // 1. Get the list of unused attributes for the left and right dataset
        List<Attribute> notUsedAttributes = modelsProvider.getNotUsedAttributesByDatasetId(leftDsId);
        notUsedAttributes.addAll(modelsProvider.getNotUsedAttributesByDatasetId(rightDsId));
        // 2. Group attributes by name
        Map<String, List<Attribute>> groupedNotUsedAttributes = notUsedAttributes.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Attribute::getName, HashMap::new, toList()));
        // 3. Add to the list of compared attributes
        comparedAttributes.addAll(compareUnusedAttributes(groupedNotUsedAttributes));

        // sort by attribute name
        comparedAttributes.sort(Comparator.comparing(ComparedAttribute::getAttributeName));

        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize());
        final int totalSize = comparedAttributes.size();
        final int start = Math.min((int) pageRequest.getOffset(), totalSize);
        final int end = Math.min((start + pageRequest.getPageSize()), totalSize);
        return new DetailedComparisonDsResponse(
                leftDsId,
                leftDs.getName(),
                leftDs.getDataSetList().getName(),
                rightDsId,
                rightDs.getName(),
                rightDs.getDataSetList().getName(),
                totalSize,
                comparedAttributes.subList(start, end)
        );
    }

    private List<ComparedAttribute> compareAttributes(UUID leftDsId, UUID rightDsId,
                                                      Map<String, List<Parameter>> groupedParameters) {
        List<ComparedAttribute> comparedAttributes = new ArrayList<>();
        for (Map.Entry<String, List<Parameter>> entry : groupedParameters.entrySet()) {
            String attributeName = entry.getKey();
            List<Parameter> value = entry.getValue();
            if (value.size() > 2) {
                log.error("Found more than two parameters related to attributes with same name "
                                + "(attributeName {}, leftDatasetId {}, rightDatasetId {})",
                        attributeName, leftDsId, rightDsId);
                continue;
            }
            if (value.size() == 2) {
                comparedAttributes.add(getComparedAttribute(value.get(0), value.get(1), attributeName));
            } else {
                ComparedAttribute comparedAttribute =
                        getComparedAttribute(leftDsId, rightDsId, value.get(0), attributeName);
                comparedAttributes.add(comparedAttribute);
            }
        }
        return comparedAttributes;
    }

    private List<ComparedAttribute> compareUnusedAttributes(Map<String, List<Attribute>> notUsedAttribute) {
        List<ComparedAttribute> comparedAttributes = new ArrayList<>();
        for (Map.Entry<String, List<Attribute>> entry : notUsedAttribute.entrySet()) {
            List<Attribute> attributes = entry.getValue();
            if (attributes.size() != 2) {
                // ignore attributes that are not used in only one dataset,
                // as they will be added when comparing parameters
                continue;
            }
            Attribute leftAttribute = attributes.get(0);
            Attribute rightAttribute = attributes.get(1);
            comparedAttributes.add(new ComparedAttribute(
                    leftAttribute.getId(),
                    null,
                    leftAttribute.getAttributeType(),
                    false,
                    rightAttribute.getId(),
                    null,
                    rightAttribute.getAttributeType(),
                    false,
                    leftAttribute.getName(),
                    // If types are different, they are incompatible
                    // otherwise they are equal
                    leftAttribute.getAttributeType().equals(rightAttribute.getAttributeType())
                            ? EQUAL : INCOMPATIBLE_TYPES
            ));
        }
        return comparedAttributes;
    }

    private ComparedAttribute getComparedAttribute(Parameter leftParameter,
                                                   Parameter rightParameter,
                                                   String attributeName) {
        log.debug("Create comparedAttribute for (leftParameterId: {}, rightParameterId: {}, attributeName: {})",
                leftParameter.getId(), rightParameter.getId(), attributeName);

        if (leftParameter.isOverlap() || rightParameter.isOverlap()) {

            AttributeKey leftAttr = leftParameter.getAttributeKey();
            AttributeKey rightAttr = rightParameter.getAttributeKey();

            return new ComparedAttribute(
                    leftAttr.getId(),
                    leftParameter.getParameterValueByTypeAsObject(),
                    leftAttr.getAttributeType(),
                    leftParameter.isOverlap(),
                    rightAttr.getId(),
                    rightParameter.getParameterValueByTypeAsObject(),
                    rightAttr.getAttributeType(),
                    rightParameter.isOverlap(),
                    attributeName,
                    compareParameters(leftParameter, rightParameter));
        } else {

            return new ComparedAttribute(
                    leftParameter.getAttributeId(),
                    leftParameter.getParameterValueByTypeAsObject(),
                    leftParameter.getAttribute().getAttributeType(),
                    leftParameter.isOverlap(),
                    rightParameter.getAttributeId(),
                    rightParameter.getParameterValueByTypeAsObject(),
                    rightParameter.getAttribute().getAttributeType(),
                    rightParameter.isOverlap(),
                    attributeName,
                    compareParameters(leftParameter, rightParameter));
        }
    }

    private ComparedAttribute getComparedAttribute(UUID leftDsId, UUID rightDsId,
                                                   Parameter parameter, String attributeName) {

        AttributeTypeName attrType;
        UUID attributeId = parameter.getAttributeId();
        Object parameterValue = parameter.getParameterValueByTypeAsObject();
        UUID parameterDatasetId = parameter.getDataSetId();

        if (parameter.isOverlap()) {
            attrType = parameter.getAttributeKey().getAttributeType();
        } else {
            attrType = parameter.getAttribute().getAttributeType();
        }

        ComparedAttribute comparedAttribute = new ComparedAttribute();
        if (leftDsId.equals(parameterDatasetId)) {
            log.debug("Create comparedAttribute for (leftParameterId: {}, rightParameterId: {}, attributeName: {})",
                    parameter.getId(), null, attributeName);
            comparedAttribute.setLeftAttributeId(attributeId);
            comparedAttribute.setLeftAttributeValue(parameterValue);
            comparedAttribute.setLeftAttributeType(attrType);
            comparedAttribute.setLeftAttributeIsOverlap(parameter.isOverlap());
        }
        if (rightDsId.equals(parameterDatasetId)) {
            log.debug("Create comparedAttribute for (leftParameterId: {}, rightParameterId: {}, attributeName: {})",
                    null, parameter.getId(), attributeName);
            comparedAttribute.setRightAttributeId(attributeId);
            comparedAttribute.setRightAttributeValue(parameterValue);
            comparedAttribute.setRightAttributeType(attrType);
            comparedAttribute.setRightAttributeIsOverlap(parameter.isOverlap());
        }
        comparedAttribute.setAttributeName(attributeName);
        comparedAttribute.setStatus(NOT_EQUAL);
        return comparedAttribute;
    }

    /**
     * Check that attribute type is comparable.
     */
    public boolean isAttributesComparable(UUID leftAttrId, UUID rightAttrId) {
        if (Objects.isNull(leftAttrId) || Objects.isNull(rightAttrId)) {
            return false;
        }
        AttributeTypeName leftAttributeType;
        AttributeTypeName rightAttributeType;

        if (modelsProvider.getAttributeById(leftAttrId) != null) {
            leftAttributeType = modelsProvider.getAttributeById(leftAttrId).getAttributeType();
            rightAttributeType = modelsProvider.getAttributeById(rightAttrId).getAttributeType();

            if (modelsProvider.isDslDifferentAttributes(leftAttrId, rightAttrId)) {
                throw new AttributeDslCopyException();
            }
        } else {
            leftAttributeType = modelsProvider.getAttributeKeyById(leftAttrId).getAttributeType();
            rightAttributeType = modelsProvider.getAttributeKeyById(rightAttrId).getAttributeType();
        }

        return leftAttributeType.equals(rightAttributeType);
    }

    /**
     * Compares attribute values.
     */
    public DetailedComparisonStatus compareAttributeValues(UUID leftDatasetId, UUID rightDataSetId,
                                                           UUID leftAttributeId, UUID rightAttributeId) {
        Parameter leftParameter =
                modelsProvider.getParameterByAttributeIdAndDataSetId(leftAttributeId, leftDatasetId);
        Parameter rightParameter =
                modelsProvider.getParameterByAttributeIdAndDataSetId(rightAttributeId, rightDataSetId);

        if (Objects.isNull(leftParameter) || Objects.isNull(rightParameter)) {
            if (Objects.isNull(leftParameter) && Objects.isNull(rightParameter)) {
                return isAttributesComparable(leftAttributeId, rightAttributeId) ? EQUAL : INCOMPATIBLE_TYPES;
            }
            return isAttributesComparable(leftAttributeId, rightAttributeId) ? NOT_EQUAL : INCOMPATIBLE_TYPES;
        }

        return compareParameters(leftParameter, rightParameter);
    }
}
