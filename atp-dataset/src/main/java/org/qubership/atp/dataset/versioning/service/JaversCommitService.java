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

package org.qubership.atp.dataset.versioning.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.javers.core.commit.Commit;
import org.javers.repository.jql.GlobalIdDTO;

public interface JaversCommitService {

    Commit commit(Object currentVersion);

    Commit commitWithExtraProperties(Object currentVersion, Map<String, String> commitProperties);

    CompletableFuture<Commit> commitAsync(Object currentVersion, Executor executor);

    Commit commitShallowDelete(Object deleted);

    Commit commitShallowDeleteById(GlobalIdDTO globalId);
}
