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

package org.qubership.atp.dataset.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Configuration
@EnableAsync
public class ThreadPoolConfig {

    private static final String THREAD_NAME_PREFIX = "ArchiveJob-";
    private static final String THREAD_NAME_ASYNC_COPY_PREFIX = "AsyncCopy-";

    @Value("${atp-dataset.archive.job.thread.max-pool-size}")
    private Integer maxPoolSize;
    @Value("${atp-dataset.archive.job.thread.core-pool-size}")
    private Integer corePoolSize;
    @Value("${atp-dataset.archive.job.thread.queue-capacity}")
    private Integer queueCapacity;

    /**
     * Archive job thread pool task executor.
     *
     * @return {@link ThreadPoolTaskExecutor} the thread pool task executor
     */
    @Bean
    public ThreadPoolTaskExecutor archiveJobExecutor() {
        return createExecutor(THREAD_NAME_PREFIX);
    }

    @Qualifier("asyncCopyTaskExecutor")
    @Bean("asyncCopyTaskExecutor")
    public AsyncTaskExecutor asyncCopyTaskExecutor() {
        return new DelegatingSecurityContextAsyncTaskExecutor(createExecutor(THREAD_NAME_ASYNC_COPY_PREFIX));
    }

    private ThreadPoolTaskExecutor createExecutor(String prefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(maxPoolSize);
        executor.setCorePoolSize(corePoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setThreadNamePrefix(prefix);
        executor.initialize();
        return executor;
    }
}
