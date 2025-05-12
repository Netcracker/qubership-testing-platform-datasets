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

package org.qubership.atp.dataset.model.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.impl.FlatDataImpl;
import org.qubership.atp.dataset.model.utils.tree.AllRefsIterator;
import org.qubership.atp.dataset.model.utils.tree.Leaf;
import org.qubership.atp.dataset.model.utils.tree.LeafsWalker;
import org.qubership.atp.dataset.model.utils.tree.RefsVisitor;
import org.qubership.atp.dataset.service.direct.macros.DsEvaluator;
import org.qubership.atp.dataset.service.jpa.impl.DataSetParameterProvider;
import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManAttribute;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManDataSetList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;

public class Utils {
    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final Pattern uuidPattern = Pattern.compile(UUID_REGEX);

    public static final Joiner JOINER_DOT = Joiner.on('.');
    private static final Supplier<?> EMPTY_SUP = new Supplier<Object>() {

        @Override
        public String toString() {
            return "null supplier";
        }

        @Override
        public Object get() {
            return null;
        }
    };

    @Nonnull
    public static <T> Supplier<T> emptySup() {
        //noinspection unchecked
        return (Supplier<T>) EMPTY_SUP;
    }

    /**
     * To not to copy paste {@link Object#equals(Object)} logic.
     *
     * @param o1 object to compare with o2
     * @param o2 object to compare with o1
     * @return true if equals
     */
    public static boolean isEqual(@Nonnull Identified o1, @Nullable Object o2) {
        return isEqual(Identified.class, o1, o2);
    }

    /**
     * To not to copy paste {@link Object#equals(Object)} logic.
     *
     * @param clazz general superclass of o1 and o2
     * @param o1    object to compare with o2
     * @param o2    object to compare with o1
     * @param <T>   general superclass of o1 and o2
     * @return true if equals
     */
    public static <T extends Identified> boolean isEqual(@Nonnull Class<T> clazz,
                                                         @Nonnull T o1, @Nullable Object o2) {
        return o1 == o2 || isEqual(clazz, o1.getId(), o2);
    }

    /**
     * Checks target object has expected class and id.
     *
     * @param clazz  which should be extended by target
     * @param id     expected id of target
     * @param target checking object
     * @param <T>    type
     * @return true if target has expected type and id
     */
    @SuppressWarnings("SimplifiableIfStatement")
    public static <T extends Identified> boolean isEqual(@Nonnull Class<T> clazz,
                                                         @Nonnull Object id, @Nullable Object target) {
        if (target == null) {
            return false;
        }
        if (!clazz.isAssignableFrom(target.getClass())) {
            return false;
        }
        return Objects.equals(id, clazz.cast(target).getId());
    }

