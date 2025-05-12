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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qubership.atp.dataset.migration.formula.model.CellData;
import org.qubership.atp.dataset.migration.formula.model.EvaluationContext;
import org.qubership.atp.dataset.migration.formula.model.FormulaType;
import org.qubership.atp.dataset.migration.formula.model.ParameterAssociation;
import org.qubership.atp.dataset.migration.formula.model.TransformationException;

public class InternalReferenceParser extends RegexpFormulaAdapter {

    private static final String REFERENCE_REGEX = "[a-zA-Z]+\\d+";
    private static final Pattern justReference = Pattern.compile(REFERENCE_REGEX);

    @Override
    public FormulaType getType() {
        return FormulaType.REFERENCE;
    }

    @Override
    public String transform(CellData cellData) throws TransformationException {
        Matcher matcher = getMatcher(cellData);
        String cellAddress = matcher.group();
        EvaluationContext context = EvaluationContext.getContext();
        ParameterAssociation referencedParameter = context.get(cellData);
        if (referencedParameter == null) { //for case when reference is nowhere
            throw new TransformationException("reference '" + cellAddress + "' links to nowhere");
        }
        return getAddress(referencedParameter);
    }

    private String getAddress(ParameterAssociation referencedParameter) {
        //IN SCOPE: DSL_NAME.DATA_SET_NAME.ATTRIBUTE_NAME
        return String.format("#REF_DSL(%s.%s.%s)",
                referencedParameter.parameterSup.getContainer().getGroupDsl().getName(),
                referencedParameter.parameterSup.getContainer().getGroupDs().getName(),
                referencedParameter.parameterSup.getAttrName());
    }

    @Override
    protected Pattern getPattern() {
        return justReference;
    }

    @Override
    protected String getFormulaDescription() {
        return REFERENCE_REGEX + " (excel link to the another cell, e.g. =C11)";
    }
}
