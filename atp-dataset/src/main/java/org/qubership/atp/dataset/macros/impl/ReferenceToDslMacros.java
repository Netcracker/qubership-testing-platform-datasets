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

package org.qubership.atp.dataset.macros.impl;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.macros.EvaluationContext;
import org.qubership.atp.dataset.macros.Macros;
import org.qubership.atp.dataset.macros.args.ArgsParser;
import org.qubership.atp.dataset.macros.args.DotSeparatedArgsParser;
import org.qubership.atp.dataset.macros.args.MacroArg;
import org.qubership.atp.dataset.macros.args.MacroArgFactory;
import org.qubership.atp.dataset.macros.args.MacroArgsFactory;
import org.qubership.atp.dataset.macros.args.RefArg;
import org.qubership.atp.dataset.macros.args.SignatureArg;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.utils.OverlapItem;
import org.qubership.atp.dataset.model.utils.OverlapIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;

@Component
public class ReferenceToDslMacros extends Macros<String> {

    public static final String MACROS_DEFINITION = "REF_DSL";

    @Autowired
    private MacroArgsFactory args;

    public ReferenceToDslMacros() {
        super(MACROS_DEFINITION);
    }

    private static String evaluate(@Nonnull RefArg<DataSetList> dslArg,
                                   @Nonnull RefArg<DataSet> dsArg,
                                   @Nonnull List<RefArg<Attribute>> attrArgs,
                                   @Nonnull EvaluationContext context) throws Exception {
        ImmutableList<RefArg<?>> argsList = ImmutableList.<RefArg<?>>builder()
                .add(dslArg)
                .add(dsArg)
                .addAll(attrArgs)
                .build();
        context.resolveArguments(argsList);
        DataSetList dsl = dslArg.get();
        DataSet ds = dsArg.get();
        if (!ds.getDataSetList().equals(dsl)) {
            throw new Exception("Can not search for reference: no such data set found "
                    + argsList.stream().map(Objects::toString).collect(Collectors.joining(".")));
        }
        OverlapItem refItem = OverlapIterator.create(ds, attrArgs.get(attrArgs.size() - 1).get().getId(),
                attrArgs.stream()
                        .limit(attrArgs.size() - 1)
                        .map(refArg -> refArg.get().getId())
                        .collect(Collectors.toList()))
                .next();
        if (!refItem.isReachable()) {
            throw new Exception("Can not find reference "
                    + argsList.stream().map(Objects::toString).collect(Collectors.joining(".")));
        }
        OverlapItem.Reachable reachable = refItem.asReachable();
        Optional<Parameter> paramOpt = reachable.getParameter();
        if (!paramOpt.isPresent()) {
            return "";
        }
        Parameter parameter = paramOpt.get();
        Optional<String> valueOpt = reachable.getValue();
        if (!valueOpt.isPresent()) {
            return "";
        }
        String value = valueOpt.get();
        if (value.isEmpty()) {
            return value;
        }
        return context.evaluate(reachable, parameter, value);
    }

    @Override
    public String evaluate(Stream<? extends MacroArg> args, EvaluationContext context) throws Exception {
        Iterator<? extends MacroArg> iterator = args.iterator();
        MacroArg first = iterator.next();
        RefArg<DataSetList> dslArg = first.asRef()
                .flatMap(RefArg::asDsl)
                .orElseThrow(() -> new IllegalArgumentException("Expected dsl as 1st parameter, got: " + first));
        MacroArg second = iterator.next();
        RefArg<DataSet> dsArg = second.asRef()
                .flatMap(RefArg::asDs)
                .orElseThrow(() -> new IllegalArgumentException("Expected ds as 2nd parameter, got: " + second));
        List<RefArg<Attribute>> attrArgs = new LinkedList<>();
        for (int i = 3; iterator.hasNext(); i++) {
            MacroArg next = iterator.next();
            int itemIdx = i;
            RefArg<Attribute> attr = next.asRef()
                    .flatMap(RefArg::asAttr)
                    .orElseThrow(() -> new IllegalArgumentException("Expected attr as " + itemIdx
                            + " parameter, got: " + next));
            attrArgs.add(attr);
        }
        //does not take relative attr path into account
        try {
            //try to resolve by using relative attr path
            return context.executeInAnyContext(context.getStrictContexts(), (ctx) -> {
                List<RefArg<Attribute>> relativeAttrArgs = context
                        .relativizePathFromSharedToThis(ctx, attrArgs);
                return evaluate(dslArg, dsArg, relativeAttrArgs, ctx);
            });
        } catch (Exception e) {
            Iterator<? extends EvaluationContext> nonStrict = context.getNonStrictContexts();
            if (!nonStrict.hasNext()) {
                throw e;
            }
            try {
                //try to resolve by using absolute attr path
                return context.executeInAnyContext(nonStrict,
                        (ctx) -> evaluate(dslArg, dsArg, attrArgs, ctx));
            } catch (Exception e2) {
                e.addSuppressed(e2);
                throw e;
            }
        }
    }

    @Override
    public ArgsParser createArgsParser() {
        return new DotSeparatedArgsParser(args) {

            @Override
            protected SignatureArg createArg(int index, @Nonnull MacroArgFactory args) throws Exception {
                switch (index) {
                    case 0:
                        return args.dsl();
                    case 1:
                        return args.ds();
                    default:
                        return args.attr();
                }
            }
        };
    }

    @Override
    public boolean doCache() {
        return false;
    }
}