    /**
     * Makes Stream of root and all it's dependencies tree collected by {@link
     * Identified#getReferences()}.
     */
    @Nonnull
    public static Stream<Identified> allRefs(@Nonnull Identified root) {
        Iterator<Identified> allRefs = new AllRefsIterator<Identified>(Iterators.singletonIterator(root), true) {
            @Override
            protected Iterator<? extends Identified> getChildren(@Nonnull Identified parent) {
                return parent.getReferences().iterator();
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(allRefs, Spliterator.ORDERED), false);
    }

    /**
     * Takes the root object. Fills and returns {@link FlatDataImpl} with this object plus all the
     * dependencies tree collected by {@link Identified#getReferences()}.
     */
    @Nonnull
    public static FlatDataImpl doFlatData(@Nonnull Identified root) {
        FlatDataImpl result = new FlatDataImpl();
        allRefs(root).forEach(result);
        return result;
    }

    /**
     * Takes DatasetList and creates flat data from it.
     *
     * @param dataSetList root {@link DataSetList} object.
     * @return mapped data without duplicates as {@link FlatDataImpl}
     */
    public static FlatDataImpl doFlatData(@Nonnull DataSetList dataSetList) {
        FlatDataImpl flatData = new FlatDataImpl();
        flatData.setParameters(new LinkedHashSet<>());
        flatData.setDataSets(new LinkedHashSet<>());
        flatData.setAttributes(new LinkedHashSet<>());
        flatData.setDataSetLists(new LinkedHashSet<>());
        fillFlatData(flatData, dataSetList);
        return flatData;
    }

    /**
     * Creates {@link UiManDataSetList} decorator for provided {@link DataSetList}.
     */
    public static UiManDataSetList doUiDs(@Nonnull DataSetList dataSetList,
                                          @Nonnull DsEvaluator eval,
                                          boolean expandAll) {
        DslUiHandler dslUiHandler = new DslUiHandler(eval, expandAll);
        RefsVisitor<DataSetList, Attribute> visitor = new RefsVisitor<>(Iterators.singletonIterator(dataSetList),
                dslUiHandler, null);
        while (visitor.hasNext()) {
            visitor.next();
        }
        return dslUiHandler.getResult();
    }

    /**
     * Creates {@link UiManDataSetList} decorator for provided {@link DataSetList}.
     */
    public static UiManDataSetList doUiDs(@Nonnull DataSetList dataSetList,
                                          MacroContext macroContext,
                                          DataSetParameterProvider dataSetParameterProvider,
                                          DsEvaluator eval,
                                          boolean evaluate,
                                          boolean expandAll) {
        DslUiHandler dslUiHandler = new DslUiHandler(eval, macroContext, dataSetParameterProvider, evaluate, expandAll);
        RefsVisitor<DataSetList, Attribute> visitor = new RefsVisitor<>(Iterators.singletonIterator(dataSetList),
                dslUiHandler, null);
        while (visitor.hasNext()) {
            visitor.next();
        }
        return dslUiHandler.getResult();
    }

    /**
     * Finds attribute by provided path inclusive.
     *
     * @param attrPath should not be empty.
     */
    @Nullable
    public static UiManAttribute doUiAttr(@Nonnull DataSet dataSet,
                                          @Nonnull DsEvaluator eval,
                                          @Nonnull List<UUID> attrPath) {
        DsAttrPathUiHandler h = new DsAttrPathUiHandler(eval, attrPath);
        RefsVisitor<DataSet, Attribute> visitor = new RefsVisitor<>(Iterators.singletonIterator(dataSet), h, null);
        while (visitor.hasNext()) {
            visitor.next();
        }
        return h.getAttribute();
    }

    /**
     * See {@link Suppliers#memoize(com.google.common.base.Supplier)}.
     */
    @Nonnull
    public static <T> Supplier<T> memoize(@Nonnull Supplier<T> delegate) {
        return delegate instanceof MemoizingSupplier
                ? delegate
                : new MemoizingSupplier<>(Preconditions.checkNotNull(delegate));
    }

    /**
     * Creates a walker which is able to iterate over dataSets and parameters recursively. Handles
     * case when parameter references to another dataSet.
     *
     * @param root data set to find parameters in
     * @return walker which is able to iterate over data sets and parameters
     */
    public static Iterator<Leaf> paramsInDslWalker(DataSetList root, DsEvaluator evaluator) {
        return new LeafsWalker<>(root.getDataSets().iterator(), new DsParamHandler(evaluator), null);
    }

    /**
     * Creates a walker which is able to iterate over dataSets and parameters recursively. Handles
     * case when parameter references to another dataSet.
     *
     * @param root data set to find parameters in
     * @return walker which is able to iterate over data sets and parameters
     */
    public static Iterator<Leaf> paramsInDsWalker(DataSet root, DsEvaluator evaluator) {
        return new LeafsWalker<>(Iterators.singletonIterator(root), new DsParamHandler(evaluator), null);
    }

    /**
     * Creates a walker which is able to iterate over dataSetLists and attributes recursively.
     * Handles case when attribute references to another dataSetList.
     *
     * @param root data set list to find parameters in
     * @return walker which is able to iterate over data set lists and attributes
     */
    public static Iterator<List<String>> attributesInDslWalker(DataSetList root) {
        return new LeafsWalker<>(Iterators.singletonIterator(root), new DslAttrHandler(), null);
    }

    /**
     * Serializes dataSet in ITF way: {attribute = parameter, attribute2 : {attribute = parameter2}}
     * and so on.
     *
     * @param ds     root to serialize
     * @param mapper serialization factory
     * @return serializable json object
     */
    @Nonnull
    public static ObjectNode serializeInItfWay(@Nonnull DataSet ds,
                                               @Nonnull ObjectMapper mapper,
                                               @Nonnull DsEvaluator eval) {
        ObjectNode result = mapper.createObjectNode();
        Iterator<Leaf> walker = paramsInDsWalker(ds, eval);
        while (walker.hasNext()) {
            Leaf leaf = walker.next();
            List<String> pathLst = leaf.getPath();
            Preconditions.checkArgument(!pathLst.isEmpty(),
                    "Data set should not contain parameter without name (path to it)");
            String paramName = pathLst.remove(pathLst.size() - 1);//last sub path is a name of parameter
            //will recreate groups
            createGroupsPath(result, pathLst, mapper)
                    .set(paramName, mapper.convertValue(leaf.getLeaf(), JsonNode.class));
        }
        return result;
    }

    /**
     * Serializes dataSetLists attributes in ITF way: [attribute, attribute.attribute1] and so on.
     *
     * @param dsl    root to serialize
     * @param mapper serialization factory
     * @return serializable json array
     */
    @Nonnull
    public static ArrayNode serializeAttrInItfWay(@Nonnull DataSetList dsl,
                                                  @Nonnull ObjectMapper mapper) {
        ArrayNode result = mapper.createArrayNode();
        Iterator<List<String>> walker = attributesInDslWalker(dsl);
        while (walker.hasNext()) {
            result.add(JOINER_DOT.join(walker.next().iterator()));
        }
        return result;
    }

    /**
     * Makes combinations between elements in the different collections in the list.
     * https://stackoverflow.com/questions/32131987/how-can-i-make-cartesian-product-with-java-8-streams
     * . Consider using {@link com.google.common.collect.Lists#cartesianProduct} instead.
     */
    public static <T, A, R> Stream<R> combinations(@Nonnull List<? extends Collection<? extends T>> values,
                                                   @Nonnull Collector<T, A, R> collector) {
        if (values.isEmpty()) {
            return Stream.empty();
        }
        Function<A, R> finisher = collector.finisher();
        return combine(values, 0, null, collector.supplier(), collector.accumulator())
                .map(finisher);
    }

    /**
     * Chains prefix with next element from values on specified offset.
     *
     * @return chain in a form of collection, provided by supplier.
     */
    private static <T, A> Stream<A> combine(@Nonnull List<? extends Collection<? extends T>> values,
                                            int offset,
                                            @Nullable Prefix<T> prefix,
                                            @Nonnull Supplier<A> supplier,
                                            @Nonnull BiConsumer<A, T> accumulator) {
        if (offset == values.size() - 1) {
            return values.get(offset).stream()
                    .map(e -> new Prefix<>(prefix, e).addTo(supplier.get(), accumulator));
        }
        return values.get(offset).stream()
                .flatMap(e -> combine(values, offset + 1, new Prefix<>(prefix, e), supplier, accumulator));
    }

    /**
     * Returns json object, representing the deepest nesting group based on path provided. Creates
     * nested json objects if necessary. Expects that path representing json entities may not exist,
     * or be json objects only.
     *
     * @param root   the root container json object.
     * @param path   the names of inner groups hierarchy.
     * @param mapper json factory.
     * @return json object, representing the last deepest nested group in hierarchy.
     */
    @Nonnull
    private static ObjectNode createGroupsPath(
            @Nonnull ObjectNode root, @Nonnull List<String> path, @Nonnull ObjectMapper mapper) {
        if (path.isEmpty()) {
            return root;
        }
        ObjectNode target = root;
        for (String groupName : path) {
            JsonNode node = target.get(groupName);
            if (node == null) {
                ObjectNode newOne = mapper.createObjectNode();
                target.set(groupName, newOne);
                target = newOne;
            } else {
                target = (ObjectNode) node;
            }
        }
        return target;
    }

    private static void fillFlatData(FlatDataImpl flatData, DataSetList dsl) {
        flatData.getDataSets().addAll(dsl.getDataSets());
        List<Attribute> attributes = dsl.getAttributes();
        flatData.getAttributes().addAll(attributes);
        flatData.getDataSetLists().add(dsl);
        dsl.getDataSets().forEach(dataSet -> {
            flatData.getParameters().addAll(dataSet.getParameters());
            dataSet.getParameters().forEach(parameter -> {
                if (parameter.getDataSetReference() != null
                        && !flatData.getDataSets().contains(parameter.getDataSetReference())) {
                    fillFlatData(flatData, parameter.getDataSetReference().getDataSetList());
                }
            });
        });
    }

    /**
     * Makes a friendly message from the throwable.
     */
    @Nonnull
    public static StringBuilder appendFriendlyMessage(@Nonnull StringBuilder to, @Nonnull Throwable throwable) {
        if (throwable.getSuppressed().length == 0) {
            collectRootCauses(to, throwable);
        } else {
            int[] offset = {0};
            new AllRefsIterator<Throwable>(Iterators.singletonIterator(throwable), false) {
                @Nullable
                @Override
                protected Iterator<? extends Throwable> getChildren(@Nonnull Throwable parent) {
                    return Arrays.asList(parent.getSuppressed()).iterator();
                }

                @Override
                protected void forwardToNewParent(Throwable parent) {
                    offset[0]++;
                }

                @Override
                protected void backToPreviousParent() {
                    offset[0]--;
                }
            }.forEachRemaining(error -> {
                boolean isChild = false;
                for (int i = 0; i < offset[0]; i++) {
                    to.append("  ");
                    isChild = true;
                }
                if (isChild) {
                    to.append("|-");
                }
                collectRootCauses(to, error);
                to.append("\n");
            });
        }
        return to;
    }

    private static void collectRootCauses(@Nonnull StringBuilder to, @Nonnull Throwable throwable) {
        Joiner.on(": ")
                .appendTo(to, Throwables.getCausalChain(throwable).stream()
                        .map(Throwable::getMessage).distinct().iterator());
    }

    /**
     * Returns true if argument is UUID.
     * */
    public static boolean isUuid(Object value) {
        if (value instanceof UUID) {
            return true;
        } else if (value instanceof String) {
            return uuidPattern.matcher((String)value).matches();
        } else {
            return false;
        }
    }
}
