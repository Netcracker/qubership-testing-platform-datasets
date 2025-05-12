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

import javax.annotation.Nonnull;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.qubership.atp.dataset.migration.formula.model.EvaluationContext;
import org.qubership.atp.dataset.migration.formula.model.ParameterAssociation;
import org.qubership.atp.dataset.migration.model.ImportResources;
import org.qubership.atp.dataset.migration.model.ParamContainer;
import org.qubership.atp.dataset.migration.model.ToCreate;
import org.qubership.atp.dataset.migration.repo.DsServicesFacade;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;

public class ParentDsImporter {

    private static final Logger LOG = LoggerFactory.getLogger(ParentDsImporter.class);

    private ImportResources res;

    public ParentDsImporter(ImportResources res) {
        this.res = res;
    }

    /**
     * create parent dataset.
     *
     * @param resources see {@link ImportResources#create(DsServicesFacade, String, String, String,
     *                  String)}.
     */
    public static ParentDsImporter create(ImportResources resources) {
        return new ParentDsImporter(resources);
    }

    public static void process(ImportResources resources, Boolean rootAttr) {
        create(resources).process(rootAttr);
    }

    /**
     * Imports first sheet from the supplied excel into {@link DataSet}.
     */
    public void process(Boolean rootAttr) {
        //lets load only parent:
        // WB = ?
        // Sheet = DSL
        // 1st column 'Entity' = DSL (as a group)
        // 2nd column 'Parameter' = Attribute
        //3 column (TEMPLATE (DATASET)) = DSs
        //cell in the 3rd column = Parameter
        //"TEST_DATA/test_data_1/Parent_SMALL.xlsx"));
        //logic is:
        //create VA
        //create  DSL (sheet name)
        //create new data set (TEMPLATE)
        //when group is changed:
        //  add group as a parameter to TEMPLATE DATA SET
        //  get or create DSL (as a group)
        //  get_or_create DS (as a group)
        //for each row in a group:
        //  get or create attribute
        //  set attribute value in DS (as a group)
        //  next group
        //  next step - child + formulas
        if (res.book.getNumberOfSheets() > 1) {
            res.falloutReport.report(res.bookName, "-", "parent book should has only ONE sheet", "");
        }
        final XSSFSheet sheet = res.book.getSheetAt(0);
        LOG.info("Processing sheet: " + sheet.getSheetName());
        try {
            processParentDataSet(SheetDataIterator.create(sheet, 2, res.excelEvaluator), rootAttr);
        } catch (Exception e) {
            LOG.error("FAILED TO PROCESS SHEET : " + sheet.getSheetName() + " from file: " + e);
            LOG.error(Throwables.getStackTraceAsString(e));
            res.falloutReport.report(sheet.getSheetName(), "-", "failed to process sheet", e.getMessage());
        }
        res.close();
    }

    /**
     * Fills {@link EvaluationContext} with {@link ParameterAssociation}'s.
     *
     * @param sheetData with parameters to cache.
     */
    private void processParentDataSet(Iterator<DsRow> sheetData, Boolean rootAttr) {
        Group group = null;
        EvaluationContext.getContext().clear();//to avoid errors when parent is loaded after something else
        if (rootAttr) {
            sheetData = Iterators.filter(sheetData, dsRow -> !"default".equals(dsRow.getGroup()));
        }
        while (sheetData.hasNext()) {
            DsRow row = sheetData.next();
            if (row.gotNewGroup()) {
                //create new dataset parameter
                group = Group.create(res.services, res.va, row.getGroup(), res.groupDataSetName);
            }
            if (row.getParameterKey() != null) {
                ParentParameterSup parameterSup = new ParentParameterSup(group, row.getParameterKey());
                final Cell cell = row.getParameterValueCell();
                //add all parameters to context without calculation
                EvaluationContext.getContext().put(res.excelEvaluator, cell, parameterSup);
            }
        }
        EvaluationContext.getContext().evaluateAndFlushAll(res.excelEvaluator, res.services,
                res.falloutReport, res.book);
    }

    static class Group implements ParamContainer {

        private final DataSet groupDs;

        private Group(DataSet groupDs) {
            this.groupDs = groupDs;
        }

        static Group create(DsServicesFacade services, VisibilityArea va, String groupDslName, String groupDsName) {
            //eager initialization
            DataSetList groupDsl = services.getDslByNameOrCreate(va, groupDslName);
            DataSet groupDs = services.get_DS_ByNameOrCreate(groupDsName, groupDsl);
            return new Group(groupDs);
        }

        @Override
        public DataSet getGroupDs() {
            return groupDs;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("groupDs", groupDs)
                    .toString();
        }
    }

    static class ParentParameterSup extends ToCreate {

        private final Group parent;
        private final String key;

        private ParentParameterSup(Group parent, String key) {
            this.parent = parent;
            this.key = key;
        }

        @Nonnull
        @Override
        public ParamContainer getContainer() {
            return parent;
        }

        @Nonnull
        @Override
        public String getAttrName() {
            return key;
        }
    }
}

