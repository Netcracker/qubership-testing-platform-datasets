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

import static org.qubership.atp.dataset.versioning.service.DataSetListSnapshotService.RESTORED_TO_PROPERTY;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.javers.core.Javers;
import org.javers.core.commit.CommitId;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.repository.jql.JqlQuery;
import org.javers.repository.jql.QueryBuilder;
import org.javers.shadow.Shadow;
import org.qubership.atp.dataset.constants.Constants;
import org.qubership.atp.dataset.service.rest.dto.versioning.ChangeSummary;
import org.qubership.atp.dataset.service.rest.dto.versioning.HistoryItemDto;
import org.qubership.atp.dataset.service.rest.dto.versioning.HistoryItemResponseDto;
import org.qubership.atp.dataset.service.rest.dto.versioning.PageInfoDto;
import org.qubership.atp.dataset.versioning.model.domain.DataSetListSnapshot;
import org.qubership.atp.dataset.versioning.service.JaversHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JaversHistoryServiceImpl implements JaversHistoryService {

    private final Javers javers;
    private final JaversHistoryCacheableService javersHistoryCacheableService;
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    @Autowired
    public JaversHistoryServiceImpl(Javers javers, JaversHistoryCacheableService javersHistoryCacheableService) {
        this.javers = javers;
        this.javersHistoryCacheableService = javersHistoryCacheableService;
    }

    @Override
    public HistoryItemResponseDto getAllHistory(UUID id, Integer offset, Integer limit) {
        log.debug("Retrieving history of DSL. ID: {}, offset: {}, limit: {}", id, offset, limit);
        HistoryItemResponseDto response = new HistoryItemResponseDto();
        response.setPageInfo(getPageInfo(id, offset, limit));
        response.setHistoryItems(createHistoryItems(id, offset, limit));
        log.debug("History retrieval completed. Response: {}", response);
        return response;
    }

    private List<HistoryItemDto> createHistoryItems(UUID id, Integer offset, Integer limit) {
        List<HistoryItemDto> historyItems = new ArrayList<>(limit);

        List<Shadow<DataSetListSnapshot>> shadows = javers.findShadows(getQuery(id, offset, limit));
        Optional<Shadow<DataSetListSnapshot>> beforeMainPack = getShadowBeforeMainPack(id, offset, limit);

        for (int i = 0; i < shadows.size(); i++) {
            Shadow<DataSetListSnapshot> shadow = shadows.get(i);
            Optional<Shadow<DataSetListSnapshot>> previousShadow;
            if (i == shadows.size() - 1) { // the last element of loop
                previousShadow = beforeMainPack;
            } else {
                previousShadow = Optional.of(shadows.get(i + 1));
            }
            log.trace("Comparing two DSL history shadows. Old: {}, new: {}", previousShadow, shadow);
            HistoryItemDto historyItemDto = processTwoShadows(id, shadow, previousShadow);
            if (isDataSetNameFound(historyItemDto)) {
                historyItems.add(historyItemDto);
            }
        }
        fillHistoryMetadata(id, historyItems);
        return historyItems;
    }

    private Optional<Shadow<DataSetListSnapshot>> getShadowBeforeMainPack(
            UUID id,
            Integer offset,
            Integer limit
    ) {
        List<Shadow<DataSetListSnapshot>> shadows = javers.findShadows(getQuery(id, offset + limit, 1));
        if (CollectionUtils.isEmpty(shadows)) {
            return Optional.empty();
        } else {
            return Optional.of(shadows.get(0));
        }
    }

    private HistoryItemDto processTwoShadows(
            UUID id,
            Shadow<DataSetListSnapshot> actualShadow,
            Optional<Shadow<DataSetListSnapshot>> oldShadow
    ) {
        HistoryItemDto historyItem;
        boolean isDslCreated = !oldShadow.isPresent();
        boolean isDlsDeleted = actualShadow.get().getId() == null;
        boolean isRestored = actualShadow.getCommitMetadata().getProperties().get(RESTORED_TO_PROPERTY) != null;
        if (isDslCreated) {
            historyItem = dslCreatedChanges();
        } else if (isDlsDeleted) {
            historyItem = dslDeletedChanges();
        } else if (isRestored) {
            historyItem = dslRestoredChanges(Integer.valueOf(
                    actualShadow.getCommitMetadata().getProperties().get(RESTORED_TO_PROPERTY)));
        } else {
            historyItem = javersHistoryCacheableService.compareTwoShadows(actualShadow, oldShadow.get());
        }
        historyItem.setCommitId(actualShadow.getCommitMetadata().getId().valueAsNumber());
        if (isDataSetNameFound(historyItem)) {
            historyItem.setModifiedWhen(actualShadow.getCommitMetadata().getCommitDate().format(DATE_FORMATTER));
            historyItem.setModifiedBy(actualShadow.getCommitMetadata().getAuthor());
        }
        return historyItem;
    }

    private boolean isDataSetNameFound(HistoryItemDto historyItemDto) {
        return !Constants.NOT_FOUND_NAME_DS.equalsIgnoreCase(historyItemDto.getDataSet());
    }

    private HistoryItemDto dslCreatedChanges() {
        HistoryItemDto historyItem = new HistoryItemDto();
        historyItem.setChangeSummary(ChangeSummary.DSL_ADDED.toString());
        log.trace("DSL was created at current history shadow.");
        return historyItem;
    }

    private HistoryItemDto dslDeletedChanges() {
        HistoryItemDto historyItem = new HistoryItemDto();
        historyItem.setChangeSummary(ChangeSummary.DSL_CHANGED.toString());
        log.trace("DSL was deleted at current history shadow.");
        return historyItem;
    }

    private HistoryItemDto dslRestoredChanges(Integer revisionId) {
        HistoryItemDto historyItem = new HistoryItemDto();
        historyItem.setChangeSummary(ChangeSummary.RESTORED.toString(revisionId));
        log.trace("DSL was restored to revision {}.", revisionId);
        return historyItem;
    }

    private void fillHistoryMetadata(UUID id, List<HistoryItemDto> historyItemDtoList) {
        List<BigDecimal> bigDecimals =
                historyItemDtoList.stream().map(HistoryItemDto::getCommitId)
                        .collect(Collectors.toList());
        List<CdoSnapshot> cdoSnapshots = getVersionOfShadows(id, bigDecimals);
        historyItemDtoList.forEach(historyItemDto -> {
            Optional<CdoSnapshot> snapshot = cdoSnapshots.stream()
                    .filter(cdoSnapshot -> cdoSnapshot.getCommitId().valueAsNumber()
                            .equals(historyItemDto.getCommitId())).findAny();
            snapshot.ifPresent(cdoSnapshot -> historyItemDto.setVersion((int) cdoSnapshot.getVersion()));
        });
    }

    private List<CdoSnapshot> getVersionOfShadows(UUID id, List<BigDecimal> commitIds) {
        JqlQuery query = getQuery(id, commitIds);
        return javers.findSnapshots(query);
    }

    private PageInfoDto getPageInfo(UUID id, Integer offset, Integer limit) {
        PageInfoDto pageInfo = new PageInfoDto();
        pageInfo.setOffset(offset);
        pageInfo.setLimit(limit);
        List<Shadow<Object>> shadows = javers.findShadows(getQuery(id));
        int countOfShadows = shadows.size();
        pageInfo.setItemsTotalCount(countOfShadows);
        return pageInfo;
    }

    private JqlQuery getQuery(UUID id, Integer offset, Integer limit) {
        return QueryBuilder.byInstanceId(id, DataSetListSnapshot.class)
                .withNewObjectChanges()
                .skip(offset)
                .limit(limit)
                .build();
    }

    private JqlQuery getQuery(UUID id, CommitId commitId) {
        return QueryBuilder.byInstanceId(id, DataSetListSnapshot.class)
                .withCommitId(commitId)
                .build();
    }

    private JqlQuery getQuery(UUID id, List<BigDecimal> commitId) {
        return QueryBuilder.byInstanceId(id, DataSetListSnapshot.class)
                .withCommitIds(commitId)
                .build();
    }

    private JqlQuery getQuery(UUID id) {
        return QueryBuilder.byInstanceId(id, DataSetListSnapshot.class)
                .withNewObjectChanges()
                .build();
    }
}
