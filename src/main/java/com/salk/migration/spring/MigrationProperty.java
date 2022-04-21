package com.salk.migration.spring;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * migration自定义配置信息
 * 
 * @author salkli
 * @since 2022/4/15
 **/
@ConfigurationProperties(prefix = "migration")
@Data
public class MigrationProperty {
    /**
     * 灰度环境
     */
    public static final String ENV_GRAY = "gray";
    /**
     * 灰度环境
     */
    public static final String ENV_MASTER = "master";
    /**
     * 自动执行
     */
    public static final String MODEL_AUTO = "auto";
    /**
     * 自动执行
     */
    public static final String MODEL_MANUAL = "manual";
    /**
     * 系统执行和环境企业无关
     */
    public static final String SYS = "__sys__";

    /**
     * 环境 gray/master
     */
    private String env;
    /**
     * 执行类型 auto/manual
     */
    private String exeModel;
    /**
     * 是否跳过单个容器执行错误
     */
    private boolean skipErrorEntSql;

    public boolean isAutoRun() {
        return MODEL_AUTO.equals(this.getExeModel());
    }

}
