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

package org.qubership.atp.dataset.macros.parser;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

import org.qubership.atp.dataset.macros.MacroRegistry;

public class TokensIterator implements Iterator<Token> {

    private final ParsingState state;
    private final StringBuilder tail;
    private final Queue<Token> pushed = new ArrayDeque<>(2);

    /**
     * Iterates through macro tokens in macro.
     */
    public TokensIterator(MacroRegistry macroFinder, StringBuilder tail) {
        this.tail = tail;
        state = new ParsingState(macroFinder) {
            @Override
            protected void pushToken(Strategy strategy) {
                super.pushToken(strategy);
                pushed.add(strategy);
            }
        };
    }

    @Override
    public boolean hasNext() {
        return tail.length() > 0 || !pushed.isEmpty() || !state.currentStrategy.consumed;
    }

    @Override
    public Token next() {
        int i = 0;
        for (; pushed.isEmpty() && i < tail.length(); i++) {
            char chr = tail.charAt(i);
            state.read(chr);
        }
        if (i > 0) {
            tail.delete(0, i);
        }
        if (!pushed.isEmpty()) {
            return pushed.remove();
        }
        if (!state.currentStrategy.consumed) {
            state.currentStrategy.consumed = true;
            return state.currentStrategy;
        }
        throw new NoSuchElementException();
    }

    public void notifyReduce() {
        state.brackets.goDeeper();
    }

    public void reset() {
        pushed.clear();
        state.reset();
    }
}
