/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.salk.migration.extend.undo;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.callback.Event;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.executor.Context;
import org.flywaydb.core.api.executor.MigrationExecutor;
import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogFactory;
import org.flywaydb.core.api.output.CommandResultFactory;
import org.flywaydb.core.api.output.UndoResult;
import org.flywaydb.core.api.resolver.MigrationResolver;
import org.flywaydb.core.api.resolver.ResolvedMigration;
import org.flywaydb.core.internal.callback.CallbackExecutor;
import org.flywaydb.core.internal.database.base.Connection;
import org.flywaydb.core.internal.database.base.Database;
import org.flywaydb.core.internal.database.base.Schema;
import org.flywaydb.core.internal.info.MigrationInfoImpl;
import org.flywaydb.core.internal.info.MigrationInfoServiceImpl;
import org.flywaydb.core.internal.jdbc.ExecutionTemplateFactory;
import org.flywaydb.core.internal.schemahistory.SchemaHistory;
import org.flywaydb.core.internal.util.ExceptionUtils;
import org.flywaydb.core.internal.util.StopWatch;
import org.flywaydb.core.internal.util.StringUtils;
import org.flywaydb.core.internal.util.TimeFormat;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行undo的逻辑
 */
public class DbUndo {
    private static final Log LOG = LogFactory.getLog(DbUndo.class);

    private final Database database;
    private final SchemaHistory schemaHistory;
    /**
     * The schema containing the schema history table.
     */
    private final Schema schema;
    private final MigrationResolver migrationResolver;
    private final Configuration configuration;
    private final CallbackExecutor callbackExecutor;
    /**
     * The connection to use to perform the actual database migrations.
     */
    private final Connection connectionUserObjects;
    private UndoResult undoResult;
    /**
     * This is used to remember the type of migration between calls to migrateGroup().
     */
    private boolean isPreviousVersioned;
    private final List<ResolvedMigration> appliedResolvedMigrations = new ArrayList<>();

    public DbUndo(Database database, SchemaHistory schemaHistory, Schema schema, MigrationResolver migrationResolver,
                     Configuration configuration, CallbackExecutor callbackExecutor) {
        this.database = database;
        this.connectionUserObjects = database.getMigrationConnection();
        this.schemaHistory = schemaHistory;
        this.schema = schema;
        this.migrationResolver = migrationResolver;
        this.configuration = configuration;
        this.callbackExecutor = callbackExecutor;
    }

    /**
     * Starts the actual migration.
     */
    public UndoResult migrate() throws FlywayException {
        callbackExecutor.onMigrateOrUndoEvent(Event.BEFORE_UNDO);
        undoResult = CommandResultFactory.createUndoResult(database.getCatalog(), configuration);

        int count;
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            count = configuration.isGroup() ?
                    // When group is active, start the transaction boundary early to
                    // ensure that all changes to the schema history table are either committed or rolled back atomically.
                    schemaHistory.lock(this::migrateAll) :
                    // For all regular cases, proceed with the migration as usual.
                    migrateAll();

            stopWatch.stop();

            undoResult.targetSchemaVersion = getTargetVersion();

            logSummary(count, stopWatch.getTotalTimeMillis(), undoResult.targetSchemaVersion);
        } catch (FlywayException e) {
            callbackExecutor.onMigrateOrUndoEvent(Event.AFTER_UNDO_ERROR);
            throw e;
        }

        callbackExecutor.onMigrateOrUndoEvent(Event.AFTER_UNDO);

