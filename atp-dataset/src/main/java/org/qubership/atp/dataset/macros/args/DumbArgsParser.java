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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.qubership.atp.dataset.macros.Position;
import org.springframework.stereotype.Component;

@Component
public class DumbArgsParser implements ArgsParser, ArgsParser.Result {
    private final StringBuilder tail = new StringBuilder();
    private TextArg.WithSignature parsed = null;

    @Override
    public void append(CharSequence text) {
        tail.append(text);
    }

    @Override
    public void clear() {
        tail.setLength(0);
        parsed = null;
    }

    @Override
    public Result tryParse() {
        return this;
    }

    @Override
    public Result parseToTheEnd() {
        parsed = TextArg.of(new Position(0, tail.length()), tail.toString());
        tail.setLength(0);
        return this;
    }

    @Override
    public Optional<Exception> getError() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getUnparsed() {
        return tail.length() == 0 ? Optional.empty() : Optional.of(tail.toString());
    }

    @Override
    public List<SignatureArg> getParsed() {
        return parsed == null ? Collections.emptyList() : Collections.singletonList(parsed);
    }

    @Override
    public String toString() {
        return parsed == null ? tail.toString() : parsed.getText();
    }
}
