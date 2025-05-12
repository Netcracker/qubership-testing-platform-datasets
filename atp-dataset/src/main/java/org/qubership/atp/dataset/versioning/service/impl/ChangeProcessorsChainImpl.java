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

import org.javers.core.diff.Diff;
import org.qubership.atp.dataset.service.rest.dto.versioning.HistoryItemDto;
import org.qubership.atp.dataset.versioning.service.ChangeProcessorsChain;
import org.qubership.atp.dataset.versioning.service.changes.DataSetListComparable;
import org.qubership.atp.dataset.versioning.service.changes.processors.ChangeProcessor;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChangeProcessorsChainImpl implements ChangeProcessorsChain {

    private ChangeProcessor firstChangeProcessor;

    /**
     * Constructor.
     * Sets next processor for each {@link ChangeProcessor} for building a chain.
     */
    public ChangeProcessorsChainImpl(List<ChangeProcessor> changeProcessors) {
        if (changeProcessors.isEmpty()) {
            throw new IllegalArgumentException("There are no ChangeProcessors");
        }
        firstChangeProcessor = changeProcessors.get(0);
        for (int i = 0; i < changeProcessors.size() - 1; i++) {
            ChangeProcessor changeProcessor = changeProcessors.get(i);
            changeProcessor.setNextProcessor(changeProcessors.get(i + 1));
        }
    }

    @Override
    public HistoryItemDto proceed(Diff diff,
                                           DataSetListComparable oldEntity,
                                           DataSetListComparable actualEntity) {
        return firstChangeProcessor.proceed(diff, oldEntity, actualEntity);
    }
}
