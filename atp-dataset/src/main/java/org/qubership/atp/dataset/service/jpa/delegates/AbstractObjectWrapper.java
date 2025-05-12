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

import java.io.Serializable;
import java.util.UUID;

import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.qubership.atp.dataset.db.jpa.Wrapper;
import org.qubership.atp.dataset.db.jpa.entities.AbstractUuidBasedEntity;

import lombok.Getter;

public abstract class AbstractObjectWrapper<T extends AbstractUuidBasedEntity> extends Wrapper implements Serializable {
    @Getter
    protected T entity;

    public AbstractObjectWrapper(T entity) {
        this.entity = entity;
    }

    /**
     * Delete entity with removal flag check.
     * */
    public void remove() {
        if (entity.isRemoved()) {
            return;
        }
        beforeRemove();
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
        entity.setRemoved(true);
    }

    /**
     * Performs entity save operation.
     */
    public void save(AbstractUuidBasedEntity entity) {
        if (!entityManager.isJoinedToTransaction()) {
            entityManager.joinTransaction();
        }
        Session session = entityManager.unwrap(Session.class);
        session.saveOrUpdate(entity);
        entityManager.flush();
    }

    /**
     * Performs entity save operation.
     */
    public void save() {
        save(entity);
    }

    /**
     * Performs entity insert operation with selected id.
     */
    public void insert(AbstractUuidBasedEntity entity, UUID id) {
        if (!entityManager.isJoinedToTransaction()) {
            entityManager.joinTransaction();
        }
        entity.setId(id);
        Session session = entityManager.unwrap(Session.class);
        session.replicate(entity, ReplicationMode.EXCEPTION);
    }

    public void beforeRemove() {

    }

    /**
     * Replicate.
     */
    public void replicate() {
        if (!entityManager.isJoinedToTransaction()) {
            entityManager.joinTransaction();
        }
        Session session = entityManager.unwrap(Session.class);
        session.replicate(entity, ReplicationMode.OVERWRITE);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof AbstractObjectWrapper) {
            AbstractObjectWrapper otherWrapper = (AbstractObjectWrapper) other;
            return entity.getId().equals(otherWrapper.entity.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return entity.hashCode();
    }
}
