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

import java.util.Objects;
import java.util.UUID;

import javax.persistence.EntityNotFoundException;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.Assertions;
import org.qubership.atp.dataset.db.jpa.entities.AbstractAttributeEntity;
import org.qubership.atp.dataset.db.jpa.entities.AttributeEntity;
import org.qubership.atp.dataset.db.jpa.entities.AttributeKeyEntity;
import org.qubership.atp.dataset.db.jpa.entities.ListValueEntity;
import org.qubership.atp.dataset.db.jpa.entities.ParameterEntity;
import org.qubership.atp.dataset.exception.file.FileDsNotFoundException;
import org.qubership.atp.dataset.model.impl.file.FileData;

public class Parameter extends AbstractObjectWrapper<ParameterEntity> {

    private static final long serialVersionUID = -7205269656851827042L;

    public Parameter(ParameterEntity entity) {
        super(entity);
    }

    public UUID getId() {
        return entity.getId();
    }

    public void setId(UUID id) {
        entity.setId(id);
    }

    public String getStringValue() {
        return entity.getStringValue();
    }

    public void setStringValue(String value) {
        entity.setStringValue(value);
        save(entity);
    }

    public UUID getFileValueId() {
        return entity.getFileValueId();
    }

    public FileData getFileData() {
        return gridFsService.getFileInfo(getId());
    }

    /**
     * Return FileData if file exists or null.
     */
    public FileData getFileDataIfExist() {
        try {
            return gridFsService.getFileInfo(getId());
        } catch (FileDsNotFoundException ex) {
            return null;
        }
    }

    public void setFileValueId(UUID fileValueId) {
        entity.setFileValueId(fileValueId);
        save(entity);
    }

    public DataSet getDataSet() {
        return modelsProvider.getDataSet(entity.getDataSet());
    }

    public UUID getDataSetId() {
        return entity.getDataSetId();
    }

    /**
     * Returns referenced DS or null.
     */
    public DataSet getDataSetReferenceValue() {
        UUID dataSetReferenceId = entity.getDataSetReferenceId();
        if (dataSetReferenceId == null) {
            return null;
        } else {
            return modelsProvider.getDataSetById(dataSetReferenceId);
        }
    }

    /**
     * Returns referenced DS id.
     */
    public UUID getDataSetReferenceId() {
        return entity.getDataSetReferenceId();
    }

    /**
     * Sets referenced DS id.
     */
    public void setDataSetReferenceId(UUID dataSetId) {
        entity.setDataSetReferenceId(dataSetId);
        save(entity);
    }

    /**
     * Returns parent attribute id.
     */
    public Attribute getAttribute() {
        AbstractAttributeEntity attribute = entity.getAttribute();
        if (attribute == null || attribute.isAttributeKey()) {
            return null;
        }
        if (attribute instanceof HibernateProxy) {
            HibernateProxy hibernateProxy = (HibernateProxy) attribute;
            LazyInitializer initializer = hibernateProxy.getHibernateLazyInitializer();
            return modelsProvider.getAttribute((AttributeEntity) initializer.getImplementation());
        } else {
            return modelsProvider.getAttribute(attribute);
        }
    }

    public boolean isOverlap() {
        AbstractAttributeEntity attribute = entity.getAttribute();
        return attribute == null || attribute.isAttributeKey();
    }

    public UUID getListValueId() {
        return Objects.isNull(entity.getListValue()) ? null : entity.getListValue().getId();
    }

    public AttributeKey getAttributeKey() {
        return modelsProvider.getAttributeKey(
                entityManager.find(
                        AttributeKeyEntity.class, entity.getAttribute().getId()
                )
        );
    }

    public UUID getAttributeId() {
        return entity.getAttribute().getId();
    }

    public ListValueEntity getListValue() {
        return entity.getListValue();
    }

    /**
     * Set list value UUID.
     */
    public void setListValueId(UUID listValueId) {
        if (listValueId == null) {
            entity.setListValue(null);
        } else {
            ListValueEntity listValue = entityManager.find(ListValueEntity.class, listValueId);
            entity.setListValue(listValue);
        }
        save(entity);
    }

    /**
     * Returns parameter value as string from field, depending it's type.
     */
    public String getParameterValueByType() {
        Attribute attribute = getAttribute();
        if (attribute == null) {
            attribute = getAttributeKey().getAttribute();
        }
        switch (attribute.getAttributeType()) {
            case CHANGE:
            case ENCRYPTED:
            case TEXT:
                return getStringValue();
            case FILE:
                return getId().toString();
            case DSL: {
                return getDataSetReferenceId() == null ? null : dataReferencedDataSetName();
            }
            case LIST:
                return getListValue() == null ? "" : getListValue().getText();
            default:
                return null;
        }
    }

    /**
     * Returns parameter value object from field, depending it's type.
     */
    public Object getParameterValueByTypeAsObject() {
        Attribute attribute = getAttribute();
        if (attribute == null) {
            attribute = getAttributeKey().getAttribute();
        }
        switch (attribute.getAttributeType()) {
            case CHANGE:
            case ENCRYPTED:
            case TEXT:
                return getStringValue();
            case FILE:
                try {
                    return getFileData().getFileName();
                } catch (FileDsNotFoundException ignore) {
                    return null;
                }
            case DSL: {
                return getDataSetReferenceId() == null ? null : getDataSetReferenceValue().getShortInfo();
            }
            case LIST:
                return getListValue() == null ? "" : getListValue().getText();
            default:
                return null;
        }
    }

    /**
     * Get parent DS name.
     */
    public String dataReferencedDataSetName() {
        String nativeQuery = "select name from dataset where id = :ds_id";
        try {
            return (String) entityManager.createNativeQuery(nativeQuery)
                    .setParameter("ds_id", getDataSetReferenceId())
                    .getSingleResult();
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /**
     * Sets data set.
     *
     * @param dataSetId the data set id
     */
    public void setDataSet(UUID dataSetId) {
        DataSet dataSet = modelsProvider.getDataSetById(dataSetId);
        Assertions.assertNotNull(dataSet, "Cannot find data set by id " + dataSetId);
        entity.setDataSet(dataSet.getEntity());
    }

    /**
     * Sets attribute.
     *
     * @param attributeId the attribute id
     */
    public void setAttribute(UUID attributeId) {
        AbstractObjectWrapper attribute = modelsProvider.getAttributeById(attributeId);
        if (attribute == null) {
            attribute = modelsProvider.getAttributeKeyById(attributeId);
        }
        Assertions.assertNotNull(attribute, "Cannot find attribute by id " + attributeId);
        entity.setAttribute((AbstractAttributeEntity) attribute.getEntity());
    }
}
