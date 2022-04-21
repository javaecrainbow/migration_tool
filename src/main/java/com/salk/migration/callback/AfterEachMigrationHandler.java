package com.salk.migration.callback;

import com.salk.migration.extend.undo.UndoContext;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogFactory;
import org.flywaydb.core.internal.jdbc.JdbcUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author salkli
 * @since 2022/1/4
 **/
public class AfterEachMigrationHandler implements Callback {
    private static final Log LOG = LogFactory.getLog(AfterEachMigrationHandler.class);

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.AFTER_EACH_MIGRATE || event == Event.AFTER_EACH_UNDO;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return false;
    }

    @Override
    public void handle(Event event, Context context) {
        if (event == Event.AFTER_EACH_MIGRATE) {
            System.out.println("迁移完成======[" + context.getMigrationInfo().getScript() + "]");
        }
        if (event == Event.AFTER_EACH_UNDO) {
            UndoContext.remove();
            System.out.println("回滚完成======[" + context.getMigrationInfo().getScript() + "]");
            this.deleteTable(context);
        }
    }

    /**
     * 回滚成功后删除历史表对应的V数据
     * @param context
     */
    private void deleteTable(Context context) {
        MigrationInfo info = context.getMigrationInfo();
        Configuration config = context.getConfiguration();
        String version = info.getVersion().toString();
        // 组装表信息
        String tableName = config.getSchemas()[0] + "." + config.getTable();
        PreparedStatement pst = null;
        try {
            pst = context.getConnection().prepareStatement("delete from " + tableName + " where version =?");
            pst.setString(1, version);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            JdbcUtils.closeStatement(pst);
        }
    }

    @Override
    public String getCallbackName() {
        return "after_each";
    }

}
