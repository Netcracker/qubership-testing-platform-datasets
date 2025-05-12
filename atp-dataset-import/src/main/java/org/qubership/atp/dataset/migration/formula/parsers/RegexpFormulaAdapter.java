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

package org.qubership.atp.dataset.migration.formula.parsers;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.migration.formula.model.CellData;
import org.qubership.atp.dataset.migration.formula.model.ExcelFormulaAdapter;
import org.qubership.atp.dataset.migration.formula.model.TransformationException;

public abstract class RegexpFormulaAdapter implements ExcelFormulaAdapter {

    @Override
    public boolean matches(CellData cellData) {
        return getValidMatcher(cellData).isPresent();
    }

    @Nonnull
    protected final Matcher getMatcher(CellData cellData) throws TransformationException {
        final Optional<Matcher> matcher = getValidMatcher(cellData);
        if (!matcher.isPresent()) {
            throw new TransformationException(
                    "Text '" + cellData + "' is not supported expression: " + getFormulaDescription());
        }
        return matcher.get();
    }

    protected abstract Pattern getPattern();

    protected abstract String getFormulaDescription();

    /**
     * Creates matcher. Verifies it is valid.
     *
     * @return {@link Optional#empty()} if cellData is not supported.
     */
    private Optional<Matcher> getValidMatcher(CellData cellData) {
        Matcher matcher = getPattern().matcher(cellData.getFormula());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(matcher);
    }
}
