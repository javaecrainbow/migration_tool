package com.salk.migration.command;

import com.salk.migration.spring.MigrationProperty;
import com.salk.migration.spring.MigrationSpringContext;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.core.api.output.RepairResult;
import org.flywaydb.core.internal.info.MigrationInfoDumper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

/**
 * <p/>
 * shell命令处理
 * <p/>
 *
 * @author salkli
 * @date 2022/4/8
 */
@ShellComponent
public class MigrationCommand {
    @Autowired
    private Flyway flyway;

    @Autowired
    private MigrationProperty migrationProperty;

    /**
     * 执行迁移操作
     * 
     * @return
     */
    @ShellMethod("migration")
    public MigrateResult migration() {
        MigrateResult migrate = flyway.migrate();
        return migrate;
    }

    /**
     * 修复跑失败的数据
     * 
     * @return
     */
    @ShellMethod("repair")
    public String repair() {
        RepairResult repair = flyway.repair();
        return repair.toString();
    }

    /**
     * 查看当前运行环境
     * 
     * @return
     */
    @ShellMethod("env")
    public String env() {
        return migrationProperty.getEnv();
    }

    /**
     * 关闭
     */
    @ShellMethod("shutdown")
    public void stop() {
        ConfigurableApplicationContext ctx =
            (ConfigurableApplicationContext) MigrationSpringContext.getApplicationContext();
        ctx.close();
    }

    /**
     * 回退操作，目前只针对单个执行脚本
     */
    @ShellMethod("undo")
    public void undo(@ShellOption String scriptPath) {
        flyway.undo(scriptPath);

    }

    @ShellMethod("info")
    public void info() {
        MigrationInfoService info = flyway.info();
        MigrationInfo current = info.current();
        MigrationVersion currentSchemaVersion = current == null ? MigrationVersion.EMPTY : current.getVersion();
        MigrationVersion schemaVersionToOutput =
            currentSchemaVersion == null ? MigrationVersion.EMPTY : currentSchemaVersion;
        System.out.println("Schema version: " + schemaVersionToOutput);
        System.out.println("");
        MigrationInfo[] infos = info.all();
        System.out.println(MigrationInfoDumper.dumpToAsciiTable(infos));
    }

}
