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

package org.qubership.atp.dataset.db.migration.classloader;

import java.net.URL;
import java.net.URLClassLoader;

public class ChildClassLoader extends URLClassLoader {

    ClassLoader realParent;

    public ChildClassLoader(URL[] urls, ParentLastClassloader parent, ClassLoader realParent) {
        super(urls, parent);
        this.realParent = realParent;
    }

    @Override
    public URL getResource(String name) {
        URL url;
        if (realParent != null) {
            url = realParent.getResource(name);
        } else {
            url = ClassLoader.getSystemResource(name);
        }
        if (url == null) {
            url = findResource(name);
        }
        return url;
    }

    /**
     * can throw java.lang.NoClassDefFoundError when chainloading classes via findClass
     */
    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            Class<?> clazz = super.findLoadedClass(name);
            if (clazz != null) {
                return clazz;
            }
            //classloading starts here
            clazz = super.findClass(name);
            return clazz;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            try {
                return realParent.loadClass(name);
            } catch (ClassNotFoundException | NoClassDefFoundError ex) {
                ClassNotFoundException detailedException = new ClassNotFoundException(
                        String.format("Error while loading [%s]", name));
                detailedException.addSuppressed(ex);
                throw detailedException;
            }
        }
    }
}
