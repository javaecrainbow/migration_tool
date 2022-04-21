package com.salk.migration.callback;

import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;

/**
 * @author salkli
 * @since 2022/1/4
 **/
public class BeforeEachMigrationHandler implements Callback {

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.BEFORE_EACH_MIGRATE || event == Event.BEFORE_EACH_UNDO;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return false;
    }

    @Override
    public void handle(Event event, Context context) {
        if(event == Event.BEFORE_EACH_UNDO){
            System.out.println("正在执行回滚====[" + context.getMigrationInfo().getScript() + "]");
        }
        if(event == Event.BEFORE_EACH_MIGRATE){
            System.out.println("正在执行迁移====[" + context.getMigrationInfo().getScript() + "]");
        }
    }

    @Override
    public String getCallbackName() {
        return "before";
    }

}
