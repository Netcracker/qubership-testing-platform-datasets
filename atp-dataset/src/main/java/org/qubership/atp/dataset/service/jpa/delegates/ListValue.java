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

package org.qubership.atp.dataset.service.jpa.delegates;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.qubership.atp.dataset.db.jpa.entities.ListValueEntity;

public class ListValue extends AbstractObjectWrapper<ListValueEntity> {
    public ListValue(ListValueEntity entity) {
        super(entity);
    }

    public UUID getId() {
        return entity.getId();
    }

    public Attribute getAttribute() {
        return modelsProvider.getAttribute(entity.getAttribute());
    }

    public void setText(String text) {
        entity.setText(text);
    }

    public String getText() {
        return entity.getText();
    }

    public void setAttribute(UUID attributeId) {
        Attribute attribute = modelsProvider.getAttributeById(attributeId);
        entity.setAttribute(attribute.getEntity());
    }

    @Override
    public void beforeRemove() {
        Attribute attribute = getAttribute();
        List<Parameter> toDelete = new LinkedList<>();
        for (Parameter parameter : attribute.getParameters()) {
            if (Objects.nonNull(parameter.getListValue()) && parameter.getListValue().getId().equals(getId())) {
                toDelete.add(parameter);
            }
        }
        for (Parameter parameter : toDelete) {
            parameter.remove();
        }
    }
}
