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

package org.qubership.atp.dataset.macros;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MacroRegistryImpl implements MacroRegistry {

    private final Map<String, Macros> macroses;

    @Autowired
    private List<Macros> macrosList;

    public MacroRegistryImpl() {
        this(new HashMap<>());
    }

    public MacroRegistryImpl(Map<String, Macros> macroses) {
        this.macroses = macroses;
    }

    @Override
    public boolean fullyEquals(String name) {
        name = name.toUpperCase();
        for (String macro : macroses.keySet()) {
            if (name.equals(macro)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean partiallyEquals(String to) {
        to = to.toUpperCase();
        for (String macro : macroses.keySet()) {
            if (macro.startsWith(to)) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public Macros getMacros(@Nonnull String key) {
        Macros macros = macroses.get(key.toUpperCase());
        assert macros != null;
        return macros;
    }

    @PostConstruct
    public void init() {
        this.macrosList.stream().forEach((x) -> this.macroses.put(x.getDefinition().toUpperCase(), x));
    }
}
