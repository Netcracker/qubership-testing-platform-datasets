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

package org.qubership.atp.dataset.service;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.crypt.api.Decryptor;
import org.qubership.atp.crypt.api.Encryptor;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.model.impl.DataSetListImpl;
import org.qubership.atp.dataset.service.direct.helper.AbstractServicesInjected;
import org.qubership.atp.dataset.service.direct.helper.CreationFacade;
import org.qubership.atp.dataset.service.direct.helper.DbCreationFacade;
import org.qubership.atp.dataset.service.direct.impl.ClearCacheServiceImpl;
import org.qubership.atp.dataset.utils.MutableSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "jdbc.leak.detection.threshold=10",
        "atp-dataset.last.revision.count=200",
        "atp-dataset.archive.job.bulk-delete-count=1000",
        "atp-dataset.archive.cron.expression=0 0 0 * * ?",
        "atp-dataset.archive.job.name=atp-dataset-archive-job",
        "atp-dataset.archive.job.page-size=50",
        "atp-dataset.archive.job.thread.max-pool-size=5",
        "atp-dataset.archive.job.thread.core-pool-size=5",
        "atp-dataset.archive.job.thread.queue-capacity=100"
})
public abstract class AbstractTest extends AbstractServicesInjected {

    @Autowired
    protected DbCreationFacade factory;
    protected VisibilityArea va;
    @MockBean
    protected Encryptor encryptor;
    @MockBean
    protected Decryptor decryptor;
    @MockBean
    ClearCacheServiceImpl clearCacheService;
    @Nonnull
    private static DataSetList getDataSetListByName(@Nonnull Collection<DataSetList> container, @Nonnull String name) {
        return container.stream()
                .filter(dataSetList -> name.equals(dataSetList.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("DataSetList with name [" + name + "] was not found"));
    }

    protected VisibilityArea createTestData(Function<CreationFacade, VisibilityArea> data) {
        clearTestData();
        this.va = data.apply(factory);
        return this.va;
    }

    protected <T extends Supplier<? extends VisibilityArea>> T createTestDataInstance(
            Function<CreationFacade, T> data) {
        MutableSupplier<T> testDataSup = MutableSupplier.create();
        createTestData(facade -> {
            testDataSup.set(data.apply(facade));
            return testDataSup.get().get();
        });
        return testDataSup.get();
    }

    protected DataSetList getDataSetListByName(String name) {
        if (va != null) {
            return getDataSetListByName(va.getDataSetLists(), name);
        } else {
            return getDataSetListByName(dataSetListService.getAll(), name);
        }
    }

    protected void clearTestData() {
        if (va != null) {
            visibilityAreaService.delete(va.getId());
        }
    }

    @AfterEach
    public final void after() {
        clearTestData();
    }

    protected List<DataSetListImpl> getResponse(String context) throws JSONException {
        List<String> json = getElements(context);
        return json.stream()
                .map(this::deserialize)
                .collect(Collectors.toList());
    }

    protected List<String> getElements(String context) throws JSONException {
        JSONArray jsonArray = new JSONArray(context);
        List<String> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            list.add(jsonArray.get(i).toString());
        }
        return list;
    }

    private DataSetListImpl deserialize(String context) {
        Gson gson = new GsonBuilder().registerTypeAdapter(Timestamp.class, new TimestampDeserializer()).create();
        return gson.fromJson(context, new TypeToken<DataSetListImpl>() {
        }.getType());
    }

    private static class TimestampDeserializer implements JsonDeserializer<Timestamp> {

        @Override
        public Timestamp deserialize(JsonElement element, Type type,
                                     JsonDeserializationContext context) throws JsonParseException {
            return element == null ? null : new Timestamp(element.getAsLong());
        }
    }
}
