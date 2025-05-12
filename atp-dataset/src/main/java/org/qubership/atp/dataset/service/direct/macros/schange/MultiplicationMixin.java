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

package org.qubership.atp.dataset.service.direct.macros.schange;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.qubership.atp.dataset.db.DataSetListRepository;
import org.qubership.atp.dataset.db.DataSetRepository;
import org.qubership.atp.dataset.db.utils.Proxies;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.impl.AttributeImpl;
import org.qubership.atp.dataset.model.impl.DataSetImpl;
import org.qubership.atp.dataset.model.impl.MixInIdImpl;
import org.qubership.atp.dataset.model.impl.ParameterImpl;

public class MultiplicationMixin extends TraverseMixinSupport {
    private final List<MultiplicationEntry> entries;
    private final DataSetList dsl;
    private final DataSet ds;
    private final DataSetListRepository dslRepo;
    private final DataSetRepository dsRepo;

    private MultiplicationMixin(List<MultiplicationEntry> entries,
                                DataSetListRepository dslRepo,
                                DataSetRepository dsRepo,
                                DataSetList dsl,
                                DataSet ds) {
        this.entries = entries;
        this.dsl = dsl;
        this.ds = ds;
        this.dslRepo = dslRepo;
        this.dsRepo = dsRepo;
    }

    public static DataSet create(List<MultiplicationEntry> entries,
                                 DataSetListRepository dslRepo,
                                 DataSetRepository dsRepo,
                                 DataSetList dsl,
                                 DataSet ds) {
        MultiplicationMixin traverseProxy = new MultiplicationMixin(entries, dslRepo, dsRepo, dsl, ds);
        return traverseProxy.proxy(ds);
    }

    @Override
    public Attribute proxy(Attribute of) {
        Optional<Attribute> replaceWith = entries.stream()
                .filter(entry -> entry.getKey().targetAttribute.getId().equals(of.getId()))
                .findAny()
                .map(entry -> new AttributeImpl(of.getId(), of.getName(), of.getDataSetList(),
                        AttributeType.DSL,
                        Proxies.withId(DataSetList.class, entry.getKey().dataSetListId,
                                dslRepo::getById),
                        null, null));
        return super.proxy(replaceWith.orElse(of));
    }

    @Override
    public Parameter proxy(Parameter of) {
        Optional<Parameter> replaceWith = entries.stream()
                .filter(entry -> entry.getKey().targetParameter.getId().equals(of.getId()))
                .findAny()
                .map(entry -> new ParameterImpl(of.getId(), proxy(of.getAttribute()), of.getDataSet(),
                        null, null, Proxies.withId(DataSet.class,
                        entry.getValue(), dsRepo::getById)));
        return super.proxy(replaceWith.orElse(of));
    }

    @Override
    public DataSet proxy(DataSet of) {
        if (!ds.equals(of)) {
            return of;
        }
        String metadata = entries.stream().map(entry -> entry.getValue().toString()).collect(Collectors.joining());
        return super.proxy(new DataSetImpl(new MixInIdImpl(ds.getId(), metadata), ds.getName(),
                dsl, ds.getParameters(), ds.getLabels(), ds.isLocked()));
    }
}
