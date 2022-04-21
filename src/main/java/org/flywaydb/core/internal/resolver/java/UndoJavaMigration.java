package org.flywaydb.core.internal.resolver.java;

import org.flywaydb.core.api.MigrationType;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.internal.resolver.ResolvedMigrationImpl;
import org.flywaydb.core.internal.util.ClassUtils;

/**
 * <p/>
 * Description
 * <p/>
 *
 * @author salkli
 * @date 2022/4/19
 */
public class UndoJavaMigration extends ResolvedMigrationImpl {

    public UndoJavaMigration(JavaMigration javaMigration) {
        super(javaMigration.getVersion(),
                javaMigration.getDescription(),
                javaMigration.getClass().getName(),
                javaMigration.getChecksum(),
                null,
                MigrationType.UNDO_JDBC,
                ClassUtils.getLocationOnDisk(javaMigration.getClass()),
                new JavaMigrationExecutor(javaMigration)
        );
    }
}
