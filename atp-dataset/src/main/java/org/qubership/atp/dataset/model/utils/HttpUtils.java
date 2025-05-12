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

package org.qubership.atp.dataset.model.utils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

import org.qubership.atp.dataset.exception.file.FileExcelExportNotFoundException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

public class HttpUtils {

    /**
     * Building response entity with a file for downloading.
     *
     * @param file        formed file
     * @param contentType file type
     * @return response entity
     * @throws FileExcelExportNotFoundException in case if file is missing
     */
    public static ResponseEntity<InputStreamResource> buildFileResponseEntity(File file, String contentType) {
        ResponseEntity<InputStreamResource> response;
        try {
            ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                    .filename(file.getName(), StandardCharsets.UTF_8)
                    .build();
            response = ResponseEntity.ok()
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Disposition")
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(new InputStreamResource(new FileInputStream(file)));
        } catch (Exception e) {
            throw new FileExcelExportNotFoundException(file.getName());
        }
        return response;
    }
}
