package com.salk.migration.extend;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * <p/>
 * qpaas java 接口式迁移
 * <p/>
 *
 * @author salkli
 * @date 2022/4/12
 */
public abstract class AbsJavaMigration extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        // 执行迁移

        doMigration(context);

    }

    /**
     * 获取代理执行器
     *
     * @return
     */
    protected Object getDbProxy() {

        return new Object();
    }

    /**
     * 执行java api迁移
     *
     * @param context
     */
    public abstract void doMigration(Context context) throws Exception;

}
