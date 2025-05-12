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

import static org.qubership.atp.dataset.migration.formula.model.AttachedFiles.createFileParameterFromAttachedFile;

import java.util.Collections;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.migration.formula.model.Formula;
import org.qubership.atp.dataset.migration.formula.model.TransformationException;
import org.qubership.atp.dataset.migration.model.ToCreate;
import org.qubership.atp.dataset.migration.repo.DsServicesFacade;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.impl.file.FileData;

import com.google.common.base.Preconditions;

public interface CreateParam {

    CreateParam TEXT = (using, target, value) -> {
        Attribute attr = using.get_Attr_ByNameOrCreate(target.getDsl(), target.getAttrName(), AttributeType.TEXT,
                null, null);
        return using.get_Param_ByNameOrCreate(target.getDs(), attr, null);
    };

    CreateParam LIST_VALUE = new CreateParam() {
        @Nonnull
        @Override
        public Parameter createParameter(DsServicesFacade using, ToCreate target, Formula value) {
            Attribute attr = using.get_Attr_ByNameOrCreate(target.getDsl(), target.getAttrName(), AttributeType.LIST,
                    null, Collections.singletonList(value.getDatasetValue()));
            Preconditions.checkArgument(AttributeType.LIST == attr.getType(),
                    "Existing attribute has type '%s', but type '%s' expected.",
                    attr.getType(), AttributeType.LIST);
            return using.get_Param_ByNameOrCreate(target.getDs(), attr, null);
        }
    };

    CreateParam FILE = (using, target, value) -> {
        Attribute attr = using.get_Attr_ByNameOrCreate(target.getDsl(), target.getAttrName(), AttributeType.FILE,
                null, null);
        Parameter parameter = using.get_Param_ByNameOrCreate(target.getDs(), attr, null);
        FileData fileData = createFileParameterFromAttachedFile(value.getFile(), parameter.getId());
        parameter.setFileData(fileData);
        return parameter;
    };

    /**
     * Should not set any parameter value.
     */
    @Nonnull
    Parameter createParameter(DsServicesFacade using, ToCreate target, Formula value) throws TransformationException;
}
