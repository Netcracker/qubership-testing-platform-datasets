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

package org.qubership.atp.dataset.service.direct.helper;

import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.service.direct.AliasWrapperService;
import org.qubership.atp.dataset.service.direct.AttributeService;
import org.qubership.atp.dataset.service.direct.DataSetListService;
import org.qubership.atp.dataset.service.direct.DataSetService;
import org.qubership.atp.dataset.service.direct.FilterService;
import org.qubership.atp.dataset.service.direct.GridFsService;
import org.qubership.atp.dataset.service.direct.ParameterService;
import org.qubership.atp.dataset.service.direct.TestPlanService;
import org.qubership.atp.dataset.service.direct.VisibilityAreaService;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractServicesInjected {

    @Autowired
    protected AliasWrapperService wrapperService;
    @Autowired
    protected DataSetService dataSetService;
    @Autowired
    protected DataSetListService dataSetListService;
    @Autowired
    protected AttributeService attributeService;
    @Autowired
    protected ParameterService parameterService;
    @Autowired
    protected VisibilityAreaService visibilityAreaService;
    @Autowired
    protected FilterService filterService;
    @Autowired
    protected GridFsService gridFsService;
    @Autowired
    protected TestPlanService testPlanService;
    @Autowired
    protected ModelsProvider modelsProvider;
}
