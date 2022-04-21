package com.salk.migration.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * <p/>
 * migration项目的spring上下文
 * <p/>
 *
 * @author salkli
 * @date 2022/4/12
 */
@Component("MigrationSpringContext")
public class MigrationSpringContext implements ApplicationContextAware {
    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static <T> T getBean(Class<T> tClass) {
        return context.getBean(tClass);
    }

    public static ApplicationContext getApplicationContext() {
        return context;
    }
}
