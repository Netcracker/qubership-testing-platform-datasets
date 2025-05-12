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

package org.qubership.atp.dataset.db.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.utils.Utils;

import com.google.common.base.Preconditions;
import com.google.common.reflect.Reflection;

public class Proxies {

    /**
     * Proxy which obtains a real object using id and sourceFunc on access. Methods {@link
     * Object#equals(Object)}, {@link Object#hashCode()}, {@link Identified#getId()} do not trigger
     * the sourceFunc.
     *
     * @param type       target interface
     * @param id         identified object id
     * @param sourceFunc explains how to obtain a real instance, should not return null ever
     * @param <T>        target
     * @return proxy
     */
    public static <T extends Identified, IdT> T withId(@Nonnull Class<T> type, @Nonnull IdT id,
                                                       @Nonnull Function<IdT, ? extends T> sourceFunc) {
        return Reflection.newProxy(type, new IdBasedCachingH<>(type, id, sourceFunc));
    }

    /**
     * Wraps a list into proxy. See {@link #base(Class, Supplier)}
     */
    public static <T> List<T> list(@Nonnull Supplier<List<T>> sup) {
        //noinspection unchecked
        return base(List.class, sup);
    }

    /**
     * Wraps an supplier into object type of T. Supplier is triggered once on access to object
     * methods.
     *
     * @param type target object type
     * @param sup  Supplier should not return null ever
     * @param <T>  target
     * @return proxy
     */
    public static <T> T base(@Nonnull Class<T> type, @Nonnull Supplier<? extends T> sup) {
        return Reflection.newProxy(type, new CachingH<>(type, sup));
    }

    @NotThreadSafe
    private static class IdBasedCachingH<T extends Identified, IdT> implements InvocationHandler {
        private final Class<T> type;
        private final IdT id;
        private final Function<IdT, ? extends T> sourceFunc;
        private boolean initialized;
        private T cachedValue;

        private IdBasedCachingH(@Nonnull Class<T> type,
                                @Nonnull IdT id,
                                @Nonnull Function<IdT, ? extends T> sourceFunc) {
            this.type = type;
            this.id = id;
            this.sourceFunc = sourceFunc;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (initialized) {
                return method.invoke(cachedValue, args);
            }
            String methodName = method.getName();
            switch (methodName) {
                case "equals":
                    Object target = args[0];
                    return proxy == target || Utils.isEqual(type, id, target);
                case "hashCode":
                    return Objects.hashCode(id);
                case "getId":
                    return id;
                default:
                    cachedValue = Preconditions.checkNotNull(sourceFunc.apply(id),
                            "[%s] with id [%s] is not provided by [%s]", type, id, sourceFunc);
                    initialized = true;
                    return method.invoke(cachedValue, args);
            }
        }

        @Override
        public String toString() {
            return String.format("[%s with id: %s]", type.getSimpleName(), id);
        }
    }

    @NotThreadSafe
    private static class CachingH<T> implements InvocationHandler {

        private final Class<T> type;
        private final Supplier<? extends T> sup;
        private boolean initialized;
        private T cachedValue;

        private CachingH(@Nonnull Class<T> type, @Nonnull Supplier<? extends T> sup) {
            this.type = type;
            this.sup = sup;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (initialized) {
                return method.invoke(cachedValue, args);
            }
            cachedValue = Preconditions.checkNotNull(sup.get(),
                    "[%s] is not provided by [%s]", type, sup);
            initialized = true;
            return method.invoke(cachedValue, args);
        }
    }
}
