package com.salk.migration.spring;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.stereotype.Component;

/**
 * <p>
 * flyway自动执行策略.无策略。
 * 基于springboot的auto config模式自定执行migrate()
 * </p>
 * 
 * @author salkli
 * @since 2022/4/15
 **/
@Component
public class FlywayMigrationConditionStrategy implements FlywayMigrationStrategy {
    @Override
    public void migrate(Flyway flyway) {
        return;
    }
}
