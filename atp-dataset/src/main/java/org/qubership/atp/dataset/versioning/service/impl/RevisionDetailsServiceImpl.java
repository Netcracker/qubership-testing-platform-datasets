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

package org.qubership.atp.dataset.versioning.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.javers.core.Javers;
import org.javers.repository.jql.JqlQuery;
import org.javers.repository.jql.QueryBuilder;
import org.javers.shadow.Shadow;
import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.exception.EntityNotFoundException;
import org.qubership.atp.dataset.service.rest.dto.versioning.UiManDataSetListJDto;
import org.qubership.atp.dataset.versioning.model.domain.DataSetListSnapshot;
import org.qubership.atp.dataset.versioning.model.ui.DataSetListUiModel;
import org.qubership.atp.dataset.versioning.service.RevisionDetailsService;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class RevisionDetailsServiceImpl implements RevisionDetailsService {

    private final Javers javers;
    private final ModelsProvider modelsProvider;

    @Override
    public UiManDataSetListJDto getRevisionDetails(Integer revision, UUID entityId) {
        log.debug("RevisionDetailsServiceImpl#getRevisionDetails(revision={}, entityId={})", revision, entityId);
        Optional<Shadow<DataSetListSnapshot>> entity = getShadow(revision, entityId);
        Shadow<DataSetListSnapshot> shadow = entity.orElseThrow(() -> new EntityNotFoundException(
                String.format("Failed to found shadow with id=%s and revision=%s", entityId, revision)));
        UiManDataSetListJDto revisionDetails = buildResponse(shadow);
        log.debug("Revision details: {}", revisionDetails);
        return revisionDetails;
    }

    private UiManDataSetListJDto buildResponse(Shadow<DataSetListSnapshot> entity) {
        DataSetListSnapshot model = entity.get();
        log.debug("Shadow: {}", model);
        DataSetListUiModel uiModel = new DataSetListUiModel(model, modelsProvider);
        return new UiManDataSetListJDto()
                .author(entity.getCommitMetadata().getAuthor())
                .name(uiModel.getName())
                .dataSets(Lists.newArrayList(uiModel.getDataSets()))
                .attributes(Lists.newArrayList(uiModel.getAttributes()));
    }

    private Optional<Shadow<DataSetListSnapshot>> getShadow(Integer revision, UUID entityId) {
        JqlQuery query = getQuery(revision, entityId);
        List<Shadow<DataSetListSnapshot>> shadows = javers.findShadows(query);
        return shadows.stream().findFirst();
    }

    private JqlQuery getQuery(Integer revision, UUID entityId) {
        return QueryBuilder
                .byInstanceId(entityId, DataSetListSnapshot.class)
                .withScopeCommitDeep()
                .withVersion(Long.valueOf(revision))
                .build();
    }
}
