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

package org.qubership.atp.dataset.service.rest.advice;

import org.qubership.atp.dataset.exception.EntityNotFoundException;
import org.qubership.atp.dataset.service.rest.ExceptionInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Value("${atp.handler.exception.include-stack-trace:false}")
    private boolean includeStackTrace;

    /**
     *  Handle if shadow was not found for entity.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ExceptionInfo> entityNotFoundExceptionHandle(EntityNotFoundException exception) {

        ExceptionInfo info = new ExceptionInfo();
        info.setMessage(exception.getMessage());
        info.setErrorCode(HttpStatus.NOT_FOUND.value());
        if (includeStackTrace) {
            info.setStackTrace(Throwables.getStackTraceAsString(exception));
        }
        log.error(exception.getMessage(), exception);

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(info);
    }

    /**
     * Handle AccessDeniedException exception with saved headers.
     */
    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<Object> handleException(AccessDeniedException exception) {
        log.error(exception.getMessage(), exception);
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(exception.getMessage());
    }
}
