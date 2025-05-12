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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributePath;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Label;
import org.qubership.atp.dataset.model.LabelProvider;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.TestPlan;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.model.impl.AttributeImpl;
import org.qubership.atp.dataset.model.impl.AttributePathImpl;
import org.qubership.atp.dataset.model.impl.DataSetImpl;
import org.qubership.atp.dataset.model.impl.DataSetListImpl;
import org.qubership.atp.dataset.model.impl.LabelImpl;
import org.qubership.atp.dataset.model.impl.ListValueImpl;
import org.qubership.atp.dataset.model.impl.ParameterImpl;
import org.qubership.atp.dataset.model.impl.ParameterOverlapImpl;
import org.qubership.atp.dataset.model.impl.TestPlanImpl;
import org.qubership.atp.dataset.model.impl.VisibilityAreaImpl;
import org.qubership.atp.dataset.model.impl.file.FileData;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class SimpleCreationFacade implements CreationFacade {

    public static final SimpleCreationFacade INSTANCE = new SimpleCreationFacade();
    private static final Supplier<UUID> UUIDS = UUID::randomUUID;
    private static final UUID CREATED_BY = UUID.fromString("1c0167c9-1bea-4587-8f32-d637ff341d31");

    private SimpleCreationFacade() {
    }

    /**
     * Searches for existing list value by name.
     */
    @Nullable
    public static ListValue getListValueByName(@Nonnull Attribute source, @Nullable String listValueName) {
        if (listValueName == null) {
            return null;
        }
        Preconditions.checkNotNull(source.getListValues(),
                "Can not find list value with name %s: source %s has no list values",
                listValueName, source);
        Optional<ListValue> lvOpt = source.getListValues().stream()
                .filter(lv -> listValueName.equals(lv.getName())).findFirst();
        Preconditions.checkArgument(lvOpt.isPresent(),
                "No list value with name %s found. Available list values: %s",
                listValueName, source.getListValues());
        return lvOpt.get();
    }

    /**
     * Creates a list values by using names provided.
     */
    @Nonnull
    private static List<ListValue> convertListValues(@Nonnull Attribute attr, @Nullable List<String> lvNames) {
        if (lvNames == null) {
            return Lists.newArrayList();
        }
        return lvNames.stream().map(lvName -> new ListValueImpl(UUIDS.get(), attr, lvName))
                .collect(Collectors.toList());
    }

    /**
     * Creates a data set list.
     */
    public DataSetList dsl(@Nonnull VisibilityArea va, @Nonnull String name, @Nullable TestPlan testPlan) {
        Timestamp createdWhen = Timestamp.from(Instant.now());
        DataSetListImpl dataSetList = new DataSetListImpl(UUIDS.get(), va, name,
                Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList(), testPlan,
                CREATED_BY, createdWhen, CREATED_BY, createdWhen);
        va.getDataSetLists().add(dataSetList);
        return dataSetList;
    }

    public DataSetList dsl(@Nonnull VisibilityArea va, @Nonnull String name) {
        return dsl(va, name, null);
    }

    /**
     * Creates a va.
     */
    public VisibilityArea va(@Nonnull String name) {
        return new VisibilityAreaImpl(UUIDS.get(), name, Lists.newArrayList());
    }

    /**
     * Creates test plan.
     */
    @Override
    public TestPlan testPlan(@Nonnull VisibilityArea visibilityArea, @Nonnull String name) {
        return new TestPlanImpl(UUIDS.get(), name, visibilityArea);
    }

    public DataSet ds(@Nonnull DataSetList dsl, @Nonnull String name) {
        return ds(dsl, name, null);
    }

    /**
     * Creates a ds.
     */
    public DataSet ds(@Nonnull DataSetList dsl, @Nonnull String name, @Nullable TestPlan testPlan) {
        DataSet ds = new DataSetImpl(UUIDS.get(), name, dsl, Lists.newArrayList(), Lists.newArrayList(), false);
        dsl.getDataSets().add(ds);
        return ds;
    }

    /**
     * Creates a param.
     */
    public Parameter param(@Nonnull DataSet ds, @Nonnull Attribute attr,
                           String text, String listValue, DataSet dsRef,
                           FileDataDao fileDataDao, @Nullable List<Attribute> path) {
        ListValue lv = getListValueByName(attr, listValue);
        Parameter result;
        if (path == null) {
            result = new ParameterImpl(UUIDS.get(), attr, ds, text, lv, dsRef);
            attr.getParameters().add(result);
        } else {
            AttributePath attrPath = new AttributePathImpl(UUIDS.get(), ds, attr, path);
            result = new ParameterOverlapImpl(UUIDS.get(), attrPath, ds, text, lv, dsRef);
        }
        if (fileDataDao != null) {
            FileData fileData = new FileData(fileDataDao.fileName,
                    result.getId(),
                    fileDataDao.contentType);
            result.setFileData(fileData);
        }
        ds.getParameters().add(result);
        return result;
    }

    @Override
    public Label label(@Nonnull LabelProvider labelProvider, @Nonnull String name) {
        Label result = new LabelImpl(UUIDS.get(), name);
        labelProvider.getLabels().add(result);
        return result;
    }

    /**
     * Creates an attr.
     */
    public Attribute attr(@Nonnull DataSetList dsl, @Nonnull String name, @Nonnull AttributeType type,
                          DataSetList dslRef,
                          List<String> listValues) {
        Attribute result = new AttributeImpl(UUIDS.get(), name, dsl, type,
                dslRef, null, Lists.newArrayList());
        List<ListValue> lvs = convertListValues(result, listValues);
        result.setListValues(lvs);
        dsl.getAttributes().add(result);
        return result;
    }
}
