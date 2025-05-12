package org.qubership.atp.dataset.versioning.service.impl;

import org.javers.core.Javers;
import org.javers.core.diff.Diff;
import org.javers.shadow.Shadow;
import org.qubership.atp.dataset.constants.CacheEnum;
import org.qubership.atp.dataset.service.rest.dto.versioning.HistoryItemDto;
import org.qubership.atp.dataset.versioning.model.domain.DataSetListSnapshot;
import org.qubership.atp.dataset.versioning.service.ChangeProcessorsChain;
import org.qubership.atp.dataset.versioning.service.changes.DataSetListComparable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JaversHistoryCacheableService {

    private final Javers javers;

    private final ChangeProcessorsChain changeProcessorsChain;

    @Autowired
    public JaversHistoryCacheableService(Javers javers, ChangeProcessorsChain changeProcessorsChain) {
        this.javers = javers;
        this.changeProcessorsChain = changeProcessorsChain;
    }

    /**
     * Cacheable comparing of shadows.
     *
     * @param actualShadow actual version
     * @param oldShadow    old version
     * @return HistoryItemDto.
     */
    @Cacheable(value = CacheEnum.Constants.JAVERS_DIFF_CACHE,
            key = "#oldShadow.getCommitId() + '_' + #actualShadow.getCommitId()")
    public HistoryItemDto compareTwoShadows(Shadow<DataSetListSnapshot> actualShadow,
                                            Shadow<DataSetListSnapshot> oldShadow) {
        DataSetListComparable actualEntity = convertToComparable(actualShadow);
        DataSetListComparable oldEntity = convertToComparable(oldShadow);
        Diff diff = javers.compare(oldEntity, actualEntity);
        HistoryItemDto historyItem = changeProcessorsChain.proceed(diff, oldEntity, actualEntity);
        if (historyItem == null) {
            log.trace("Diff wasn't processed by any ChangeProcessor.");
            historyItem = new HistoryItemDto();
        }
        return historyItem;
    }

    private DataSetListComparable convertToComparable(Shadow<DataSetListSnapshot> shadow) {
        DataSetListSnapshot snapshot = shadow.get();
        return new DataSetListComparable(snapshot);
    }
}