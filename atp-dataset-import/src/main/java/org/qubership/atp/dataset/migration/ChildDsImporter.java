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

package org.qubership.atp.dataset.migration;

import java.util.Iterator;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.qubership.atp.dataset.migration.formula.model.EvaluationContext;
import org.qubership.atp.dataset.migration.formula.model.TransformationException;
import org.qubership.atp.dataset.migration.model.ImportResources;
import org.qubership.atp.dataset.migration.model.OverlapParamContainer;
import org.qubership.atp.dataset.migration.model.ParamContainer;
import org.qubership.atp.dataset.migration.model.ToCreate;
import org.qubership.atp.dataset.migration.model.ToOverlap;
import org.qubership.atp.dataset.migration.repo.DsServicesFacade;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

public class ChildDsImporter {

    private static final Logger LOG = LoggerFactory.getLogger(ChildDsImporter.class);

    private final ImportResources res;
    private final DataSetList dsl;

    public ChildDsImporter(ImportResources res,
                           DataSetList dsl) {
        this.res = res;
        this.dsl = dsl;
    }

    /**
     * create children datasets.
     *
     * @param resources see {@link ImportResources#create(DsServicesFacade, String, String, String,
     *                  String)}.
     * @param dslName   - where to create child datasets
     */
    public static ChildDsImporter create(ImportResources resources, String dslName) {
        DataSetList dataSetList = resources.services.getDslByNameOrCreate(resources.va, dslName);
        return new ChildDsImporter(resources, dataSetList);
    }

    public static void process(ImportResources resources, String dslName, Boolean rootAttr) {
        create(resources, dslName).process(rootAttr);
    }

    /**
     * Imports all sheets from the supplied excel into {@link DataSet}'s.
     */
    public void process(Boolean rootAttr) {
        for (Sheet sheet1 : res.book) {
            EvaluationContext.getContext().clear();//because context is applicable only for the concrete sheet
            final XSSFSheet sheet = (XSSFSheet) sheet1;
            try {
                //get or create new dataset from parent
                final Row headerRow = sheet.rowIterator().next();
                LOG.info("Processing sheet: " + sheet.getSheetName());
                //one column = one dataset
                for (int columnIndex = 2; columnIndex < headerRow.getLastCellNum(); columnIndex++) {
                    final String datasetName = res.excelEvaluator.getValue(headerRow.getCell(columnIndex));
                    if (!datasetName.isEmpty()) { //skip empty dataset
                        LOG.info("Processing data set: " + datasetName);
                        processChildDataSet(SheetDataIterator.create(sheet, columnIndex, res.excelEvaluator),
                                datasetName, rootAttr);
                    }
                }
                EvaluationContext.getContext().evaluateAndFlushAll(res.excelEvaluator, res.services,
                        res.falloutReport, res.book);
            } catch (Exception e) {
                LOG.error("FAILED TO PROCESS SHEET : " + sheet.getSheetName() + " from file", e);
                res.falloutReport.report(sheet.getSheetName(), "-", "failed to process sheet", e.getMessage());
            }
        }
        res.close();
    }

    private void processChildDataSet(Iterator<DsRow> sheetData, String dataSetName, Boolean rootAttr) {
        DataSet childDataSet = res.services.get_DS_ByNameOrCreate(dataSetName, dsl);//to create parameters in
        ChildDsParamContainer paramContainer = ChildDsParamContainer.create(childDataSet);
        Group group = null;
        while (sheetData.hasNext()) {
            DsRow row = sheetData.next();
            String groupName = row.getGroup();
            if (row.gotNewGroup() && !(rootAttr && "default".equals(groupName))) {
                //find attribute with
                //find attribute in parent DS
                // if parent param: > ref_DS
                // then child  param: > ref_DS
                try {
                    group = Group.create(res.services, res.va, dsl,
                            childDataSet, groupName, res.groupDataSetName);
                } catch (TransformationException e) {
                    res.falloutReport.report(row.getCurrentLocationInfo(),
                            groupName, "child group error", e.getMessage());
                    //move to next group - wait while group name is changed
                    group = null;
                    continue;
                }
            }
            if (row.getParameterKey() != null) {
                if (rootAttr && "default".equals(groupName)) {
                    // Create param for each DS and go to the next row
                    DsParameterSup paramSup = new DsParameterSup(paramContainer, row.getParameterKey());
                    final Cell cell = row.getParameterValueCell();
                    EvaluationContext.getContext().put(res.excelEvaluator, cell, paramSup);
                } else if (group != null) {
                    //  for each row in a group:
                    //  get attribute
                    //          if not found - report and read to next attribute ?!
                    //  get attribute value
                    //  if value is TEXT - override
                    //  if value is FORMULA and value == PARENT value (reference to parent value) - nothing to do - next
                    //  if formula and diff from parent - calculate formula and set as value (random / uuid / etc)
                    String attributeInGroupName = row.getParameterKey();
                    try {
                        ChildParameterSup parameterSup = ChildParameterSup.create(group, attributeInGroupName);
                        final Cell paramValueCell = row.getParameterValueCell();
                        EvaluationContext.getContext().put(res.excelEvaluator, paramValueCell, parameterSup);
                    } catch (Exception e) {
                        res.falloutReport.report(row.getCurrentLocationInfo(),
                                groupName, "parameter in group error", e.getMessage());
                    }
                }
            }
        }
    }


