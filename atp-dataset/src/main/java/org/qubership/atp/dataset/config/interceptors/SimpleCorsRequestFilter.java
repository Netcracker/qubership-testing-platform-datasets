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

package org.qubership.atp.dataset.config.interceptors;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.qubership.atp.dataset.service.jpa.model.CacheCleanupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SimpleCorsRequestFilter implements Filter {

    @Autowired
    protected CacheCleanupService cacheCleanupService;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
            ServletException {
        loggingRequest(req);
        try {
            HttpServletResponse response = (HttpServletResponse) res;
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, COPY, HEAD, OPTIONS");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setHeader("Access-Control-Allow-Headers",
                    "Origin, X-Requested-With, Content-Type, Accept, Key, Authorization, Content-Disposition");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            HttpServletRequest request = (HttpServletRequest) req;
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                chain.doFilter(req, res);
            }
        } finally {
            cacheCleanupService.cleanAllLocalThreadCache();
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }

    /**
     *  Method for logging request .
     */
    public void loggingRequest(ServletRequest req)  {
        HttpServletRequest httpServletRequest = null;
        try {
            httpServletRequest = (HttpServletRequest) req;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        if (httpServletRequest != null
                && (httpServletRequest.getRequestURI().contains("/itf")
                || httpServletRequest.getRequestURI().contains("/atp"))) {
            String endpoint = httpServletRequest.getRequestURI();
            String message = "Request for DataSet  - Endpoint: \"" + endpoint + "\", Method: \""
                    + httpServletRequest.getMethod();
            log.info(message);
        }
    }
}