        return undoResult;
    }

    private String getTargetVersion() {
        return null;
    }

    private int migrateAll() {
        int total = 0;
        isPreviousVersioned = true;

        while (true) {
            final boolean firstRun = total == 0;
            int count = configuration.isGroup()
                    // With group active a lock on the schema history table has already been acquired.
                    ? migrateGroup(firstRun)
                    // Otherwise acquire the lock now. The lock will be released at the end of each migration.
                    : schemaHistory.lock(() -> migrateGroup(firstRun));
            total += count;
            break;
        }

        if (isPreviousVersioned) {
            callbackExecutor.onMigrateOrUndoEvent(Event.AFTER_VERSIONED);
        }

        return total;
    }

    /**
     * Migrate a group of one (group = false) or more (group = true) migrations.
     *
     * @param firstRun
     *            Whether this is the first time this code runs in this migration run.
     * @return The number of newly applied migrations.
     */
    private Integer migrateGroup(boolean firstRun) {
        MigrationInfoServiceImpl infoService = new MigrationInfoServiceImpl(migrationResolver, schemaHistory, database,
                configuration, configuration.getTarget(), configuration.isOutOfOrder(), configuration.getCherryPick(), true,
                true, true, true);
        infoService.refresh();

        MigrationInfo current = infoService.current();
        MigrationVersion currentSchemaVersion = current == null ? MigrationVersion.EMPTY : current.getVersion();

        LinkedHashMap<MigrationInfoImpl, Boolean> group = new LinkedHashMap<>();
        for (MigrationInfoImpl pendingMigration : infoService.undo()) {
            if (appliedResolvedMigrations.contains(pendingMigration.getResolvedMigration())) {
                continue;
            }

            boolean isOutOfOrder = pendingMigration.getVersion() != null
                    && pendingMigration.getVersion().compareTo(currentSchemaVersion) < 0;

            group.put(pendingMigration, isOutOfOrder);

            if (!configuration.isGroup()) {
                // Only include one pending migration if group is disabled
                break;
            }
        }

        if (!group.isEmpty()) {
            boolean skipExecutingMigrations = false;

            applyMigrations(group, skipExecutingMigrations);
        }
        return group.size();
    }

    private void logSummary(int migrationSuccessCount, long executionTime, String targetVersion) {
        if (migrationSuccessCount == 0) {
            LOG.info("Schema " + schema + " is up to date. No undo necessary.");
            return;
        }

        String targetText = (targetVersion != null) ? ", now at version v" + targetVersion : "";

        String migrationText = (migrationSuccessCount == 1) ? "undo" : "undos";

        LOG.info("Successfully applied " + migrationSuccessCount + " " + migrationText + " to schema " + schema
                + targetText + " (execution time " + TimeFormat.format(executionTime) + ")");
    }

    /**
     * Applies this migration to the database. The migration state and the execution time are updated accordingly.
     */
    private void applyMigrations(final LinkedHashMap<MigrationInfoImpl, Boolean> group,
                                 boolean skipExecutingMigrations) {
        boolean executeGroupInTransaction = isExecuteGroupInTransaction(group);
        final StopWatch stopWatch = new StopWatch();
        try {
            if (executeGroupInTransaction) {
                ExecutionTemplateFactory.createExecutionTemplate(connectionUserObjects.getJdbcConnection(), database)
                        .execute(() -> {
                            doMigrateGroup(group, stopWatch, skipExecutingMigrations, true);
                            return null;
                        });
            } else {
                doMigrateGroup(group, stopWatch, skipExecutingMigrations, false);
            }
        } catch (FlywayMigrateException e) {
            MigrationInfoImpl migration = e.getMigration();
            String failedMsg = "Undo of " + toMigrationText(migration, e.isOutOfOrder()) + " failed!";
            if (database.supportsDdlTransactions() && executeGroupInTransaction) {
                LOG.error(failedMsg + " Changes successfully rolled back.");
            } else {
                LOG.error(failedMsg + " Please restore backups and roll back database and code!");

                stopWatch.stop();
                int executionTime = (int)stopWatch.getTotalTimeMillis();
                schemaHistory.addAppliedMigration(migration.getVersion(), migration.getDescription(),
                        migration.getType(), migration.getScript(), migration.getResolvedMigration().getChecksum(),
                        executionTime, false);
            }
            throw e;
        }
    }

    private boolean isExecuteGroupInTransaction(LinkedHashMap<MigrationInfoImpl, Boolean> group) {
        boolean executeGroupInTransaction = true;
        boolean first = true;

        for (Map.Entry<MigrationInfoImpl, Boolean> entry : group.entrySet()) {
            ResolvedMigration resolvedMigration = entry.getKey().getResolvedMigration();
            boolean inTransaction = resolvedMigration.getExecutor().canExecuteInTransaction();

            if (first) {
                executeGroupInTransaction = inTransaction;
                first = false;
                continue;
            }

            if (!configuration.isMixed() && executeGroupInTransaction != inTransaction) {
                throw new FlywayException(
                        "Detected both transactional and non-transactional migrations within the same migration group"
                                + " (even though mixed is false). First offending migration: "
                                + doQuote((resolvedMigration.getVersion() == null ? "" : resolvedMigration.getVersion())
                                + (StringUtils.hasLength(resolvedMigration.getDescription())
                                ? " " + resolvedMigration.getDescription() : ""))
                                + (inTransaction ? "" : " [non-transactional]"));
            }

            executeGroupInTransaction &= inTransaction;
        }

        return executeGroupInTransaction;
    }

    private void doMigrateGroup(LinkedHashMap<MigrationInfoImpl, Boolean> group, StopWatch stopWatch,
                                boolean skipExecutingMigrations, boolean isExecuteInTransaction) {
        Context context = new Context() {
            @Override
            public Configuration getConfiguration() {
                return configuration;
            }

            @Override
            public java.sql.Connection getConnection() {
                return connectionUserObjects.getJdbcConnection();
            }
        };

        for (Map.Entry<MigrationInfoImpl, Boolean> entry : group.entrySet()) {
            final MigrationInfoImpl migration = entry.getKey();
            boolean isOutOfOrder = entry.getValue();

            final String migrationText = toMigrationText(migration, isOutOfOrder);

            stopWatch.start();

            if (isPreviousVersioned && migration.getVersion() == null) {
                callbackExecutor.onMigrateOrUndoEvent(Event.AFTER_VERSIONED);
                callbackExecutor.onMigrateOrUndoEvent(Event.BEFORE_REPEATABLES);
                isPreviousVersioned = false;
            }

            if (skipExecutingMigrations) {
                LOG.debug("Skipping execution of undo of " + migrationText);
            } else {
                LOG.debug("Starting undo of " + migrationText + " ...");

                connectionUserObjects.restoreOriginalState();
                connectionUserObjects.changeCurrentSchemaTo(schema);

                try {
                    callbackExecutor.setMigrationInfo(migration);
                    callbackExecutor.onEachMigrateOrUndoEvent(Event.BEFORE_EACH_UNDO);

                    try {
                        LOG.info("undoing " + migrationText);
                        // With single connection databases we need to manually disable the transaction for the
                        // migration as it is turned on for schema history changes
                        boolean oldAutoCommit = context.getConnection().getAutoCommit();
                        if (database.useSingleConnection() && !isExecuteInTransaction) {
                            context.getConnection().setAutoCommit(true);
                        }
                        migration.getResolvedMigration().getExecutor().execute(context);
                        if (database.useSingleConnection() && !isExecuteInTransaction) {
                            context.getConnection().setAutoCommit(oldAutoCommit);
                        }

                        appliedResolvedMigrations.add(migration.getResolvedMigration());
                    } catch (FlywayException e) {
                        callbackExecutor.onEachMigrateOrUndoEvent(Event.AFTER_EACH_UNDO_ERROR);
                        throw new FlywayMigrateException(migration, isOutOfOrder, e);

                    } catch (SQLException e) {
                        callbackExecutor.onEachMigrateOrUndoEvent(Event.AFTER_EACH_UNDO_ERROR);
                        throw new FlywayMigrateException(migration, isOutOfOrder, e);
                    }

                    LOG.debug("Successfully completed undo of " + migrationText);
                    callbackExecutor.onEachMigrateOrUndoEvent(Event.AFTER_EACH_UNDO);
                } finally {
                    callbackExecutor.setMigrationInfo(null);
                }
            }

            stopWatch.stop();
            //int executionTime = (int)stopWatch.getTotalTimeMillis();

            //undoResult.migrations.add(CommandResultFactory.createMigrateOutput(migration, executionTime));

            //schemaHistory.addAppliedMigration(migration.getVersion(), migration.getDescription(), migration.getType(),
            //        migration.getScript(), migration.getResolvedMigration().getChecksum(), executionTime, true);
        }
    }

    private String toMigrationText(MigrationInfoImpl migration, boolean isOutOfOrder) {
        final MigrationExecutor migrationExecutor = migration.getResolvedMigration().getExecutor();
         String migrationText=null;
        if (migration.getVersion() != null) {
            migrationText = "schema " + schema + " to version "
                    + doQuote(migration.getVersion()
                    + (StringUtils.hasLength(migration.getDescription()) ? " - " + migration.getDescription() : ""))
                    + (isOutOfOrder ? " [out of order]" : "")
                    + (migrationExecutor.canExecuteInTransaction() ? "" : " [non-transactional]");
        }
        return migrationText;
    }

    private String doQuote(String text) {
        return "\"" + text + "\"";
    }

    public static class FlywayMigrateException extends FlywayException {
        private final MigrationInfoImpl migration;
        private final boolean outOfOrder;

        FlywayMigrateException(MigrationInfoImpl migration, boolean outOfOrder, SQLException e) {
            super(ExceptionUtils.toMessage(e), e);
            this.migration = migration;
            this.outOfOrder = outOfOrder;
        }

        FlywayMigrateException(MigrationInfoImpl migration, boolean outOfOrder, FlywayException e) {
            super(e.getMessage(), e);
            this.migration = migration;
            this.outOfOrder = outOfOrder;
        }

        public MigrationInfoImpl getMigration() {
            return migration;
        }

        public boolean isOutOfOrder() {
            return outOfOrder;
        }
    }
}