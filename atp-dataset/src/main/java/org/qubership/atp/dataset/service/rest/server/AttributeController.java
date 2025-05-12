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

package org.qubership.atp.dataset.service.rest.server;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.service.direct.AttributeService;
import org.qubership.atp.dataset.service.direct.ConcurrentModificationService;
import org.qubership.atp.dataset.service.direct.DataSetListService;
import org.qubership.atp.dataset.service.jpa.JpaAttributeService;
import org.qubership.atp.dataset.service.jpa.delegates.ListValue;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.service.rest.AttributeCreateResponse;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManDataSetList;
import org.qubership.atp.dataset.service.ws.entities.Pair;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Preconditions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/attribute")
@RequiredArgsConstructor
public class AttributeController {

    private final AttributeService attributeService;
    private final JpaAttributeService jpaAttributeService;
    private final DataSetListService dslService;
    private final ConcurrentModificationService concurrentModificationService;

    /**
     * Creates new attribute with provided parameters.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).ATTRIBUTE.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'CREATE')")
    @PutMapping("/dsl/{dataSetListId}")
    @AuditAction(auditAction = "Create new attribute with name: {{#name}} in dataset list: {{#dataSetListId}}")
    @Operation(summary = "Creates new attribute with provided name and order.")
    public ResponseEntity<AttributeCreateResponse> create(
            @PathVariable("dataSetListId") UUID dataSetListId,
            @RequestParam("name") String name,
            @RequestParam("ordering") Integer ordering,
            @RequestParam("type") AttributeTypeName attributeType,
            @RequestParam(value = "typeDataSetListId", required = false) UUID typeDataSetListId,
            @RequestBody(required = false) List<String> listValues,
            @RequestParam(value = "modifiedWhen", required = false) Long modifiedWhen,
            HttpServletRequest request) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        org.qubership.atp.dataset.service.jpa.delegates.Attribute attribute =
                jpaAttributeService.create(name, attributeType, dataSetListId, typeDataSetListId, listValues);
        dslService.evictAllAffectedDatasetsFromContextCacheByDslId(dataSetListId);
        List<ListValue> createdListValues = attribute.getListValues();
        List<AttributeCreateResponse.ListValue> listValuesUuid =
                createdListValues.stream().map(listValue -> new AttributeCreateResponse.ListValue(
                        attribute.getId(), listValue.getId(), listValue.getText()
                        )).collect(Collectors.toList());
        AttributeCreateResponse createResponse = new AttributeCreateResponse(
                attribute.getId(), name, dataSetListId, attributeType, typeDataSetListId, listValuesUuid);
        StringBuffer url = request.getRequestURL();
        url.append("/").append(attribute.getId());
        URI uri = URI.create(url.toString());
        return HttpStatus.IM_USED.equals(httpStatus)
                ? ResponseEntity.status(httpStatus).location(uri).body(createResponse)
                : ResponseEntity.created(uri).body(createResponse);
    }

    /**
     * Deletes selected attribute.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).ATTRIBUTE.getName(),"
            + "@attributeServiceImpl.get(#attributeId).getDataSetList().getVisibilityArea().getId(),'DELETE')")
    @DeleteMapping("/{attributeId}")
    @AuditAction(auditAction = "Delete attribute: {{#attributeId}}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deletes selected attribute.")
    public ResponseEntity<Boolean> delete(@PathVariable("attributeId") UUID attributeId,
                                          @RequestParam(value = "dataSetListId", required = false) UUID dataSetListId,
                                          @RequestParam(value = "modifiedWhen", required = false) Long modifiedWhen) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        attributeService.delete(attributeId);
        dslService.evictAllAffectedDatasetsFromContextCacheByDslId(dataSetListId);
        return HttpStatus.IM_USED.equals(httpStatus)
                ? ResponseEntity.status(httpStatus).build()
                : ResponseEntity.noContent().build();
    }

    /**
     * Deletes attributes from the selected dataSetsList.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).ATTRIBUTE.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'DELETE')")
    @DeleteMapping("/dsl/{dataSetListId}/all")
    @AuditAction(auditAction = "Delete attributes in dataset list: {{#dataSetListId}}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deletes attributes from the selected dataSetsList.")
    public ResponseEntity<Void> deleteAllByDsl(@PathVariable("dataSetListId") UUID dataSetListId,
                                               @RequestParam(value = "modifiedWhen", required = false)
                                                       Long modifiedWhen) {
        DataSetList dataSetList = dslService.get(dataSetListId);
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        if (dataSetList != null) {
            for (Attribute attribute : dataSetList.getAttributes()) {
                attributeService.delete(attribute.getId());
            }
            attributeService.updateTypeDslId(dataSetListId, null);
            dslService.evictAllAffectedDatasetsFromContextCacheByDslId(dataSetListId);
        }
        return HttpStatus.IM_USED.equals(httpStatus)
                ? ResponseEntity.status(httpStatus).build()
                : ResponseEntity.noContent().build();
    }

    /**
     * See {@link AttributeService#getByDslId(UUID)}.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).ATTRIBUTE.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'READ')")
    @GetMapping("/dsl/{dataSetListId}")
    @AuditAction(auditAction = "Get attributes in dataset list: {{#dataSetListId}}")
    @Operation(
            summary = "Returns all attributes by dataSetList id.",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Attribute.class)))))
    public Collection<Attribute> getAttributes(@PathVariable("dataSetListId") UUID dataSetListId) {
        return attributeService.getByDslId(dataSetListId);
    }

    /**
     * See {@link AttributeService#getByDslIdInItfFormat(UUID)}.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).ATTRIBUTE.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'READ')")
    @GetMapping("/dsl/{dataSetListId}/itf")
    @AuditAction(auditAction = "attributes in ITF format from dataset list: {{#dataSetListId}}")
    @Operation(summary = "Returns all attributes by dataSetList id in itf format: [attr1.attr2, attr1].")
    public Object getAttributesInItfFormat(@PathVariable("dataSetListId") UUID dataSetListId) {
        return attributeService.getByDslIdInItfFormat(dataSetListId);
    }

    /**
     * Renames selected attribute.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).ATTRIBUTE.getName(),"
            + "@attributeServiceImpl.get(#attributeId).getDataSetList().getVisibilityArea().getId(),'UPDATE')")
    @PostMapping("/{attributeId}")
    @AuditAction(auditAction = "Rename attribute: {{#attributeId}} on {{#name}}")
    @Operation(summary = "Renames selected attribute.")
    public ResponseEntity<Boolean> rename(@PathVariable("attributeId") UUID attributeId,
                                          @RequestParam("name") String name,
                                          @RequestParam(value = "dataSetListId", required = false) UUID dataSetListId,
                                          @RequestParam(value = "modifiedWhen", required = false) Long modifiedWhen) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        final boolean isUpdated = attributeService.update(attributeId, name);
        if (Objects.nonNull(dataSetListId)) {
            dslService.evictAllAffectedDatasetsFromContextCacheByDslId(dataSetListId);
        }
        return HttpStatus.IM_USED.equals(httpStatus)
                ? ResponseEntity.status(httpStatus).body(isUpdated)
                : ResponseEntity.ok(isUpdated);
    }

    /**
     * Update DSL reference.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).ATTRIBUTE.getName(),"
            + "@attributeServiceImpl.get(#attributeId).getDataSetList().getVisibilityArea().getId(),'UPDATE')")
    @PutMapping("/{attributeId}/dataSetListReference")
    @AuditAction(auditAction = "Update dataset list reference in attribute: {{#attributeId}}")
    @Operation(summary = "Update DSL reference.")
    public ResponseEntity<Boolean> updateDslReference(@PathVariable("attributeId") UUID attributeId,
                                      @RequestParam("value") UUID uuid) {
        Preconditions.checkNotNull(uuid, "Referenced DSL uuid should not be null.");
        boolean isUpdated = attributeService.updateDslReference(attributeId, uuid);
        return ResponseEntity.ok(isUpdated);
    }

    /**
     * Returns attribute.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).ATTRIBUTE.getName(),"
            + "@attributeServiceImpl.get(#attributeId).getDataSetList().getVisibilityArea().getId(),'READ')")
    @GetMapping("/{attributeId}")
    @AuditAction(auditAction = "Update dataset list reference in attribute: {{#attributeId}}")
    @Operation(summary = "Returns attribute.")
    public Attribute get(@PathVariable("attributeId") UUID attributeId) {
        return attributeService.get(attributeId);
    }

    /**
     * Returns options of attribute.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).ATTRIBUTE.getName(),"
            + "@attributeServiceImpl.get(#attributeId).getDataSetList().getVisibilityArea().getId(),'READ')")
    @GetMapping("/{attributeId}/options")
    @AuditAction(auditAction = "Get options attribute: {{#attributeId}}")
    public Object getOptions(@PathVariable("attributeId") UUID attributeId) {
        return attributeService.getOptions(attributeId);
    }

    /**
     * Creates list value.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).ATTRIBUTE.getName(),"
            + "@attributeServiceImpl.get(#attributeId).getDataSetList().getVisibilityArea().getId(),'UPDATE')")
    @PutMapping("/{attributeId}/listValues")
    @AuditAction(auditAction = "Create new list value for attribute: {{#attributeId}}")
    @Operation(summary = "Creates new list value for provided attribute.")
    public ResponseEntity<org.qubership.atp.dataset.model.ListValue> createListValue(
            @PathVariable("attributeId") UUID attributeId,
            @RequestParam("value") String value,
            @RequestParam(value = "dataSetListId", required = false) UUID dataSetListId,
            @RequestParam(value = "modifiedWhen", required = false) Long modifiedWhen,
            HttpServletRequest request) {

        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        org.qubership.atp.dataset.model.ListValue listValue = attributeService.createListValue(attributeId, value);
        StringBuffer url = request.getRequestURL();
        url.append("/").append(listValue.getId());
        URI uri = URI.create(url.toString());
        return HttpStatus.IM_USED.equals(httpStatus)
                ? ResponseEntity.status(httpStatus).location(uri).body(listValue)
                : ResponseEntity.created(uri).body(listValue);
    }

    /**
     * Creates list values.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).ATTRIBUTE.getName(),"
            + "@attributeServiceImpl.get(#attributeId).getDataSetList().getVisibilityArea().getId(),'UPDATE')")
    @PostMapping("/{attributeId}/listValues")
    @AuditAction(auditAction = "Create new list values for attribute: {{#attributeId}} by text")
    @Operation(summary = "Creates new list values for provided attribute by text.")
    public ResponseEntity<List<UUID>> createListValues(@PathVariable("attributeId") UUID attributeId,
                                                       @RequestBody List<String> values,
                                                       @RequestParam(value = "dataSetListId", required = false)
                                                               UUID dataSetListId,
                                                       @RequestParam(value = "modifiedWhen", required = false)
                                                               Long modifiedWhen) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        List<UUID> listValues = attributeService.createListValues(attributeId, values);
        return HttpStatus.IM_USED.equals(httpStatus)
                ? ResponseEntity.status(httpStatus).body(listValues)
                : ResponseEntity.status(HttpStatus.CREATED).body(listValues);
    }

    /**
     * Deletes list value from attribute.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).ATTRIBUTE.getName(),"
            + "@attributeServiceImpl.get(#attributeId).getDataSetList().getVisibilityArea().getId(),'UPDATE')")
    @DeleteMapping("/{attributeId}/listValues/{listValueId}")
    @AuditAction(auditAction = "Delete list value: {{#listValueId}} in attribute: {{#attributeId}}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete list value by id.")
    public ResponseEntity<Void> deleteListValue(
            @PathVariable("attributeId") UUID attributeId,
            @PathVariable("listValueId") UUID listValueId,
            @RequestParam(value = "dataSetListId", required = false) UUID dataSetListId,
            @RequestParam(value = "modifiedWhen", required = false) Long modifiedWhen) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        attributeService.deleteListValue(attributeId, listValueId);
        return HttpStatus.IM_USED.equals(httpStatus)
                ? ResponseEntity.status(httpStatus).build()
                : ResponseEntity.noContent().build();
    }

    /**
     * See {@link AttributeService#deleteListValues(UUID, List)}.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).ATTRIBUTE.getName(),"
            + "@attributeServiceImpl.get(#attributeId).getDataSetList().getVisibilityArea().getId(),'UPDATE')")
    @DeleteMapping("/{attributeId}/bulk")
    @AuditAction(auditAction = "Bulk delete list value: {{#listValueIds}} in attribute: {{#attributeId}}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Bulk delete list values by id.")
    public ResponseEntity<Void> bulkDeleteListValues(
            @PathVariable("attributeId") UUID attributeId,
            @RequestParam("listValues") List<UUID> listValueIds,
            @RequestParam(value = "dataSetListId", required = false) UUID dataSetListId,
            @RequestParam(value = "modifiedWhen", required = false) Long modifiedWhen) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        attributeService.deleteListValues(attributeId, listValueIds);
        return HttpStatus.IM_USED.equals(httpStatus)
                ? ResponseEntity.status(httpStatus).build()
                : ResponseEntity.noContent().build();
    }

    /**
     * Update ordering of attributes.
     *
     * @param attributesOrdering info about attributes for updating ({ID, ordering})
     */
    @PreAuthorize("@entityAccess.isAuthenticated()")
    @PutMapping("/updateOrdering")
    @AuditAction(auditAction = "Update ordering of attributes")
    public void updateOrdering(@RequestBody List<Pair<UUID, Integer>> attributesOrdering) {
        attributeService.updateOrdering(attributesOrdering);
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).ATTRIBUTE.getName(),"
            + "@attributeServiceImpl.get(#attributeId).getDataSetList().getVisibilityArea().getId(),'UPDATE')")
    @PostMapping("/{attributeId}/position")
    @AuditAction(auditAction = "Set position of attribute: {{#attributeId}}")
    @Operation(summary = "Creates new list values for provided attribute by text.")
    public void setPosition(@PathVariable("attributeId") UUID attributeId,
                            @RequestBody Integer position) {
        jpaAttributeService.setPosition(attributeId, position);
    }

    /**
     * Filtering of attribute values in all datasets.
     *
     * @param dataSetListId DatasetList UUID
     * @param attrFilterIds contains attribute Ids as path to target attribute or the targetAttrId
     * @param targetAttrId attribute UUID to filtering values
     * @return attribute values and dataset Ids as Map
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).ATTRIBUTE.getName(),"
            + "@attributeServiceImpl.get(#targetAttrId).getDataSetList().getVisibilityArea().getId(),'READ')")
    @PostMapping("/{dataSetListId}/existedValues")
    @AuditAction(auditAction = "Get parameters value and datasets id of attribute: {{#targetAttrId}}")
    @Operation(summary = "Sort Attribute values.")
    public Map<String, List<UUID>> getParametersValuesAndDataSetIdsForAttributeValuesSorting(
            @PathVariable("dataSetListId") UUID dataSetListId,
            @RequestBody(required = true) List<UUID> attrFilterIds,
            @RequestParam(value = "targetAttrId", required = true) UUID targetAttrId) {
        UiManDataSetList tree = dslService.getAsTree(
                dataSetListId, false, null, Collections.singletonList(attrFilterIds.get(0)), null, null,
                false, true);
        return attributeService
                .getParametersAndDataSetIdsForAttributeSorting(tree, dataSetListId, targetAttrId, attrFilterIds);
    }
}
