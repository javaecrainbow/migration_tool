package org.flywaydb.core.internal.resolver.sql;

import org.flywaydb.core.api.MigrationType;
import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.callback.Event;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogFactory;
import org.flywaydb.core.api.resolver.Context;
import org.flywaydb.core.api.resolver.MigrationResolver;
import org.flywaydb.core.api.resolver.ResolvedMigration;
import org.flywaydb.core.api.resource.LoadableResource;
import org.flywaydb.core.internal.parser.ParsingContext;
import org.flywaydb.core.internal.parser.PlaceholderReplacingReader;
import org.flywaydb.core.internal.resolver.ChecksumCalculator;
import org.flywaydb.core.internal.resolver.ResolvedMigrationComparator;
import org.flywaydb.core.internal.resolver.ResolvedMigrationImpl;
import org.flywaydb.core.internal.resource.ResourceName;
import org.flywaydb.core.internal.resource.ResourceNameParser;
import org.flywaydb.core.internal.sqlscript.SqlScript;
import org.flywaydb.core.internal.sqlscript.SqlScriptExecutorFactory;
import org.flywaydb.core.internal.sqlscript.SqlScriptFactory;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Migration resolver for SQL files on the classpath. The SQL files must have names like
 *  U__description.sql.
 */
public class SqlUndoResolver implements MigrationResolver {
    private static final Log LOG = LogFactory.getLog(SqlUndoResolver.class);
    private final SqlScriptExecutorFactory sqlScriptExecutorFactory;
    private final ResourceProvider resourceProvider;
    private final SqlScriptFactory sqlScriptFactory;
    private final Configuration configuration;
    private final ParsingContext parsingContext;

    public SqlUndoResolver(ResourceProvider resourceProvider, SqlScriptExecutorFactory sqlScriptExecutorFactory,
                           SqlScriptFactory sqlScriptFactory, Configuration configuration, ParsingContext parsingContext) {
        this.sqlScriptExecutorFactory = sqlScriptExecutorFactory;
        this.resourceProvider = resourceProvider;
        this.sqlScriptFactory = sqlScriptFactory;
        this.configuration = configuration;
        this.parsingContext = parsingContext;
    }

    @Override
    public List<ResolvedMigration> resolveMigrations(Context context) {
        List<ResolvedMigration> migrations = new ArrayList<>();
        String[] suffixes = configuration.getSqlMigrationSuffixes();

        addMigrations(migrations, configuration.getUndoSqlMigrationPrefix(), suffixes,
                false
        );
        migrations.sort(new ResolvedMigrationComparator());
        return migrations;
    }

    private LoadableResource[] createPlaceholderReplacingLoadableResources(List<LoadableResource> loadableResources) {
        List<LoadableResource> list = new ArrayList<>();

        for (final LoadableResource loadableResource : loadableResources) {
            LoadableResource placeholderReplacingLoadableResource = new LoadableResource() {
                @Override
                public Reader read() {
                    return PlaceholderReplacingReader.create(configuration, parsingContext, loadableResource.read());
                }

                @Override
                public String getAbsolutePath() { return loadableResource.getAbsolutePath(); }
                @Override
                public String getAbsolutePathOnDisk() { return loadableResource.getAbsolutePathOnDisk(); }
                @Override
                public String getFilename() { return loadableResource.getFilename(); }
                @Override
                public String getRelativePath() { return loadableResource.getRelativePath(); }
            };

            list.add(placeholderReplacingLoadableResource);
        }

        return list.toArray(new LoadableResource[0]);
    }

    private Integer getChecksumForLoadableResource(boolean repeatable, List<LoadableResource> loadableResources) {
        if (repeatable && configuration.isPlaceholderReplacement()) {
            return ChecksumCalculator.calculate(createPlaceholderReplacingLoadableResources(loadableResources));
        }

        return ChecksumCalculator.calculate(loadableResources.toArray(new LoadableResource[0]));
    }

    private Integer getEquivalentChecksumForLoadableResource(boolean repeatable, List<LoadableResource> loadableResources) {
        if (repeatable) {
            return ChecksumCalculator.calculate(loadableResources.toArray(new LoadableResource[0]));
        }

        return null;
    }

    private void addMigrations(List<ResolvedMigration> migrations, String prefix, String[] suffixes,
                               boolean repeatable



    ){
        ResourceNameParser resourceNameParser = new ResourceNameParser(configuration);

        for (LoadableResource resource : resourceProvider.getResources(prefix, suffixes)) {
            String filename = resource.getFilename();
            ResourceName result = resourceNameParser.parse(filename);
            if (!result.isValid() || isSqlCallback(result) || !prefix.equals(result.getPrefix())) {
                continue;
            }

            SqlScript sqlScript = sqlScriptFactory.createSqlScript(resource, configuration.isMixed(), resourceProvider);

            List<LoadableResource> resources = new ArrayList<>();
            resources.add(resource);
            Integer checksum = getChecksumForLoadableResource(repeatable, resources);
            Integer equivalentChecksum = getEquivalentChecksumForLoadableResource(repeatable, resources);

            migrations.add(new ResolvedMigrationImpl(
                    result.getVersion(),
                    result.getDescription(),
                    resource.getRelativePath(),
                    checksum,
                    equivalentChecksum,
                    MigrationType.UNDO_SQL,
                    resource.getAbsolutePathOnDisk(),
                    new SqlMigrationExecutor(sqlScriptExecutorFactory, sqlScript
                            , true, false

                    )) {
                @Override
                public void validate() { }
            });
        }
    }

    /**
     * Checks whether this filename is actually a sql-based callback instead of a regular migration.
     *
     * @param result The parsing result to check.
     */
    protected static boolean isSqlCallback(ResourceName result) {
        return Event.fromId(result.getPrefix()) != null;
    }
}