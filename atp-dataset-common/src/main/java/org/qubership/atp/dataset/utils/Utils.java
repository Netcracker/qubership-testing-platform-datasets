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

package org.qubership.atp.dataset.utils;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]*)}");

    /**
     * Behaves like a apache IOUtils.
     */
    public static void closeQuietly(@Nullable Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            LOGGER.warn("Can not close resource: " + closeable, e);
        }
    }

    /**
     * Replaces placeholder if needed.
     *
     * @param placeholderIntoValue invoked for each replace action (lazy). Converts placeholder text
     *                             (without ${} symbols) into value.
     */
    public static String replacePlaceholders(String in, Function<String, String> placeholderIntoValue) {
        Matcher matcher = PLACEHOLDER.matcher(in);
        return replacePlaceholders(matcher, 1, placeholderIntoValue);
    }

    /**
     * Replaces placeholder if needed.
     *
     * @param matcher              with placeholders to replace.
     * @param placeholderIntoValue invoked for each replace action (lazy). Converts placeholder
     *                             (with ${} symbols) into value.
     */
    public static String replacePlaceholders(Matcher matcher, Function<String, String> placeholderIntoValue) {
        return replacePlaceholders(matcher, 0, placeholderIntoValue);
    }

    /**
     * Replaces placeholder if needed.
     *
     * @param matcher              with placeholders to replace.
     * @param placeholderIntoValue invoked for each replace action (lazy). Converts selected group
     *                             of the placeholder into value.
     */
    private static String replacePlaceholders(Matcher matcher, int groupToConvert,
                                              Function<String, String> placeholderIntoValue) {
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, placeholderIntoValue.apply(matcher.group(groupToConvert)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * creates class instance.
     *
     * @param targetClazz     class to be returned
     * @param className       of created instance (class name should be sublass of targetClazz or
     *                        targetClazz itself)
     * @param debugIdentifier information for debug purposes
     * @param <T>             type of class to be created
     * @return new instance of requested class
     */
    public static <T> T doInstance(@Nonnull Class<T> targetClazz,
                                   @Nonnull String className,
                                   @Nullable String debugIdentifier) {
        Class clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException firstEx) {
            try {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (cl == null) {
                    throw new ClassNotFoundException("Have no context class loader");
                }
                clazz = cl.loadClass(className);
            } catch (ClassNotFoundException secondEx) {
                RuntimeException lastEx = new RuntimeException(appendDebugInfo(new StringBuilder("Can not find"),
                        targetClazz, className, debugIdentifier).toString(), firstEx);
                lastEx.addSuppressed(secondEx);
                throw lastEx;
            }
        }
        try {
            return targetClazz.cast(clazz.newInstance());
        } catch (Exception e) {
            throw new RuntimeException(appendDebugInfo(new StringBuilder("Can not instantiate a"),
                    targetClazz, className, debugIdentifier).toString(), e);
        }
    }

    @Nonnull
    private static StringBuilder appendDebugInfo(@Nonnull StringBuilder builder, @Nonnull Class<?> targetClazz,
                                                 @Nonnull String className, @Nullable String debugIdentifier) {
        builder.append(" class [").append(className).append("] of [").append(targetClazz.getName())
                .append("]");
        if (debugIdentifier != null) {
            builder.append(" for [").append(debugIdentifier).append("]");
        }
        return builder;
    }
}
