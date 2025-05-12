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

package org.qubership.atp.dataset.service.jpa.model.dscontext;

import java.io.Serializable;
import java.util.UUID;

import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParameterContext implements Serializable {
    private static final long serialVersionUID = -2995730089590160691L;
    private UUID attributeId;
    private UUID parameterId;
    private long order;
    private String name;
    private AttributeTypeName type;
    private String value;
    private UUID dataSetReferenceId;
    private UUID listValueId;
    private boolean overlap;
    private boolean nullValue;

    /**
     * New parameter context.
     * */
    public ParameterContext(Attribute parentTextAttribute, Parameter parameter, boolean overlap) {
        this.name = parentTextAttribute.getName();
        this.attributeId = parentTextAttribute.getId();
        this.value = parameter.getParameterValueByType();
        this.dataSetReferenceId = parameter.getDataSetReferenceId();
        this.type = parentTextAttribute.getAttributeType();
        this.parameterId = parameter.getId();
        this.order = parentTextAttribute.getOrdering();
        this.overlap = overlap;
        if (parameter.getListValue() != null) {
            this.listValueId = parameter.getListValue().getId();
        }
    }

    /**
     * New empty parameter context.
     * */
    public ParameterContext(Attribute parentTextAttribute) {
        this.attributeId = parentTextAttribute.getId();
        this.name = parentTextAttribute.getName();
        this.order = parentTextAttribute.getOrdering();
        this.type = parentTextAttribute.getAttributeType();
        this.nullValue = true;
    }
}
