package com.salk.migration.provider;

import com.salk.migration.spring.MigrationProperty;
import org.flywaydb.core.api.ClassProvider;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogFactory;
import org.flywaydb.core.api.resource.LoadableResource;
import org.flywaydb.core.internal.scanner.LocationScannerCache;
import org.flywaydb.core.internal.scanner.ResourceNameCache;
import org.flywaydb.core.internal.scanner.android.AndroidScanner;
import org.flywaydb.core.internal.scanner.classpath.ClassPathScanner;
import org.flywaydb.core.internal.scanner.classpath.ResourceAndClassScanner;
import org.flywaydb.core.internal.scanner.filesystem.FileSystemScanner;
import org.flywaydb.core.internal.util.FeatureDetector;
import org.flywaydb.core.internal.util.StringUtils;

import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.*;

/**
 * 自定义资源扫描模式
 * @author salkli
 * @since 2022/4/15
 **/
public class MigrationScannerProvider<I> implements ResourceProvider, ClassProvider<I> {
    private static final Log LOG = LogFactory.getLog(MigrationScannerProvider.class);

    private final List<LoadableResource> resources = new ArrayList<>();
    private final List<Class<? extends I>> classes = new ArrayList<>();
    /**
     * undo的class列表
     */
    private final List<Class<? extends I>> undoClasses = new ArrayList();

    private final List<LoadableResource> undoResource = new ArrayList<>();

    private String env;

    private final HashMap<String, LoadableResource> relativeResourceMap = new HashMap<>();
    private HashMap<String, LoadableResource> absoluteResourceMap = null;

    public MigrationScannerProvider(Class<I> implementedInterface, Collection<Location> locations,
        ClassLoader classLoader, Charset encoding, boolean detectEncoding, boolean stream,
        ResourceNameCache resourceNameCache, LocationScannerCache locationScannerCache, boolean throwOnMissingLocations,
        String env) {
        FileSystemScanner fileSystemScanner =
            new FileSystemScanner(encoding, stream, detectEncoding, throwOnMissingLocations);

        FeatureDetector detector = new FeatureDetector(classLoader);
        boolean android = detector.isAndroidAvailable();
        for (Location location : locations) {
            if (location.isFileSystem()) {
                resources.addAll(fileSystemScanner.scanForResources(location));
            } else {
                ResourceAndClassScanner<I> resourceAndClassScanner =
                    android ? new AndroidScanner<>(implementedInterface, classLoader, encoding, location)
                        : new ClassPathScanner<>(implementedInterface, classLoader, encoding, location,
                            resourceNameCache, locationScannerCache, throwOnMissingLocations);
                resources.addAll(resourceAndClassScanner.scanForResources());

                classes.addAll(resourceAndClassScanner.scanForClasses());
            }
        }
        for (LoadableResource resource : resources) {
            relativeResourceMap.put(resource.getRelativePath().toLowerCase(), resource);
        }
        this.env = env;
    }

    @Override
    public LoadableResource getResource(String name) {
        LoadableResource loadedResource = relativeResourceMap.get(name.toLowerCase());

        if (loadedResource != null) {
            return loadedResource;
        }

        // Only build the HashMap and resolve the absolute paths if an
        // absolute path is requested as this is really slow
        // Should only ever be required for sqlplus @
        if (Paths.get(name).isAbsolute()) {
            if (absoluteResourceMap == null) {
                absoluteResourceMap = new HashMap<>();
                for (LoadableResource resource : resources) {
                    absoluteResourceMap.put(resource.getAbsolutePathOnDisk().toLowerCase(), resource);
                }
            }

            loadedResource = absoluteResourceMap.get(name.toLowerCase());

            if (loadedResource != null) {
                return loadedResource;
            }
        }

        return null;
    }

    /**
     * Returns all known resources starting with the specified prefix and ending with any of the specified suffixes.
     *
     * @param prefix
     *            The prefix of the resource names to match.
     * @param suffixes
     *            The suffixes of the resource names to match.
     * @return The resources that were found.
     */
    @Override
    public Collection<LoadableResource> getResources(String prefix, String... suffixes) {
        List<LoadableResource> result = new ArrayList<>();
        for (LoadableResource resource : resources) {
            String fileName = resource.getFilename();
            // sys 类型的sql只在gray执行
            if (!MigrationProperty.ENV_GRAY.equals(env) && fileName.indexOf(MigrationProperty.SYS) != -1) {
                LOG.debug("Filtering out sys resource on stable or other env: " + resource.getAbsolutePath()
                    + " (filename: " + fileName + ")");

                continue;
            }
            if (StringUtils.startsAndEndsWith(fileName, prefix, suffixes)) {
                result.add(resource);
            } else {
                if(!"U".equals(prefix)) {
                    LOG.debug("Filtering out resource: " + resource.getAbsolutePath() + " (filename: " + fileName + ")");
                }
            }
        }
        return result;
    }

    @Override
    public Collection<Class<? extends I>> getClasses() {
        List<Class<? extends I>> result = new ArrayList();
        for (Class<? extends I> classT : classes) {
            if (!MigrationProperty.ENV_GRAY.equals(env) && classT.getSimpleName().indexOf(MigrationProperty.SYS) != -1) {
                LOG.debug(
                    "Filtering out sys resource on " + env + ": " + " (filename: " + classT.getSimpleName() + ")");
                continue;
            }
            result.add(classT);
        }
        return Collections.unmodifiableCollection(result);
    }
}
