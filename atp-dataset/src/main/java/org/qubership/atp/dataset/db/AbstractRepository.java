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

package org.qubership.atp.dataset.db;

import org.qubership.atp.dataset.db.generated.QAttribute;
import org.qubership.atp.dataset.db.generated.QAttributeKey;
import org.qubership.atp.dataset.db.generated.QDataset;
import org.qubership.atp.dataset.db.generated.QDatasetLabel;
import org.qubership.atp.dataset.db.generated.QDatasetlist;
import org.qubership.atp.dataset.db.generated.QDatasetlistLabel;
import org.qubership.atp.dataset.db.generated.QFilterDsLabels;
import org.qubership.atp.dataset.db.generated.QFilterDslLabels;
import org.qubership.atp.dataset.db.generated.QFilters;
import org.qubership.atp.dataset.db.generated.QLabel;
import org.qubership.atp.dataset.db.generated.QListValues;
import org.qubership.atp.dataset.db.generated.QParameter;
import org.qubership.atp.dataset.db.generated.QTestPlan;
import org.qubership.atp.dataset.db.generated.QVisibilityArea;


public abstract class AbstractRepository {

    public static final QVisibilityArea VA = new QVisibilityArea("VA");
    public static final QDatasetlist DSL = new QDatasetlist("DSL");
    public static final QDataset DS = new QDataset("DS");
    public static final QAttribute ATTR = new QAttribute("ATTR");
    public static final QAttributeKey AK = new QAttributeKey("AK");
    public static final QParameter PARAM = new QParameter("PARAM");
    public static final QListValues LV = new QListValues("LIST_VALUES");
    public static final QLabel LABEL = new QLabel("LABEL");
    public static final QDatasetlistLabel DSLLABEL = new QDatasetlistLabel("DATASETLIST_LABEL");
    public static final QDatasetLabel DSLABEL = new QDatasetLabel("DATASET_LABEL");
    public static final QFilters FILTERS = new QFilters("FILTERS");
    public static final QFilterDslLabels FILTER_DSL_LABELS = new QFilterDslLabels("FILTER_DSL_LABELS");
    public static final QFilterDsLabels FILTER_DS_LABELS = new QFilterDsLabels("FILTER_DS_LABELS");
    public static final QTestPlan TEST_PLAN = new QTestPlan("TEST_PLAN");

    protected AbstractRepository() {
    }
}
