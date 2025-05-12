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

package org.qubership.atp.dataset.service.direct.importexport.utils;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class StreamUtils {

    /**
     * Extract id's from any type entities.
     *
     * @param entities  input entities
     * @param extractor id extractor
     * @param <T>       processed entities type
     * @return result set
     */
    public static <T> Set<UUID> extractIds(Collection<T> entities, Function<T, UUID> extractor) {
        if (entities == null) {
            return Collections.emptySet();
        }

        return getIdsStream(entities, extractor)
                .collect(toSet());
    }

    /**
     * Get id's stream from any type entities.
     *
     * @param entities  input entities
     * @param extractor id extractor
     * @param <T>       processed entities type
     * @return result stream
     */
    private static <T> Stream<UUID> getIdsStream(Collection<T> entities, Function<T, UUID> extractor) {
        return entities.stream()
                .map(extractor)
                .filter(Objects::nonNull);
    }

    private static <T> Stream<T> stream(Iterable<T> entities) {
        return StreamSupport.stream(entities.spliterator(), false);
    }

    public static <T> Map<String, UUID> toNameIdEntityMap(Iterable<T> entities, Function<T, String> keyExtractor,
                                                          Function<T, UUID> valueExtractor) {
        return stream(entities)
                .collect(Collectors.toMap(keyExtractor, valueExtractor));
    }

    public static <T> Map<String, T> toNameEntityMap(Iterable<T> entities, Function<T, String> keyExtractor) {
        return stream(entities)
                .collect(Collectors.toMap(keyExtractor, identity()));
    }

    public static <T, R> Map<R, T> toEntityMap(Iterable<T> entities, Function<T, R> keyExtractor) {
        return stream(entities)
                .collect(Collectors.toMap(keyExtractor, identity()));
    }

    public static <T> Map<UUID, List<T>> toEntityListMap(Iterable<T> entities,
                                                         Function<T, UUID> keyExtractor) {
        return stream(entities)
                .collect(Collectors.groupingBy(keyExtractor));
    }
}
