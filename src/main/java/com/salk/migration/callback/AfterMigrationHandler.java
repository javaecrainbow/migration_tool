package com.salk.migration.callback;

import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogFactory;

/**
 * @author salkli
 * @since 2022/1/4
 **/
public class AfterMigrationHandler implements Callback {
    private static final Log LOG = LogFactory.getLog(AfterMigrationHandler.class);

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.AFTER_MIGRATE;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return false;
    }

    @Override
    public void handle(Event event, Context context) {
        System.err.println("大吉大利，迁移完成=================================================");
    }

    @Override
    public String getCallbackName() {
        return "after_migration";
    }

}
