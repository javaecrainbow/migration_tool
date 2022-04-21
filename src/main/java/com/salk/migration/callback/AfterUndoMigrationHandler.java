package com.salk.migration.callback;

import com.salk.migration.extend.undo.UndoContext;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;

/**
 * @author salkli
 * @since 2022/1/4
 **/
public class AfterUndoMigrationHandler implements Callback {

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.AFTER_UNDO_ERROR || event == Event.AFTER_UNDO;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return false;
    }

    @Override
    public void handle(Event event, Context context) {
        UndoContext.remove();
    }

    @Override
    public String getCallbackName() {
        return "after_undo";
    }

}
