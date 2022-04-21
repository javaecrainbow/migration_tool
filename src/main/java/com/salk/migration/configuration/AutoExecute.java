package com.salk.migration.configuration;

import com.qpaas.migration.spring.MigrationProperty;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 自动执行
 */
@Component
@DependsOn(value = {"MigrationSpringContext", "buildEntEntity", "EnterpriseMgr"})
public class AutoExecute {
    @Autowired
    private MigrationProperty migrationProperty;
    
    @Resource
    private Flyway flyway;

    @PostConstruct
    public void init() {
        if ("auto".equals(migrationProperty.getExeModel())) {
            flyway.migrate();
        }
    }

}
