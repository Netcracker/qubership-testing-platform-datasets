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

package org.qubership.atp.dataset.service.direct.impl;

import static java.util.Objects.isNull;

import java.sql.Timestamp;
import java.util.UUID;

import javax.annotation.Nullable;

import org.qubership.atp.dataset.service.direct.ConcurrentModificationService;
import org.qubership.atp.dataset.service.direct.DataSetListService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConcurrentModificationServiceImpl implements ConcurrentModificationService {

    private final DataSetListService dataSetListService;

    /**
     * See {@link ConcurrentModificationService#getHttpStatus(UUID, Long)}.
     */
    @Override
    public HttpStatus getHttpStatus(@Nullable UUID entityId, @Nullable Long modifiedWhen) {
        log.debug("ConcurrentModificationServiceImpl#getHttpStatus(entityId: {}, modifiedWhen: {})",
                entityId, modifiedWhen);
        if (isNull(entityId) || isNull(modifiedWhen)) {
            return HttpStatus.OK;
        }
        Timestamp entityModifiedWhen = dataSetListService.getModifiedWhen(entityId);
        log.debug("Entity modifiedWhen: {}", entityModifiedWhen);
        long entityModifiedWhenLong = entityModifiedWhen.getTime();
        log.debug("Entity entityModifiedWhenLong: {}", entityModifiedWhenLong);
        return entityModifiedWhenLong == modifiedWhen ? HttpStatus.OK : HttpStatus.IM_USED;
    }
}
