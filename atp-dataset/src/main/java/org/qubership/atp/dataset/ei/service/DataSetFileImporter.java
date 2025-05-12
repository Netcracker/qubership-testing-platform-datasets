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

package org.qubership.atp.dataset.ei.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.qubership.atp.dataset.db.GridFsRepository;
import org.qubership.atp.dataset.model.impl.file.FileData;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSetFileImporter {

    private static final String DESCRIPTOR_FILE_NAME_REGEXP =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.json";
    public static final String DELIMITER = "__";
    public static final int UUID_LENGTH = 36;
    private final ObjectLoaderFromDiskService objectLoaderFromDiskService;
    private final GridFsRepository gridFsRepoProvider;

    /**
     * Import files.
     *
     * @param workDir the work dir
     */
    public void importFiles(Path workDir, ExportImportData importData) throws IOException {
        log.info("start importFiles(workDir: {})", workDir);
        Map<UUID, Path> list = getFileDescriptors(workDir, "files");
        log.debug("importFiles list: {}", list);
        list.forEach((id, path) -> {
            log.debug("importFiles start import id: {}", id);
            FileData fileData;
            if (importData.isCreateNewProject() || importData.isInterProjectImport()) {
                Map<UUID, UUID> map = new HashMap<>(importData.getReplacementMap());
                fileData = objectLoaderFromDiskService.loadFileAsObjectWithReplacementMap(path, FileData.class, map);
                //fix url in case parameter uuid was changed by replacement map
                fileData.setUrl("/attachment/" + fileData.getParameterUuid());
            } else {
                fileData = objectLoaderFromDiskService.loadFileAsObject(path, FileData.class);
            }

            log.debug("importFiles import fileData: {}", fileData);
            if (fileData == null || fileData.getParameterUuid() == null) {
                log.info("Selected file is no a file descriptor. Path {}", path.toString());
                return;
            }
            String lettersName = fileData.getFileName();
            String fileName = Files.exists(path.getParent().resolve(lettersName)) ? lettersName : id.toString();
            checkName(fileName);
            try (InputStream inputStream = Files.newInputStream(path.getParent().resolve(fileName))) {
                gridFsRepoProvider.save(fileData, inputStream);
            } catch (Exception e) {
                log.info("Cannot read file from disk. File Data {}", fileData);
            }
        });
        log.info("end importFiles()");
    }

    private void checkName(String fileName) {
        if (fileName.contains(DELIMITER) && fileName.indexOf(DELIMITER) == UUID_LENGTH) {
            throw new RuntimeException(
              "Can not complete Import process due to the old zip archive version. Use the latest Datasets version.");
        }
    }

    private Map<UUID, Path> getFileDescriptors(Path workDir, String folderName) {
        Path dirWithObjects = workDir.resolve(folderName);

        log.debug("start getListOfObjectIdByFolder(dirWithObjects: {})", dirWithObjects);
        Map<UUID, Path> res = new HashMap<>();
        try (Stream<Path> result = Files.find(dirWithObjects, 5,
                (path, basicFileAttributes) -> basicFileAttributes.isRegularFile() && isDescriptorFileName(path))) {
            result.forEach(pathToFile -> {
                UUID objectId;
                try {
                    objectId = UUID.fromString(pathToFile.getFileName().toString().split("\\.")[0]);
                } catch (IllegalArgumentException e) {
                    log.warn("Can't get uuid from filename.", e);
                    return;
                }
                res.put(objectId, pathToFile);
            });
        } catch (Exception e) {
            log.error("Cannot find dir {}", dirWithObjects, e);
        }
        log.debug("end getListOfObjectIdByFolder(): {}", res);
        return res;
    }

    private boolean isDescriptorFileName(Path path) {
        return path.getFileName().toString().matches(DESCRIPTOR_FILE_NAME_REGEXP);
    }
}
