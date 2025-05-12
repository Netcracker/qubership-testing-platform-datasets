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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mockito;

import org.qubership.atp.dataset.migration.model.Settings;
import org.qubership.atp.dataset.migration.repo.DsServicesFacade;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.service.direct.helper.SimpleCreationFacade;

@Isolated
public class DataSetImporterTest {
    static final DsServicesFacade SERVICES = Mockito.mock(DsServicesFacade.class);
    static final VisibilityArea VA = SimpleCreationFacade.INSTANCE.va("va");
    static final DataSetList DSL = SimpleCreationFacade.INSTANCE.dsl(VA, "dsl");
    static final DataSet DS = SimpleCreationFacade.INSTANCE.ds(DSL, "DS");
    static final DataSetList GROUP_DSL = SimpleCreationFacade.INSTANCE.dsl(VA, "default");
    static final DataSet GROUP_DS = SimpleCreationFacade.INSTANCE.ds(GROUP_DSL, "DEFAULT");
    static final String EXCEL_FOLDER = "TEST_DATA/";
    static final int TIMEOUT_SECONDS = 9999999;
    static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();


    static void doImport(Settings settings) throws IOException, InvalidFormatException {
        DataSetImporter dataSetImporter = new DataSetImporter(settings, SERVICES);
        dataSetImporter.run();
    }

    static void doImport(@Nullable String parentFileName,
                         @Nullable String childFileName,
                         boolean rootAttr) throws IOException, InvalidFormatException {
        doImport(new Settings(EXCEL_FOLDER, parentFileName, childFileName,
                VA.getName(), DSL.getName(), GROUP_DS.getName(), Objects.toString(rootAttr)));
    }

