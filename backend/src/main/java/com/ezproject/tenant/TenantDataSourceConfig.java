package com.ezproject.tenant;

import javax.sql.DataSource;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Enveloppe la DataSource pour activer le Row-Level Security (désactivable : app.rls.enabled=false). */
@Configuration
@ConditionalOnProperty(name = "app.rls.enabled", havingValue = "true", matchIfMissing = true)
class TenantDataSourceConfig {

    @Bean
    static BeanPostProcessor tenantDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                return bean instanceof DataSource ds && !(bean instanceof TenantAwareDataSource)
                        ? new TenantAwareDataSource(ds) : bean;
            }
        };
    }
}
