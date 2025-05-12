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

package org.qubership.atp.dataset.db;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.db.utils.StrongIdentifiedCache;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.service.AbstractTest;
import org.qubership.atp.dataset.service.direct.helper.CreationFacade;

@Isolated
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfiguration.class})
@TestPropertySource(properties = {
        "atp-dataset.javers.enabled=false"
})
public class DataSetListTreeRepositoryTest extends AbstractTest {

    @Autowired
    DataSetListTreeRepository repo;

    @Test
    public void loadDslTree_WithDsRefOverlap_EachGroupIsLazyLoaded() throws Exception {
        TestData d = createTestDataInstance(TestData::new);
        IdentifiedCache cache = new StrongIdentifiedCache();
        DataSetListTreeRepository.ToLoadLater loadLater = new DataSetListTreeRepository.ToLoadLater(repo, cache);
        DataSetList dsl = repo.getDslTree(d.dslTop.getId(), ImmutableList.of(d.dsTop.getId()), loadLater);
        Assertions.assertEquals(d.dslTop, dsl);
        Attribute topIntoMiddle = cache.getIfPresent(Attribute.class, d.topIntoMiddle.getAttribute().getId());
        DataSetList dslMiddle = topIntoMiddle.getDataSetListReference();
        Assertions.assertEquals(d.erTop, cache);
        //will load 2nd level
        dslMiddle.getName();
        Assertions.assertEquals(d.dslMiddle, dslMiddle);
        Parameter middleIntoLowOverlap = cache.getIfPresent(Parameter.class, d.middleIntoLowOverlap.getId());
        DataSet dsLowOverlap = middleIntoLowOverlap.getDataSetReference();
        Assertions.assertEquals(d.erMiddle, cache);
        //will load 3rd level
        dsLowOverlap.getName();
        Assertions.assertEquals(d.dsLowOverlap, dsLowOverlap);
        Assertions.assertEquals(d.erLow, cache);
    }

    private static class TestData implements Supplier<VisibilityArea> {

        final VisibilityArea va;
        final DataSetList dslTop;
        final DataSet dsTop;
        final Parameter topIntoMiddle;
        final DataSetList dslMiddle;
        final DataSet dsMiddle;
        final Parameter middleIntoLow;
        final Parameter middleIntoLowOverlap;
        final DataSetList dslLow;
        final DataSet dsLowDefault;
        final DataSet dsLowOverlap;
        final IdentifiedCache erTop;
        final IdentifiedCache erMiddle;
        final IdentifiedCache erLow;

        public TestData(CreationFacade create) {
            va = create.va("ATPII-1877");
            dslTop = create.dsl(va, "Top");
            dsTop = create.ds(dslTop, "Default");
            dslMiddle = create.dsl(va, "Middle");
            dsMiddle = create.ds(dslMiddle, "Default");
            dslLow = create.dsl(va, "Low");
            dsLowDefault = create.ds(dslLow, "Default");
            dsLowOverlap = create.ds(dslLow, "Overlap");
            Attribute someAttr = create.textAttr(dslLow, "SomeAttr");
            Parameter someParam1 = create.textParam(dsLowDefault, someAttr, "Some value");
            Parameter someParam2 = create.textParam(dsLowOverlap, someAttr, "Some value");
            middleIntoLow = create.refParam(dsMiddle, dsLowDefault);
            topIntoMiddle = create.refParam(dsTop, dsMiddle);
            middleIntoLowOverlap = create.overrideParam(dsTop, middleIntoLow.getAttribute(),
                    null, null, dsLowOverlap, null, topIntoMiddle.getAttribute());
            //top level loaded
            erTop = createErCache(Lists.newArrayList(dslTop),
                    Lists.newArrayList(dsTop),
                    Lists.newArrayList(topIntoMiddle.getAttribute()),
                    Lists.newArrayList(topIntoMiddle, middleIntoLowOverlap));
            //middle level loaded
            erMiddle = createErCache(Lists.newArrayList(dslTop, dslMiddle),
                    Lists.newArrayList(dsTop, dsMiddle),
                    Lists.newArrayList(topIntoMiddle.getAttribute(), middleIntoLow.getAttribute()),
                    Lists.newArrayList(topIntoMiddle, middleIntoLowOverlap, middleIntoLow));
            //low level loaded
            erLow = createErCache(Lists.newArrayList(dslTop, dslMiddle, dslLow),
                    Lists.newArrayList(dsTop, dsMiddle, dsLowDefault, dsLowOverlap),
                    Lists.newArrayList(topIntoMiddle.getAttribute(), middleIntoLow.getAttribute(), someAttr),
                    Lists.newArrayList(topIntoMiddle, middleIntoLowOverlap, middleIntoLow, someParam1, someParam2));

        }

        private static IdentifiedCache createErCache(@Nonnull Collection<DataSetList> dsls,
                                                     @Nullable Collection<DataSet> dses,
                                                     @Nullable Collection<Attribute> attrs,
                                                     @Nullable Collection<Parameter> params) {
            return createErCache(dsls.stream().collect(Collectors.toMap(Identified::getId, v -> v)),
                    index(dses),
                    index(attrs),
                    index(params));
        }

        private static IdentifiedCache createErCache(@Nonnull Map<UUID, DataSetList> dsls,
                                                     @Nullable Map<UUID, DataSet> dses,
                                                     @Nullable Map<UUID, Attribute> attrs,
                                                     @Nullable Map<UUID, Parameter> params) {
            Map<Class<? extends Identified>, Map<UUID, ? extends Identified>> caches = new HashMap<>();
            caches.put(DataSetList.class, dsls);
            if (dses != null) {
                caches.put(DataSet.class, dses);
            }
            if (attrs != null) {
                caches.put(Attribute.class, attrs);
            }
            if (params != null) {
                caches.put(Parameter.class, params);
            }
            return new StrongIdentifiedCache(caches);
        }

        @Nullable
        private static <T extends Identified> Map<UUID, T> index(@Nullable Collection<T> i) {
            return Optional.ofNullable(i).map(d -> d.stream()
                    .collect(Collectors.toMap(Identified::getId, v -> v)))
                    .orElse(null);
        }

        @Override
        public VisibilityArea get() {
            return va;
        }

    }

}
