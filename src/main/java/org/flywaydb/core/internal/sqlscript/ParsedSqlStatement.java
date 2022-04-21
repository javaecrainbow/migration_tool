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
package org.flywaydb.core.internal.sqlscript;

import com.qpaas.migration.ent.EnterpriseMgr;
import com.qpaas.migration.spring.MigrationProperty;
import com.qpaas.migration.spring.MigrationSpringContext;
import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogFactory;
import org.flywaydb.core.internal.jdbc.JdbcTemplate;
import org.flywaydb.core.internal.jdbc.Results;

import java.util.List;

/**
 * A sql statement from a script that can be executed at once against a database.
 */
public class ParsedSqlStatement implements SqlStatement {
    private static final Log LOG = LogFactory.getLog(ParsedSqlStatement.class);

    private final int pos;
    private final int line;
    private final int col;
    private final String sql;

    public int getPos() {
        return pos;
    }

    public int getLine() {
        return line;
    }

    public int getCol() {
        return col;
    }

    /**
     * The delimiter of the statement.
     */
    private final Delimiter delimiter;

    private final boolean canExecuteInTransaction;

    public ParsedSqlStatement(int pos, int line, int col, String sql, Delimiter delimiter,
        boolean canExecuteInTransaction

    ) {
        this.pos = pos;
        this.line = line;
        this.col = col;
        this.sql = sql;
        this.delimiter = delimiter;
        this.canExecuteInTransaction = canExecuteInTransaction;

    }

    @Override
    public final int getLineNumber() {
        return line;
    }

    @Override
    public final String getSql() {
        return sql;
    }

    @Override
    public String getDelimiter() {
        return delimiter.toString();
    }

    @Override
    public boolean canExecuteInTransaction() {
        return canExecuteInTransaction;
    }

    private MigrationProperty migrationProperty =
        MigrationSpringContext.getApplicationContext().getBean(MigrationProperty.class);

    @Override
    public Results execute(JdbcTemplate jdbcTemplate) {

        String env = migrationProperty.getEnv();
        List<String> entIds = EnterpriseMgr.getEntId(env);
        Results out = new Results();
        for (String entId : entIds) {
            if (!sql.contains("#{entId}#")) {
                return jdbcTemplate.executeStatement(sql);
            }
            String afterReplace = sql.replaceAll("#\\{entId\\}#", entId);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing entId:[" + entId + "] SQL: " + sql);
            }
            Results results = jdbcTemplate.executeStatement(afterReplace);
            if (results.getException() != null) {
                if (!migrationProperty.isSkipErrorEntSql()) {
                    return results;
                }
                results.getResults().forEach(item -> out.addResult(item));
                out.setException(results.getException());
                results.getWarnings().forEach(item2 -> out.addWarning(item2));
                results.getErrors().forEach(item2 -> out.addError(item2));
            }
        }
        return out;
    }

}