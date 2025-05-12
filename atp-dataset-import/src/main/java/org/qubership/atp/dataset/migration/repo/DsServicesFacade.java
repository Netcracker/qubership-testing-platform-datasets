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

package org.qubership.atp.dataset.migration.repo;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import org.qubership.atp.dataset.migration.model.OverlapParamContainer;
import org.qubership.atp.dataset.migration.model.ToOverlap;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.ParameterOverlap;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.service.direct.AttributeService;
import org.qubership.atp.dataset.service.direct.DataSetListService;
import org.qubership.atp.dataset.service.direct.DataSetService;
import org.qubership.atp.dataset.service.direct.GridFsService;
import org.qubership.atp.dataset.service.direct.ParameterService;
import org.qubership.atp.dataset.service.direct.VisibilityAreaService;

import com.google.common.base.Preconditions;

/**
 * Encapsulates all interaction with DS service.
 */
public class DsServicesFacade {

    public final VisibilityAreaService va;
    public final DataSetListService dsl;
    public final ParameterService param;
    public final AttributeService attr;
    public final GridFsService fs;
    private final OrderedAttributeRepository attrRepo;//is used to avoid order calculation
    private final DataSetService ds;

    /**
     * See {@link DsServicesFacade}.
     */
    public DsServicesFacade(VisibilityAreaService va,
                            DataSetListService dsl,
                            OrderedAttributeRepository attrRepo,
                            DataSetService ds,
                            ParameterService param,
                            AttributeService attr,
                            GridFsService fs) {
        this.va = va;
        this.dsl = dsl;
        this.attrRepo = attrRepo;
        this.ds = ds;
        this.param = param;
        this.attr = attr;
        this.fs = fs;
    }

    /**
     * Overlaps parent {@link ToOverlap#getParameterToOverlap()} by new value in {@link
     * OverlapParamContainer}.
     */
    public ParameterOverlap overlapParameter(ToOverlap childParameter,
                                             String textValue,
                                             UUID dsRef,
                                             UUID listValueRef) {
        UUID attrId = childParameter.getAttributeToOverlap().getId();
        OverlapParamContainer childGroup = childParameter.getContainer();
        final UUID rootDataSetId = childGroup.getDs().getId();
        return param.set(rootDataSetId, attrId,
                Collections.singletonList(childGroup.getRefToDsl().getId()),
                textValue, dsRef, listValueRef).asOverlap();
    }

    /**
     * Creates {@link VisibilityArea} if no item with specified name exists.
     *
     * @return existing va.
     */
    public VisibilityArea get_VA_ByNameOrCreate(String vaName) {
        return va.getAll().stream()
                .filter(visibilityArea -> visibilityArea.getName().equals(vaName))
                .findFirst()
                .orElseGet(() -> va.create(vaName));
    }

    /**
     * Tries to find {@link DataSetList} by name in {@link VisibilityArea}.
     */
    public Optional<DataSetList> getDslByName(VisibilityArea visibilityArea, String dslName) {
        return visibilityArea.getDataSetLists().stream()
                .filter(dsl -> dsl.getName().equals(dslName))
                .findFirst();
    }

    public DataSetList getDslByNameOrCreate(VisibilityArea visibilityArea, String dslName) {
        return getDslByName(visibilityArea, dslName).orElseGet(() ->
                dsl.get(dsl.create(visibilityArea.getId(), dslName, null).getId()));
    }

    /**
     * Tries to find {@link DataSet} by name in {@link DataSetList}.
     */
    public Optional<DataSet> getDsByName(DataSetList dataSetList, String dataSetName) {
        return dataSetList.getDataSets().stream()
                .filter(ds -> ds.getName().equals(dataSetName))
                .findFirst();
    }

    /**
     * Find {@link DataSet} by name in {@link DataSetList} or create new one with specified name.
     *
     * @return dateset.
     */
    public DataSet get_DS_ByNameOrCreate(String dataSetName, DataSetList dataSetList) {
        return getDsByName(dataSetList, dataSetName).orElseGet(() -> ds.create(dataSetList.getId(), dataSetName));
    }

    /**
     * Creates {@link Attribute} if no item with specified name exists.
     *
     * @return existing attribute.
     */
    public Attribute get_Attr_ByNameOrCreate(DataSetList dataSetList, String paramName,
                                             AttributeType attributeType, UUID dslRefId,
                                             @Nullable List<String> listValues) {
        return attrRepo.getByParentId(dataSetList.getId()).stream()
                .filter(attribute -> attribute.getName().equals(paramName))
                .findFirst()
                .orElseGet(() -> attrRepo.create(dataSetList.getId(),
                        paramName, attributeType, dslRefId, listValues));
    }

    /**
     * Creates {@link ListValue} if no item with specified name exists.
     *
     * @return existing list value.
     */
    public ListValue get_ListValue_ByNameOrCreate(Attribute attribute, String name) {
        Attribute freshInstance = attr.get(attribute.getId());//to be sure that we got actual data
        Preconditions.checkNotNull(freshInstance);
        return freshInstance.getListValues()
                .stream()
                .filter(lv -> lv.getName().equals(name))
                .findAny()
                .orElseGet(() -> attr.createListValue(attribute.getId(), name));
    }

    /**
     * Creates {@link Parameter} if no item with specified name in provided {@link Attribute}
     * exists.
     *
     * @return existing parameter.
     */
    public Parameter get_Param_ByNameOrCreate(DataSet dataSet, Attribute attribute, UUID typeDslId) {
        DataSet freshInstance = ds.get(dataSet.getId());//to be sure that we got actual data
        Preconditions.checkNotNull(freshInstance);
        return freshInstance.getParameters().stream()
                .filter(parameter -> parameter.getAttribute().getId().equals(attribute.getId()))
                .findFirst()
                .orElseGet(() ->
                        param.create(
                                dataSet.getId(), attribute.getId(), null, null, typeDslId)
                );
    }
}
