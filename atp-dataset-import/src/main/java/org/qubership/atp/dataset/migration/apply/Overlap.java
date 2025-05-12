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

import java.io.ByteArrayInputStream;

import javax.annotation.Nullable;

import org.qubership.atp.dataset.migration.formula.model.Formula;
import org.qubership.atp.dataset.migration.formula.model.TransformationException;
import org.qubership.atp.dataset.migration.model.ToOverlap;
import org.qubership.atp.dataset.migration.repo.DsServicesFacade;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.ParameterOverlap;
import org.qubership.atp.dataset.model.impl.file.FileData;

import com.google.common.base.Preconditions;

public interface Overlap {

    Overlap TEXT_OR_LIST_VALUE = new Overlap() {
        @Nullable
        @Override
        public ParameterOverlap overlapParameter(DsServicesFacade using, ToOverlap target, Formula value)
                throws TransformationException {
            Attribute attribute = target.getAttributeToOverlap();
            String lvName = value.getDatasetValue();
            switch (attribute.getType()) {
                case LIST:
                    ListValue listValue = using.get_ListValue_ByNameOrCreate(attribute, lvName);
                    return using.overlapParameter(target, null, null, listValue.getId());
                case TEXT:
                    return using.overlapParameter(target, value.getDatasetValue(), null, null);
                default:
                    throw new TransformationException("Can not overlap parameter of '" + attribute.getType()
                            + "' type.");
            }
        }
    };

    Overlap TEXT_ONLY = (using, target, value) -> {
        Parameter paramToOverlap = target.getParameterToOverlap();
        Preconditions.checkArgument(AttributeType.TEXT == paramToOverlap.getAttribute().getType(),
                "Can not overlap parameter with type '%s'. Type '%s' expected.",
                paramToOverlap.getAttribute().getType(), AttributeType.TEXT);
        if (target.getParameterToOverlap().getText().equals(value.getDatasetValue())) {
            return null;
        }
        return using.overlapParameter(target, value.getDatasetValue(), null, null);
    };

    Overlap FILE = (using, target, value) -> {
        ParameterOverlap parameter = using.overlapParameter(target, value.getDatasetValue(), null, null);
        FileData fileData = createFileParameterFromAttachedFile(value.getFile(), parameter.getId());
        using.fs.save(fileData,
                new ByteArrayInputStream(
                        value.getFile().getDataBuffer()
                ), false
        );
        parameter.setFileData(fileData);
        return parameter;
    };

    Overlap SKIP = (using, target, value) -> {
        //clean parameter attribute in DB
        //do nothing - in future check reference is to the valid parameter - if not - ERROR!
        return null;
    };

    Overlap MACROS_AS_TEXT_ONLY = (using, target, value) -> {
        Parameter paramToOverlap = target.getParameterToOverlap();
        Preconditions.checkArgument(AttributeType.TEXT == paramToOverlap.getAttribute().getType(),
                "Can not overlap parameter with type '%s'. Type '%s' expected.",
                paramToOverlap.getAttribute().getType(), AttributeType.TEXT);
        // if value is FORMULA and value == PARENT value (reference to parent value) - next
        if (target.getParameterToOverlap().getText().equals(value.getExcelFormulaValue())) {
            return null;
        }
        // if formula and diff from parent - calculate formula and set as value
        return using.overlapParameter(target, value.getDatasetValue(), null, null);
    };

    /**
     * Should set parameter value.
     */
    @Nullable
    ParameterOverlap overlapParameter(DsServicesFacade using, ToOverlap target, Formula value)
            throws TransformationException;
}