    /**
     * #ToDo
     */
    static void doImportWithTimeout(@Nullable String parentFileName,
                                    @Nullable String childFileName,
                                    boolean rootAttr) {
        Future<Object> result = EXECUTOR.submit(() -> {
            doImport(parentFileName, childFileName, rootAttr);
            return null;
        });
        try {
            result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Can not wait of import to be ended in " + TIMEOUT_SECONDS + " seconds."
                    + " It is probably stuck", e);
        } catch (ExecutionException ee) {
            throw new RuntimeException("Import ended with exception", ee.getCause());
        } catch (InterruptedException | CancellationException e) {
            throw new RuntimeException("Can not wait of import to be ended - it is interrupted/cancelled", e);
        } finally {
            result.cancel(true);
        }
    }

    @Nonnull
    private static List<FalloutRecord> getFallout(@Nonnull String filename) throws IOException {
        return Files.readAllLines(Paths.get(EXCEL_FOLDER).resolve(filename), Charset.forName("UTF-8")).stream()
                .skip(1)//header
                .map(FalloutRecord::create)
                .collect(Collectors.toList());
    }

    @BeforeEach
    public void setUp() throws Exception {
        Mockito.reset(SERVICES);
        when(SERVICES.get_VA_ByNameOrCreate(VA.getName())).thenReturn(VA);
        when(SERVICES.getDslByNameOrCreate(VA, DSL.getName())).thenReturn(DSL);
        when(SERVICES.getDslByNameOrCreate(VA, GROUP_DSL.getName())).thenReturn(GROUP_DSL);
        when(SERVICES.get_DS_ByNameOrCreate(eq(DS.getName()), eq(DSL))).thenReturn(DS);
        when(SERVICES.get_DS_ByNameOrCreate(eq(GROUP_DS.getName()), eq(GROUP_DSL))).thenReturn(GROUP_DS);
    }

    @Test
    public void doImport_ParentWithRootAttributes_NothingIsCreated() throws Exception {
        doImportWithTimeout("OneRowDefaultGroup.xlsx", null, true);
        verify(SERVICES, never()).getDslByNameOrCreate(any(), any());
        verify(SERVICES, never()).get_DS_ByNameOrCreate(any(), any());
        verify(SERVICES, never()).get_Attr_ByNameOrCreate(any(), any(), any(), any(), any());
    }

    @Test
    public void doImport_ParentWithGroup_GroupDsAndParameterAreCreated() throws Exception {
        Attribute attr = SimpleCreationFacade.INSTANCE.textAttr(GROUP_DSL, "atpMacro");
        when(SERVICES.get_Attr_ByNameOrCreate(eq(GROUP_DSL), eq(attr.getName()), any(), any(), any())).thenReturn(attr);
        doImportWithTimeout("OneRowDefaultGroup.xlsx", null, false);
        verify(SERVICES, times(1))
                .getDslByNameOrCreate(eq(VA), eq(GROUP_DSL.getName()));
        verify(SERVICES, times(1))
                .get_DS_ByNameOrCreate(eq(GROUP_DS.getName()), eq(GROUP_DSL));
        verify(SERVICES, times(1))
                .get_Attr_ByNameOrCreate(eq(GROUP_DSL), eq("atpMacro"), any(), any(), any());
        verify(SERVICES, times(1))
                .get_Param_ByNameOrCreate(eq(GROUP_DS), eq(attr), any());
    }

    @Test
    public void doImport_ChildWithRootAttributes_ParameterIsCreatedAndGroupIsNot() throws Exception {
        Attribute attr = SimpleCreationFacade.INSTANCE.textAttr(DSL, "atpMacro");
        when(SERVICES.get_Attr_ByNameOrCreate(eq(DSL), eq(attr.getName()), any(), any(), any())).thenReturn(attr);
        doImportWithTimeout(null, "OneRowDefaultGroup.xlsx", true);
        verify(SERVICES, never()).getDslByNameOrCreate(eq(VA), eq(GROUP_DSL.getName()));
        verify(SERVICES, times(1))
                .getDslByNameOrCreate(eq(VA), eq(DSL.getName()));
        verify(SERVICES, times(1))
                .get_DS_ByNameOrCreate(eq(DS.getName()), eq(DSL));
        verify(SERVICES, times(1))
                .get_Attr_ByNameOrCreate(eq(DSL), eq("atpMacro"), any(), any(), any());
        verify(SERVICES, times(1))
                .get_Param_ByNameOrCreate(eq(DS), eq(attr), any());
    }

    @Test
    public void doImport_ChildWithNonExistingGroup_GroupIsSkippedAndProcessIsNotStuck() throws Exception {
        when(SERVICES.getDslByName(any(), any())).thenReturn(Optional.empty());
        doImportWithTimeout(null, "OneRowDefaultGroup.xlsx", false);
        List<FalloutRecord> fallout = getFallout("OneRowDefaultGroup.xlsx.fallout.tsv");
        Assertions.assertEquals(1, fallout.size());
        Assertions.assertEquals("PARENT_DSL|A2", fallout.get(0).location);
    }

    @Test
    public void doImport_AtpParentAndChild_AtpMacroGoesIntoFalloutReport() throws Exception {
        when(SERVICES.get_Param_ByNameOrCreate(any(), any(), any())).thenThrow(new RuntimeException("Test exception"));
        doImportWithTimeout("OneRowDefaultGroup.xlsx", "OneRowDefaultGroup.xlsx", true);
        List<FalloutRecord> fallout = getFallout("OneRowDefaultGroup.xlsx.fallout.tsv");
        Assertions.assertEquals(1, fallout.size());
        Assertions.assertEquals("PARENT_DSL!C3", fallout.get(0).location);
    }

    private static class FalloutRecord {
        private final String location;
        private final String problemPlace;
        private final String shortMessage;
        private final String detailedMessage;

        private FalloutRecord(String location, String problemPlace, String shortMessage, String detailedMessage) {
            this.location = location;
            this.problemPlace = problemPlace;
            this.shortMessage = shortMessage;
            this.detailedMessage = detailedMessage;
        }

        private static FalloutRecord create(@Nonnull String row) {
            String[] split = Arrays.copyOf(row.split("\\t"), 4);
            return new FalloutRecord(split[0], split[1], split[2], split[3]);
        }
    }
}
