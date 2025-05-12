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

package org.qubership.atp.dataset.service.jpa.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.exception.visibilityarea.VisibilityAreaNameException;
import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.JpaDataSetListService;
import org.qubership.atp.dataset.service.jpa.JpaVisibilityAreaService;
import org.qubership.atp.dataset.service.jpa.delegates.VisibilityArea;
import org.qubership.atp.dataset.service.jpa.model.VisibilityAreaFlatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JpaVisibilityAreaServiceImpl implements JpaVisibilityAreaService {
    @Autowired
    protected ModelsProvider modelsProvider;
    @Autowired
    protected JpaDataSetListService dataSetListService;

    @Override
    @Transactional(readOnly = true)
    public List<VisibilityAreaFlatModel> getAll() {
        List<VisibilityAreaFlatModel> result = new ArrayList<>();
        List<VisibilityArea> all = modelsProvider.getAllVisibilityAreas();
        all.forEach(item -> result.add(new VisibilityAreaFlatModel(item)));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public VisibilityAreaFlatModel getFlatById(UUID visibilityAreaId) {
        VisibilityArea visibilityArea = modelsProvider.getVisibilityAreaById(visibilityAreaId);
        if (visibilityArea == null) {
            return null;
        }
        return new VisibilityAreaFlatModel(visibilityArea);
    }

    @Override
    @Transactional(readOnly = true)
    public VisibilityArea getById(UUID visibilityAreaId) {
        VisibilityArea visibilityArea = modelsProvider.getVisibilityAreaById(visibilityAreaId);
        if (visibilityArea == null) {
            return null;
        }
        return visibilityArea;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisibilityAreaFlatModel> getAllSortedByNameAsc() {
        List<VisibilityAreaFlatModel> result = new ArrayList<>();
        List<VisibilityArea> all = modelsProvider.getAllVisibilityAreasOrderedByNameAsc();
        all.forEach(item -> result.add(new VisibilityAreaFlatModel(item)));
        return result;
    }

    @Override
    @Transactional
    public void deleteById(UUID visibilityAreaId) {
        modelsProvider.getVisibilityAreaById(visibilityAreaId).remove();
    }

    @Override
    @Transactional
    public UUID getOrCreateWithName(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new VisibilityAreaNameException();
        }
        VisibilityArea visibilityArea = modelsProvider.getVisibilityAreaByName(name);
        if (visibilityArea != null) {
            return visibilityArea.getId();
        }
        return modelsProvider.createVisibilityArea(name).getId();
    }

    @Override
    @Transactional
    public VisibilityArea replicate(UUID id, String name) throws DataSetServiceException {
        if (StringUtils.isEmpty(name)) {
            throw new DataSetServiceException("Visibility area name can't be null or empty");
        }
        return modelsProvider.replicate(id, name);
    }

    @Override
    @Transactional
    public boolean setName(UUID visibilityAreaId, String newName) {
        if (StringUtils.isEmpty(newName)) {
            throw new VisibilityAreaNameException();
        }
        VisibilityArea visibilityArea = modelsProvider.getVisibilityAreaById(visibilityAreaId);
        if (visibilityArea == null) {
            return false;
        }
        visibilityArea.setName(newName);
        return true;
    }

    @Override
    public void copyDataSetListsToVisibilityArea(UUID sourceAreaId, UUID targetAreaId) {
        VisibilityArea visibilityArea = modelsProvider.getVisibilityAreaById(sourceAreaId);
        dataSetListService.copyDataSetLists(visibilityArea.getDataSetListIds(), true, targetAreaId, null, null, null);
    }
}
