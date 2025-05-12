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

package org.qubership.atp.dataset.model.impl.file;

import java.util.UUID;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.model.Parameter;

import com.google.common.base.Strings;

public class FileData {

    private String fileName;
    private UUID parameterUuid;
    private String fileType;
    private String contentType;
    private String url;

    public FileData() {
    }

    /**
     * File parameter need only for transfer file information.
     *
     * @param fileName      to upload
     * @param parameterUuid target {@link Parameter} id.
     * @param contentType   content type in http request format.
     */
    public FileData(@Nonnull String fileName, @Nonnull UUID parameterUuid, @Nonnull String contentType) {
        this();
        this.fileName = fileName;
        this.parameterUuid = parameterUuid;
        this.fileType = getFileExtension(fileName);
        this.contentType = Strings.isNullOrEmpty(contentType) ? "plain/text" : contentType;
        this.url = "/attachment/" + parameterUuid;
    }

    private String getFileExtension(String fileName) {
        if (Strings.isNullOrEmpty(fileName)) {
            return null;
        }
        int lastIndex = fileName.lastIndexOf('.');
        String contentType = "unknown";
        if (lastIndex > -1) {
            contentType = fileName.substring(lastIndex + 1);
        }
        return contentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public UUID getParameterUuid() {
        return parameterUuid;
    }

    public void setParameterUuid(UUID parameterUuid) {
        this.parameterUuid = parameterUuid;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
