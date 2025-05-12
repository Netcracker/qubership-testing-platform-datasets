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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.qubership.atp.dataset.db.migration.Utils;
import org.qubership.atp.dataset.db.migration.customchange.constant.CustomChangeConstants;
import org.qubership.atp.dataset.db.migration.customchange.constant.JoinerSplitterConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ParentLastClassloader extends URLClassLoader {

    private static final String JAR = ".jar";
    private static final Logger LOGGER = LoggerFactory.getLogger(ParentLastClassloader.class);
    private static final String Q_CLASSES_JAR_EXPRESSION = "atp-dataset-q-classes-generation.+\\.jar";
    private final String sourcePath;
    private Map<String, ChildClassLoader> urlClassLoaders = new HashMap<>();
    private String jdbcType;

    /**
     * ClassLoader based on {@link URLClassLoader} for loading classes from self. In case class not
     * found in this CL, it will try to load it from parent CL.
     *
     * @param sourcePath - location of resources. update.xml, migration scripts/jars etc. For
     *                   example: ./scripts/
     * @param jdbcType   - type of database. This parameter need for loading generated q-classes. If
     *                   you want use postgres database, and your jar with classes has name
     *                   atp-dataset-q-classes-generation-pg.jar, then you must define
     *                   jdbcType=pg When CL trying to load jar, it split the name on three parts:
     *                   1. general name = qubership-atp-dataset-q-classes-generation 2. suffix =
     *                   pg 3. extension = .jar The suffix must match to jdbcType for loading this
     *                   jar. So, you can check how it works in method {@link
     *                   #createNewUrlClassLoader(List)}.
     */
    public ParentLastClassloader(URLClassLoader urlClassLoader, String sourcePath, String jdbcType) {
        super(urlClassLoader.getURLs(), urlClassLoader);
        this.jdbcType = jdbcType;
        if (Objects.isNull(jdbcType) || jdbcType.isEmpty()) {
            throw new IllegalStateException("Parameter 'jdbc_type' can't be null or empty."
                    + "Value of jdbc_type depends of suffix in jar name, which contains generated q-classes."
                    + "atp-dataset-q-classes-generation-pg.jar - there is jdbc_type is 'pg'");
        }
        this.sourcePath = sourcePath;
        try (Stream<Path> walk = Files.walk(Paths.get(sourcePath))) {
            walk
                    .filter(path -> !Utils.isJarFile(path))
                    .forEach(path -> {
                        try {
                            this.addURL(path.toUri().toURL());
                        } catch (MalformedURLException e) {
                            LOGGER.error("Unable convert file to url", e);
                        }
                    });
        } catch (IOException e) {
            LOGGER.error("Unable to add resource files to classpath", e);
        }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> resources = null;
        try {
            resources = findResources(name);
        } catch (Exception e) {
            /*Ignore it*/
        }
        if (resources != null && !resources.hasMoreElements()) {
            return loadFromSuper(name);
        }
        return resources;
    }

    private Enumeration<URL> loadFromSuper(String name) throws IOException {
        LOGGER.warn("Resource not found in self class loader. Resource: " + name);
        return super.getResources(name);
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return null;
    }

    /**
     * Load class from current classloader. In case the liquibase started executing new one
     * changeset, then classloader will load compatible jars with q-classes and shaded jar with
     * required dependencies. So, if package start with v000.ATPII671.CustomChangeSet Then
     * classloader will load jars from {@code sourcePath}/v000/ATPII671 Which of q-classes jar will
     * be loaded? It depends of {@code jdbc_type}.
     *
     * @param className target class name.
     * @return the {@link Class}
     * @throws ClassNotFoundException in case Class not found in current and parent classloader.
     */
    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        if (isClassInsideBasePackOrThirdPartyPacks(className)) {
            return super.loadClass(className, false);
        }

        if (!className.matches("^v\\d+.+")) {
            throw new IllegalArgumentException("Is liquibase started loading new change set?");
        }
        URLClassLoader urlClassLoader = refreshClassLoader(className);
        //reload compatible q-classes.jar and dependencies
        Class<?> loadedClass = urlClassLoader.loadClass(className);
        if (loadedClass == null) {
            super.loadClass(className);
        }
        return loadedClass;
    }

    private boolean isClassInsideBasePackOrThirdPartyPacks(String className) {
        if (!(className.startsWith(CustomChangeConstants.CUSTOM_CHANGE_BASE_PACK)
                || className.startsWith(CustomChangeConstants.LIQUIBASE_PACK))) {
            return false;
        }
        String packageName = getPackageName(className);
        return CustomChangeConstants.CUSTOM_CHANGE_BASE_PACK.equals(packageName)
                || packageName.startsWith(CustomChangeConstants.LIQUIBASE_PACK);
    }

    private String getPackageName(String className) {
        List<String> splitClassPath = JoinerSplitterConstants.POINT_SPLITTER.splitToList(className);
        splitClassPath = splitClassPath.stream().limit(splitClassPath.size() - 1).collect(Collectors.toList());
        return JoinerSplitterConstants.POINT_JOINER.join(splitClassPath);
    }

    protected ChildClassLoader refreshClassLoader(String className) {
        String jarPath = className
                .replaceAll("\\.", "/").replaceFirst("\\w+$", "");
        ChildClassLoader urlClassLoader = urlClassLoaders.get(jarPath);
        if (urlClassLoader != null) {
            return urlClassLoader;
        }
        try {
            Path dir = Paths.get(sourcePath, jarPath);
            List<Path> jarList = Files.list(dir).filter(Utils::isJarFile).collect(Collectors.toList());
            urlClassLoader = createNewUrlClassLoader(jarList);
            urlClassLoaders.put(jarPath, urlClassLoader);
            return urlClassLoader;
        } catch (IOException e) {
            throw new IllegalStateException("Jar file is not found by path:" + jarPath, e);
        }
    }

    private ChildClassLoader createNewUrlClassLoader(List<Path> jarList) {
        final List<URL> urls = new LinkedList<>();
        jarList.forEach(path -> {
            try {
                String name = path.toFile().getName().toLowerCase();
                if (name.matches(Q_CLASSES_JAR_EXPRESSION)) {
                    loadGeneratedQClasses(urls, name, path);
                } else {
                    urls.add(getUrl(path));
                }
            } catch (IOException e) {
                LOGGER.error("Can't convert path to URL. File path:" + path, e);
            }
        });
        return new ChildClassLoader(urls.toArray(new URL[urls.size()]), this, getParent());
    }

    private void loadGeneratedQClasses(List<URL> urls, String name, Path path) throws IOException {
        if (name.endsWith(jdbcType + JAR)) {
            urls.add(getUrl(path)); //load jar with q-classes
        }
    }

    private URL getUrl(Path jarFile) throws IOException {
        //The syntax of a JAR URL is: jar:${path}!/
        return new URL("jar:file:" + jarFile.toString() + "!/");
    }
}
