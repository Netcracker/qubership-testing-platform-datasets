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

package org.qubership.atp.dataset.service.direct.helper;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.exception.dataset.DataSetExistsException;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.Label;
import org.qubership.atp.dataset.model.LabelProvider;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.TestPlan;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.model.impl.file.FileData;
import org.qubership.atp.dataset.service.direct.LabelProviderService;

import lombok.SneakyThrows;

public class DbCreationFacade extends AbstractServicesInjected implements CreationFacade {

    private final AtomicInteger attrOrder = new AtomicInteger(0);

    @Override
    public VisibilityArea va(@Nonnull String name) {
        return visibilityAreaService.create(name);
    }

    @Override
    public DataSetList dsl(@Nonnull VisibilityArea va, @Nonnull String name, @Nullable TestPlan testPlan) {
        UUID testPlanId = (testPlan != null) ? testPlan.getId() : null;
        return dataSetListService.create(va.getId(), name, testPlanId);
    }

    /**
     * Creates test plan.
     */
    @Override
    public TestPlan testPlan(@Nonnull VisibilityArea visibilityArea, @Nonnull String name) {
        return testPlanService.create(visibilityArea.getId(), name).getFirst();
    }

    @Override
    @SneakyThrows(DataSetExistsException.class)
    public DataSet ds(@Nonnull DataSetList dsl, @Nonnull String name) {
        return dataSetService.create(dsl.getId(), name);
    }

    @Override
    public Attribute attr(@Nonnull DataSetList dsl, @Nonnull String name, @Nonnull AttributeType type,
                          @Nullable DataSetList dslRef, @Nullable List<String> listValues) {
        UUID dslRefId = Optional.ofNullable(dslRef).map(Identified::getId).orElse(null);
        return attributeService.create(dsl.getId(), attrOrder.getAndIncrement(), name, type, dslRefId, listValues);
    }

    @Override
    public Parameter param(@Nonnull DataSet ds, @Nonnull Attribute attr,
                           @Nullable String text, @Nullable String listValue, @Nullable DataSet dsRef,
                           FileDataDao fileDataDto, @Nullable List<Attribute> path) {
        UUID dsRefId = Optional.ofNullable(dsRef).map(Identified::getId).orElse(null);
        ListValue lv = SimpleCreationFacade.getListValueByName(attr, listValue);
        UUID lvId = Optional.ofNullable(lv).map(Identified::getId).orElse(null);
        List<UUID> attrPathIds = null;
        if (path != null) {
            attrPathIds = path.stream().map(Identified::getId).collect(Collectors.toList());
        }
        Parameter result = parameterService.set(ds.getId(), attr.getId(), attrPathIds, text, dsRefId, lvId);
        if (fileDataDto != null) {
            FileData fileData = new FileData(fileDataDto.fileName,
                    result.getId(),
                    fileDataDto.contentType);
            gridFsService.save(fileData, new ByteArrayInputStream(fileDataDto.fileName.getBytes()), false);
            result.setFileData(fileData);
        }
        return result;
    }

    @Override
    public Label label(@Nonnull LabelProvider labelProvider, @Nonnull String name) {
        LabelProviderService service;
        UUID targetId;
        if (labelProvider instanceof DataSet) {
            service = dataSetService;
            targetId = ((DataSet) labelProvider).getId();
        } else if (labelProvider instanceof DataSetList) {
            service = dataSetListService;
            targetId = ((DataSetList) labelProvider).getId();
        } else {
            throw new IllegalArgumentException("No service found for ["
                    + labelProvider.getClass().getSimpleName() + "] LabelProvider");
        }
        return service.mark(targetId, name);
    }
}
