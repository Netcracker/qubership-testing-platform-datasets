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

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.qubership.atp.dataset.db.CacheRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component("dsCacheInterceptor")
public class CacheInterceptor {

    @Autowired
    private CacheRepository cacheRepo;

    /**
     *  Method interceptor for method with Transactional annotation.
     */
    @Around("within(org.qubership.atp.dataset.service.direct.IdentifiedService+) "
            + "&& ! @annotation(org.springframework.transaction.annotation.Transactional)")
    public Object invoke(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            cacheRepo.enableCache();
            return joinPoint.proceed();
        } finally {
            cacheRepo.disableCache();
        }
    }
}
