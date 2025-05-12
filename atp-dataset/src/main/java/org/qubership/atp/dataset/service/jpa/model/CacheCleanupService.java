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

package org.qubership.atp.dataset.service.jpa.model;

import org.qubership.atp.dataset.db.GridFsRepository;
import org.qubership.atp.dataset.service.jpa.impl.DataSetListContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CacheCleanupService {
    @Autowired
    protected DataSetListContextService dataSetListContextService;
    @Autowired
    protected MacroContextService macroContextService;
    @Autowired
    protected GridFsRepository gridFsRepository;

    /**
     * Method called when request finished.
     */
    public void cleanAllLocalThreadCache() {
        cleanDataSetListContextCache();
        cleanMacroContextCache();
        cleanGridFsServiceCache();
    }

    public void cleanMacroContextCache() {
        macroContextService.dropLocalThreadCache();
    }

    public void cleanDataSetListContextCache() {
        dataSetListContextService.dropLocalThreadCache();
    }

    public void cleanGridFsServiceCache() {
        gridFsRepository.dropLocalThreadCache();
    }
}
