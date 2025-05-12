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

package org.qubership.atp.dataset.exception.excel;

import org.qubership.atp.dataset.exception.DataSetException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "TDS-7005")
public class ExcelImportUnexpectedException extends DataSetException {

    private static String DEFAULT_MESSAGE = "Unexpected error while import. Exception: '%s'";

    public ExcelImportUnexpectedException(Exception e) {
        super(String.format(DEFAULT_MESSAGE, e.getMessage()));
    }
}
