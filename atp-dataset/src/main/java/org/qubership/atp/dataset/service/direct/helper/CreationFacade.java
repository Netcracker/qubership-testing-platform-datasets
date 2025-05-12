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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Label;
import org.qubership.atp.dataset.model.LabelProvider;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.TestPlan;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.model.utils.ChangeType;

public interface CreationFacade {

    TestPlan testPlan(@Nonnull VisibilityArea visibilityArea, @Nonnull String name);

    VisibilityArea va(@Nonnull String name);

    DataSetList dsl(@Nonnull VisibilityArea va, @Nonnull String name, @Nullable TestPlan testPlan);

    default DataSetList dsl(@Nonnull VisibilityArea va, @Nonnull String name) {
        return dsl(va, name, null);
    }

    default DataSetList dsl(String vaName, String name, @Nullable TestPlan testPlan) {
        return dsl(va(vaName), name, testPlan);
    }

    default DataSetList dsl(String vaName, String name) {
        return dsl(va(vaName), name);
    }

    DataSet ds(@Nonnull DataSetList dsl, @Nonnull String name);

    default DataSet ds(String vaName, String dslName, String dsName) {
        return ds(dsl(vaName, dslName), dsName);
    }

    default DataSet ds(VisibilityArea va, String dslName, String dsName) {
        return ds(dsl(va, dslName), dsName);
    }

    Label label(@Nonnull LabelProvider labelProvider, @Nonnull String name);

    Attribute attr(@Nonnull DataSetList dsl, @Nonnull String name, @Nonnull AttributeType type,
                   DataSetList dslRef,
                   List<String> listValues);

    Parameter param(@Nonnull DataSet ds, @Nonnull Attribute attr,
                    String text, String listValue, DataSet dsRef,
                    @Nullable FileDataDao fileData, @Nullable List<Attribute> path);

    @Nonnull
    default String schange(@Nonnull ChangeType operation, @Nullable List<UUID> args) {
        return operation.toText(args);
    }

    default Parameter textParam(DataSet ds, String key, String text) {
        return textParam(ds, textAttr(ds.getDataSetList(), key), text);
    }

    default Parameter textParam(DataSet ds, Attribute attr, String text) {
        return param(ds, attr, text, null, null, null, null);
    }

    default Parameter schangeParam(DataSet ds, String key, DataSetList dslId, ChangeType operation, UUID... args) {
        return schangeParam(ds, schangeAttr(ds.getDataSetList(), key, dslId), operation, args);
    }

    default Parameter schangeParam(DataSet ds, Attribute attr, ChangeType operation, UUID... args) {
        return textParam(ds, attr, schange(operation, Arrays.asList(args)));
    }

    default Parameter refParam(DataSet from, DataSet to) {
        return refParam(from, to.getDataSetList().getName(), to);
    }

    default Parameter refParam(DataSet from, String key, DataSet to) {
        return refParam(from, refAttr(from.getDataSetList(), key, to.getDataSetList()), to);
    }

    default Parameter refParam(DataSet ds, Attribute refAttr, DataSet refDs) {
        return param(ds, refAttr, null, null, refDs, null, null);
    }

    default Parameter listParam(DataSet ds, String key, String selectedListValue, String... possibleListValues) {
        return listParam(ds, listAttr(ds.getDataSetList(), key, possibleListValues), selectedListValue);
    }

    default Parameter listParam(DataSet ds, Attribute attr, String listValue) {
        return param(ds, attr, null, listValue, null, null, null);
    }

    default Parameter fileParam(DataSet ds, String key, @Nonnull String fileName, @Nonnull String contentType) {
        return fileParam(ds, key, new FileDataDao(fileName, contentType));
    }

    default Parameter fileParam(DataSet ds, String key, FileDataDao fileData) {
        return fileParam(ds, fileAttr(ds.getDataSetList(), key), fileData);
    }

    default Parameter fileParam(DataSet ds, Attribute attr, FileDataDao fileData) {
        return param(ds, attr, null, null, null, fileData, null);
    }

    default Parameter overrideParam(DataSet ds, Attribute attr, String text, String listValue, DataSet dsRef,
                                    FileDataDao fileData, Attribute... pathToOverridden) {
        return overrideParam(ds, attr, text, listValue, dsRef, fileData, Arrays.asList(pathToOverridden));
    }

    default Parameter overrideParam(DataSet ds, Attribute attr, String text, String listValue, DataSet dsRef,
                                    FileDataDao fileData, @Nonnull List<Attribute> pathToOverridden) {
        return param(ds, attr, text, listValue, dsRef, fileData, pathToOverridden);
    }

    default Attribute schangeAttr(DataSetList dsl, String name, DataSetList dslRef) {
        return attr(dsl, name, AttributeType.CHANGE, dslRef, null);
    }

    default Attribute textAttr(DataSetList dsl, String name) {
        return attr(dsl, name, AttributeType.TEXT, null, null);
    }

    default Attribute refAttr(DataSetList dsl, String name, DataSetList dslRef) {
        return attr(dsl, name, AttributeType.DSL, dslRef, null);
    }

    default Attribute fileAttr(DataSetList dsl, String name) {
        return attr(dsl, name, AttributeType.FILE, null, null);
    }

    default Attribute listAttr(DataSetList dsl, String name, String... listValues) {
        return attr(dsl, name, AttributeType.LIST, null, Arrays.asList(listValues));
    }

    default Attribute listAttr(DataSetList dsl, String name, List<String> listValues) {
        return attr(dsl, name, AttributeType.LIST, null, listValues);
    }
}
