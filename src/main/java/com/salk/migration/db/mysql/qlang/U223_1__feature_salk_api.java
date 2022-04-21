package com.salk.migration.db.mysql.qlang;

import com.salk.migration.extend.AbsJavaMigration;
import com.salk.migration.spring.MigrationSpringContext;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.context.ApplicationContext;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * <p/>
 * Description
 * <p/>
 *
 * @author salkli
 * @date 2022/4/11
 */
public class U223_1__feature_salk_api extends AbsJavaMigration {
    @Override
    public boolean isUndo() {
        return true;
    }
    @Override
    public void doMigration(Context context) throws Exception {
        Connection connection = context.getConnection();
        //获取连接
        Configuration configuration = context.getConfiguration();
        //获取配置信息
        //执行复杂的业务逻辑
        ApplicationContext applicationContextAware = MigrationSpringContext.getApplicationContext();
        System.out.println("spring bean {}" + applicationContextAware);
        System.out.println("extend==============");
    }


    /**
     * 通过proxydb 执行支持多数据查询
     */
    private void executeByProxyDb() {
        String sql = "select 1 from dual";
        Object dbProxy = getDbProxy();
    }

    /**
     * 执行sql
     *
     * @param connection
     * @throws Exception
     */
    private void executeSql(Connection connection) throws Exception {
        try (Statement select = connection.createStatement()) {
            try (ResultSet rows = select.executeQuery("SELECT id FROM person ORDER BY id")) {
                while (rows.next()) {
                    int id = rows.getInt(1);
                    String anonymizedName = "Anonymous" + id;
                    try (Statement update = connection.createStatement()) {
                        update.execute("UPDATE person SET name='" + anonymizedName + "' WHERE id=" + id);
                    }
                }
            }
        }
    }


}
