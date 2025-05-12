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

package org.qubership.atp.dataset.macros.args;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.macros.Position;

import com.google.common.collect.Lists;

public abstract class DotSeparatedArgsParser implements ArgsParser {

    private final String delimeter;
    private final int delimeterLength;
    private final MacroArgsFactory factory;
    protected State state = new State();

    protected DotSeparatedArgsParser(MacroArgsFactory factory) {
        this(".", factory);
    }

    private DotSeparatedArgsParser(String delimeter, MacroArgsFactory factory) {
        this.delimeter = delimeter;
        this.delimeterLength = delimeter.length();
        this.factory = factory;
    }

    protected abstract SignatureArg createArg(int index, @Nonnull MacroArgFactory args) throws Exception;

    @Override
    public void append(CharSequence text) {
        state.tail.append(text);
    }

    @Override
    public void clear() {
        state.reset();
    }

    @Override
    public Result tryParse() {
        State backup = this.state.copy();
        try {
            backup.tryParse();
            this.state = backup;
        } catch (Exception e) {
            this.state.error = e;
        }
        return this.state;
    }

    @Override
    public String toString() {
        return state.toString();
    }

    @Override
    public Result parseToTheEnd() {
        tryParse();
        if (state.tail.length() == 0 || state.error != null) {
            state.fullyParsed = true;
            return state;
        }
        try {
            state.parseTail();
            state.fullyParsed = true;
        } catch (Exception e) {
            state.error = e;
        }
        return state;
    }

    private class State extends MacroArgFactoryImpl implements Result {

        protected final List<SignatureArg> parsed;
        final StringBuilder tail;
        protected Exception error;
        protected int offset;
        int curArgIndex;
        boolean fullyParsed;

        State() {
            this(new StringBuilder(),
                    Lists.newArrayListWithExpectedSize(3),
                    null, 0, 0, false);
        }

        State(StringBuilder tail, List<SignatureArg> parsed,
              Exception error, int offset, int curArgIndex, boolean fullyParsed) {
            super(factory);
            this.tail = tail;
            this.parsed = parsed;
            this.error = error;
            this.offset = offset;
            this.curArgIndex = curArgIndex;
            this.fullyParsed = fullyParsed;
        }

        State copy() {
            return new State(new StringBuilder(tail), new ArrayList<>(parsed), error, offset, curArgIndex, fullyParsed);
        }

        void reset() {
            tail.setLength(0);
            parsed.clear();
            error = null;
            offset = 0;
            curArgIndex = 0;
        }

        void tryParse() throws Exception {
            for (int delimIdx = tail.indexOf(delimeter);
                 delimIdx != -1; delimIdx = tail.indexOf(delimeter)) {
                SignatureArg arg = createArg(curArgIndex++,
                        withText(new Position(offset, offset + delimIdx), tail.substring(0, delimIdx)));
                parsed.add(arg);
                int end = delimIdx + delimeterLength;
                tail.delete(0, end);
                offset += end;
            }
        }

        void parseTail() throws Exception {
            SignatureArg arg = createArg(curArgIndex++,
                    withText(new Position(offset, offset + tail.length()), tail.toString()));
            tail.setLength(0);
            parsed.add(arg);
        }

        @Override
        public Optional<Exception> getError() {
            return Optional.ofNullable(error);
        }

        @Override
        public Optional<String> getUnparsed() {
            return tail.length() == 0 ? Optional.empty() : Optional.of(tail.toString());
        }

        @Override
        public List<SignatureArg> getParsed() {
            return parsed;
        }

        @Override
        public String toString() {
            Stream<String> args = parsed.stream().map(MacroArg::getText);
            if (!fullyParsed) {
                args = Stream.concat(args, Stream.of(tail.toString()));
            }
            return args.collect(Collectors.joining(DotSeparatedArgsParser.this.delimeter));
        }
    }
}
