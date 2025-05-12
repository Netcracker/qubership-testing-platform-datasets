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

package org.qubership.atp.dataset.migration.apply;

import java.io.ByteArrayInputStream;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.migration.formula.model.Formula;
import org.qubership.atp.dataset.migration.formula.model.TransformationException;
import org.qubership.atp.dataset.migration.model.ToCreate;
import org.qubership.atp.dataset.migration.repo.DsServicesFacade;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.impl.file.FileData;

import com.google.common.base.Preconditions;

public interface SetParam {

    SetParam TEXT = (using, source, target, value) ->
            using.param.update(target.getId(), value.getDatasetValue());

    SetParam LIST_VALUE = (using, source, target, value) -> {
        Attribute attr = target.getAttribute();
        String lvName = value.getDatasetValue();
        Preconditions.checkArgument(AttributeType.LIST == attr.getType(),
                "Existing attribute has type '%s', but type '%s' expected.",
                attr.getType(), AttributeType.LIST);
        ListValue listValue = using.get_ListValue_ByNameOrCreate(attr, lvName);
        using.param.set(target.getDataSet().getId(), attr.getId(), null,
                null, null, listValue.getId());
    };

    SetParam FILE = (using, source, target, value) -> {
        FileData fileData = target.getFileData();
        using.fs.save(fileData,
                new ByteArrayInputStream(
                        value.getFile().getDataBuffer()
                ), false
        );
        using.param.update(fileData.getParameterUuid(), value.getDatasetValue());
    };

    void setParameterValue(DsServicesFacade using, ToCreate source, @Nonnull Parameter target, Formula value)
            throws TransformationException;
}