    static class ChildDsParamContainer implements ParamContainer {
        private final DataSet ds;

        private ChildDsParamContainer(DataSet ds) {
            this.ds = ds;
        }

        static ChildDsParamContainer create(DataSet ds) {
            return new ChildDsParamContainer(ds);
        }

        @Override
        public DataSet getGroupDs() {
            return ds;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("ds", ds)
                    .toString();
        }
    }

    static class DsParameterSup extends ToCreate {
        private final ChildDsParamContainer paramContainer;
        private final String key;

        private DsParameterSup(ChildDsParamContainer paramContainer, String key) {
            this.paramContainer = paramContainer;
            this.key = key;
        }

        @Nonnull
        @Override
        public ParamContainer getContainer() {
            return paramContainer;
        }

        @Nonnull
        @Override
        public String getAttrName() {
            return key;
        }
    }

    static class Group implements OverlapParamContainer {

        private final DataSet groupDs;
        private final Parameter refToDs;

        private Group(DataSet groupDs, Parameter refToDs) {
            this.groupDs = groupDs;
            this.refToDs = refToDs;
        }

        static Group create(DsServicesFacade services, VisibilityArea va, DataSetList dsl, DataSet ds,
                            String groupDslName, String groupDsName) throws TransformationException {
            //eager initialization
            DataSetList groupDsl = services.getDslByName(va, groupDslName)
                    .orElseThrow(() -> new TransformationException(
                            "Group data set list '" + groupDslName + "' is not found"));
            DataSet groupDs = services.getDsByName(groupDsl, groupDsName)
                    .orElseThrow(() -> new TransformationException(
                            "Default data set is not found in Data set list '" + groupDslName + "'"));
            Attribute refToDsl = services.get_Attr_ByNameOrCreate(dsl,
                    groupDslName, AttributeType.DSL, groupDsl.getId(), null);
            Parameter refToDs = services.get_Param_ByNameOrCreate(ds, refToDsl, groupDs.getId());
            return new Group(groupDs, refToDs);
        }

        @Override
        public Parameter getRefToDs() {
            return refToDs;
        }

        @Override
        public DataSet getGroupDs() {
            return groupDs;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("groupDs", groupDs)
                    .add("refToDs", refToDs)
                    .toString();
        }
    }

    static class ChildParameterSup extends ToOverlap {

        private final Group parent;
        private final Parameter parentParamInGroup;

        private ChildParameterSup(Group parent, Parameter parentParamInGroup) {
            this.parent = parent;
            this.parentParamInGroup = parentParamInGroup;
        }

        static ChildParameterSup create(Group parent, String key) throws TransformationException {
            //eager initialization
            Attribute attributeToBeOverridden = parent.getGroupDsl().getAttributes().stream()
                    .filter(a -> a.getName().equals(key))
                    .findFirst()
                    .orElseThrow(() ->
                            new TransformationException("Attribute is not found '" + key + "' "
                                    + "under group '" + parent.getGroupDsl().getName() + "'"));
            UUID id = attributeToBeOverridden.getId();
            Parameter parentParamInGroup = parent.getGroupDs().getParameters().stream()
                    .filter(p -> p.getAttribute().getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new TransformationException("Parameter is not found for attribute '"
                            + key + "' under parent group dataset: " + parent.getGroupDs().getName()));
            return new ChildParameterSup(parent, parentParamInGroup);
        }

        @Nonnull
        @Override
        public OverlapParamContainer getContainer() {
            return parent;
        }

        @Override
        public Parameter getParameterToOverlap() {
            return parentParamInGroup;
        }
    }
}
