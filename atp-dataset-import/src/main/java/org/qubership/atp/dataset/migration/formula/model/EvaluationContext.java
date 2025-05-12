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

package org.qubership.atp.dataset.migration.formula.model;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.poi.poifs.filesystem.Ole10Native;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.qubership.atp.dataset.migration.Progress;
import org.qubership.atp.dataset.migration.model.ExcelEvaluator;
import org.qubership.atp.dataset.migration.model.FalloutReport;
import org.qubership.atp.dataset.migration.model.ParameterData;
import org.qubership.atp.dataset.migration.model.ToCreate;
import org.qubership.atp.dataset.migration.model.ToOverlap;
import org.qubership.atp.dataset.migration.repo.DsServicesFacade;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.ParameterOverlap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvaluationContext {

    private static final Logger LOG = LoggerFactory.getLogger(EvaluationContext.class);

    private static final EvaluationContext thiz = new EvaluationContext();
    private final Map<String, ParameterAssociation> calculatedHash;

    private EvaluationContext() {
        calculatedHash = new HashMap<>();
    }

    public static EvaluationContext getContext() {
        return thiz;
    }

    //what if cell references to another sheet? - impossible because of InternalReferenceParser
    //but it is not strict solution - should be changed
    public static String getCellAddress(Cell cell) {
        return cell.getAddress().formatAsString();
    }

    /**
     * store data for future parameter calculation.
     *
     * @param excelEvaluator formulas calculator
     * @param cell           excel cell to be calculation
     * @param parameterSup   data set parameter data used to set value
     * @return parameter to be calculated
     */
    public ParameterAssociation put(ExcelEvaluator excelEvaluator, Cell cell, ParameterData parameterSup) {
        ParameterAssociation parameterAssoc = new ParameterAssociation(
                new CellData(excelEvaluator, cell),
                parameterSup);
        put(getCellAddress(cell), parameterAssoc);
        return parameterAssoc;
    }

    public void put(String address, ParameterAssociation parameter) {
        calculatedHash.put(address, parameter);
    }

    public ParameterAssociation get(CellData cellData) {
        return calculatedHash.get(cellData.getFormula());
    }

    public void clear() {
        calculatedHash.clear();
    }

    /**
     * Evaluates all {@link ParameterAssociation}'s currently in context by using {@code
     * evaluateWith}, imports them into ds service using {@code flushWith}.
     */
    public void evaluateAndFlushAll(ExcelEvaluator evaluateWith, DsServicesFacade flushWith,
                                    FalloutReport report, XSSFWorkbook book) {
        LOG.info("Flushing " + calculatedHash.size() + " items");
        final List<Ole10Native> files = AttachedFiles.createAttachedFiles(book, report);

        Progress progress = Progress.withTimeThreshold(percents -> LOG.info("Progress: {}%", percents),
                calculatedHash.size());
        Map<ToCreate, Map.Entry<Parameter, Formula>> paramsToSetValueTo = new LinkedHashMap<>();
        getParametersToBeCalculated().forEach(param -> {
            Formula formula = evaluateWith.getFormulaValue(param, files);
            if (param.parameterSup.isOverlap()) {
                overlapParameter(flushWith, report, param.parameterSup.toOverlap(), formula);
                progress.increment(1);
            } else {
                ToCreate target = param.parameterSup.toCreate();
                Optional<Parameter> created = createParameter(flushWith, report, target, formula);
                if (created.isPresent()) {
                    //we should create all parameters first to be able to set reference values between each other.
                    //see AliasWrapperServiceImpl
                    paramsToSetValueTo.put(target, new AbstractMap.SimpleEntry<>(created.get(), formula));
                    progress.increment(0.5);//will be additionally set
                } else {
                    progress.increment(1);//will not be additionally set
                }
            }
        });
        for (Map.Entry<ToCreate, Map.Entry<Parameter, Formula>> entry : paramsToSetValueTo.entrySet()) {
            ToCreate source = entry.getKey();
            Parameter target = entry.getValue().getKey();
            Formula value = entry.getValue().getValue();
            setParameterValue(flushWith, report, source, target, value);
            progress.increment(0.5);
        }
        LOG.info("Flushing done");
    }

    /**
     * Tries to overlap target {@link ToOverlap} with formula value {@link Formula} using {@link
     * DsServicesFacade}. All errors are going to {@link FalloutReport}.
     *
     * @param flushWith used to interact with ds service.
     * @param report    used to report errors into.
     * @param target    holds an info about target parameter to overlap.
     * @param value     reflects excel value - ds service value mapping.
     * @return {@link ParameterOverlap} if operation succeeded.
     */
    private Optional<ParameterOverlap> overlapParameter(DsServicesFacade flushWith, FalloutReport report,
                                                        ToOverlap target, Formula value) {
        try {
            ParameterOverlap result = value.getFormulaType().overlap.overlapParameter(flushWith, target, value);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            report.report(value.getLocation(),
                    target.getLocation() + "=" + value.getDatasetValue(),
                    e.getMessage(),
                    "parameter " + target
                            + " can not be overlapped with value " + value);
            return Optional.empty();
        }
    }

    /**
     * Tries to create parameter using ds service info from {@link ToCreate} and value type info
     * provided by {@link Formula}.
     *
     * @param flushWith used to interact with ds service.
     * @param report    used to report errors into.
     * @param source    holds an info about parameter container, name, location.
     * @param value     reflects excel value - ds service value mapping.
     * @return {@link Parameter} if operation succeeded.
     */
    private Optional<Parameter> createParameter(DsServicesFacade flushWith, FalloutReport report,
                                                ToCreate source, Formula value) {
        try {
            Parameter parameter = value.getFormulaType().createParam.createParameter(flushWith, source, value);
            return Optional.of(parameter);
        } catch (Exception e) {
            report.report(value.getLocation(),
                    source.getLocation() + "=" + value.getDatasetValue(),
                    e.getMessage(),
                    "parameter " + value + " can not be created");
            return Optional.empty();
        }
    }

    /**
     * Tries to set the value provided by {@link Formula} to already created {@link Parameter}.
     *
     * @param flushWith used to interact with ds service.
     * @param report    used to report errors into.
     * @param source    holds an info about parameter container, name, location.
     * @param target    parameter created previously by using provided source.
     * @param value     reflects excel value - ds service value mapping.
     * @return {@code true} if operation succeeded.
     */
    private boolean setParameterValue(DsServicesFacade flushWith, FalloutReport report,
                                      ToCreate source, Parameter target, Formula value) {
        try {
            value.getFormulaType().setParam.setParameterValue(flushWith, source, target, value);
            return true;
        } catch (Exception e) {
            report.report(value.getLocation(),
                    source.getLocation() + "=" + value.getDatasetValue(),
                    e.getMessage(),
                    "parameter value " + value
                            + " can not be set to parameter " + source);
            return false;
        }
    }

    public Stream<ParameterAssociation> getParametersToBeCalculated() {
        return calculatedHash.entrySet().stream()
                .map(Map.Entry::getValue);
    }
}
