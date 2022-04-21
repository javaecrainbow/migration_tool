package com.salk.migration.configuration;

import com.qpaas.migration.callback.AfterEachMigrationHandler;
import com.qpaas.migration.callback.AfterMigrationHandler;
import com.qpaas.migration.callback.AfterUndoMigrationHandler;
import com.qpaas.migration.callback.BeforeEachMigrationHandler;
import com.qpaas.migration.ent.EnterpriseMgr;
import com.qpaas.migration.provider.MigrationScannerProvider;
import com.qpaas.migration.spring.MigrationProperty;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.internal.scanner.LocationScannerCache;
import org.flywaydb.core.internal.scanner.ResourceNameCache;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * <p/>
 * Description
 * <p/>
 *
 * @author salkli
 * @date 2022/4/8
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({FlywayProperties.class, MigrationProperty.class})
public class MigrationConfiguration {
    @Autowired
    private FlywayProperties flywayProperties;
    @Autowired
    private MigrationProperty migrationProperty;
    @Value("${spring.datasource.url}")
    private String datasourceUrl;
    @Value("${spring.datasource.username}")
    private String databaseUsername;
    @Value("${spring.datasource.password}")
    private String databasePassword;

    @Bean("buildEntEntity")
    EnterpriseMgr buildEntEntity() {
        // 根据配置加载，如果是指定企业，entpriseManger为制定的企业列表
        return new EnterpriseMgr(datasourceUrl, databaseUsername, databasePassword);
    }

    @Bean
    Flyway flyway() {
        ClassicConfiguration configuration = new ClassicConfiguration();
        configuration.setDataSource(datasourceUrl, databaseUsername, databasePassword);
        configuration.setSchemas(flywayProperties.getSchemas().toArray(new String[0]));
        configuration.setLocationsAsStrings(flywayProperties.getLocations().toArray(new String[0]));
        BeanUtils.copyProperties(flywayProperties, configuration, "batch", "errorOverrides", "oracleSqlplus",
            "oracleSqlplusWarn", "stream", "undoSqlMigrationPrefix","licenseKey");
        MigrationScannerProvider<JavaMigration> scanner = new MigrationScannerProvider<>(JavaMigration.class,
            Arrays.asList(configuration.getLocations()), configuration.getClassLoader(), configuration.getEncoding(),
            configuration.getDetectEncoding(), false, new ResourceNameCache(), new LocationScannerCache(),
            configuration.getFailOnMissingLocations(), migrationProperty.getEnv());
        configuration.setResourceProvider(scanner);
        configuration.setJavaMigrationClassProvider(scanner);
        configuration.setCallbacks(new AfterMigrationHandler(), new AfterEachMigrationHandler(),
            new BeforeEachMigrationHandler(), new AfterUndoMigrationHandler());
        Flyway flyway = Flyway.configure().configuration(configuration).load();
        return flyway;
    }

}