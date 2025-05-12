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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.ContentTypes;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.poifs.filesystem.Ole10Native;
import org.apache.poi.poifs.filesystem.Ole10NativeException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.qubership.atp.dataset.migration.model.FalloutReport;
import org.qubership.atp.dataset.model.impl.file.FileData;

import com.google.common.base.Strings;

public class AttachedFiles {

    /**
     * Search for file attachments from a XSSFWorkbook.
     */
    public static List<Ole10Native> createAttachedFiles(XSSFWorkbook book, FalloutReport report) {
        try {
            List<PackagePart> parts = book.getAllEmbedds();
            List<Ole10Native> files = new ArrayList<>(parts.size());
            parts.forEach(part -> {
                try {
                    files.add(
                            Ole10Native.createFromEmbeddedOleObject(
                                    new POIFSFileSystem(part.getInputStream())
                            )
                    );
                } catch (Ole10NativeException e) {
                    report.report("XSSFWorkbook", " ",
                            "Invalid or unexcepted data format", e.getMessage());
                } catch (IOException e) {
                    report.report("XSSFWorkbook", " ", "", e.getMessage());
                }
            });
            return files;
        } catch (OpenXML4JException e) {
            report.report("XSSFWorkbook", " ",
                    "Error when searching for attached files", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Search for a file in the list by attribute name and dataset name.
     *
     * @param name   Attribute name.
     * @param dsName Dataset name.
     */
    public static Optional<Ole10Native> findFileFromList(List<Ole10Native> attachedFiles, String name, String dsName) {
        Optional<Ole10Native> fileAttach = attachedFiles.stream().filter(
                file -> {
                    String filePath = file.getFileName();
                    String type = file.getLabel().replaceAll(".*\\.", "");
                    return filePath.startsWith(dsName) && filePath.endsWith(name + '.' + type);
                }
        ).findFirst();
        if (fileAttach.isPresent()) {
            return fileAttach;
        } else {
            return attachedFiles.stream().filter(
                    file -> {
                        String fileName = file.getLabel().replaceAll("\\..*", "");
                        return fileName.equals(name);
                    }
            ).findFirst();
        }
    }

    /**
     * Creation FileData.
     *
     * @param uuid Parameter UUID.
     */
    public static FileData createFileParameterFromAttachedFile(Ole10Native file, UUID uuid) {
        String fileName = file.getLabel();
        String contentType = ContentTypes.getContentTypeFromFileExtension(fileName);
        return new FileData(fileName, uuid, Strings.isNullOrEmpty(contentType) ? "plain/text" : contentType);
    }
}
