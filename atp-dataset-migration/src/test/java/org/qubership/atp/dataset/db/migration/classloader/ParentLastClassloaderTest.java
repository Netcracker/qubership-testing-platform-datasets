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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;

import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ParentLastClassloaderTest {

    private ParentLastClassloader parentLassCL;

    @BeforeEach
    public void setUp() throws MalformedURLException {

        Path path = Paths.get("atp-dataset-migration/target/scripts");
        URL[] urls = new URL[]{path.toUri().toURL()};
        URLClassLoader urlClassLoader = new URLClassLoader(urls);
        parentLassCL = new ParentLastClassloader(urlClassLoader, "src/test/resources/data", "pg");
    }

    /*
        ATTENTION! Works only with surefire plugin. Due to it adds to class path required jar from
        src/test/resources/test-data
        You can find it in pom.xml.
    */
    @Disabled
    @Test
    public void testQClassLoadedFromChildClassPath() throws Exception {
        Class<?> qClass = loadVisiblityAreaQClass(getClass().getClassLoader());
        Assertions.assertFalse(qClass.getProtectionDomain().getCodeSource().toString().contains("v000"),
                "QVisibilityArea class is loaded from 'src/test/resources/data/v000/' but must be" +
                        " loaded shurefire plugin from 'src/test/resources/test-data'");
        Class entryPoint = refreshCl();
        qClass = loadVisiblityAreaQClass(entryPoint.getClassLoader());
        assertThat(
                qClass.getProtectionDomain().getCodeSource().getLocation().toString(),
                StringContains.containsString("v000")
        );
    }

    @Disabled
    @Test
    public void testClassLoadedFromParentClassLoader() throws ClassNotFoundException {
        Class entryPoint = refreshCl();
        assertNotNull(entryPoint.getClassLoader().loadClass("liquibase.change.custom.CustomTaskChange"));
    }

    @Disabled
    @Test
    public void testClassLoaderLoadsResources() throws IOException, ClassNotFoundException {
        Class entryPoint = refreshCl();
        Enumeration<URL> resources = entryPoint.getClassLoader().getResources("changes.xml");
        assertTrue(resources.hasMoreElements());
    }

    @Disabled
    @Test
    public void testClassLoaderLoadsDifferentQClasses() throws ClassNotFoundException {
        Class entryPoint = refreshCl();
        Class<?> qClass = loadVisiblityAreaQClass(entryPoint.getClassLoader());
        assertThat(qClass.getProtectionDomain().getCodeSource().toString(), StringContains.containsString("pg.jar"));
    }

    private Class refreshCl() throws ClassNotFoundException {
        return parentLassCL.loadClass("v000.ATPII704.ReferenceUpdateChange");
    }

    private Class<?> loadVisiblityAreaQClass(ClassLoader classLoader) throws ClassNotFoundException {
        return classLoader.loadClass("org.qubership.atp.dataset.db.generated.QVisibilityArea");
    }
}
