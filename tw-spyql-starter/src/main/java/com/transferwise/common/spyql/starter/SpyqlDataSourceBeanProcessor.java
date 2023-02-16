package com.transferwise.common.spyql.starter;

import com.transferwise.common.baseutils.ExceptionUtils;
import com.transferwise.common.baseutils.jdbc.ParentAwareDataSourceProxy;
import com.transferwise.common.spyql.SpyqlDataSource;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

public class SpyqlDataSourceBeanProcessor implements BeanPostProcessor, Ordered {

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    return ExceptionUtils.doUnchecked(() -> {
      if (!(bean instanceof DataSource)) {
        return bean;
      }

      var dataSource = (DataSource) bean;

      if (dataSource.isWrapperFor(SpyqlDataSource.class)) {
        return dataSource;
      }

      if (!dataSource.isWrapperFor(HikariDataSource.class)) {
        throw new IllegalStateException("Only Hikari CP is supported.");
      }

      var hikariDataSource = dataSource.unwrap(HikariDataSource.class);

      var databaseName = hikariDataSource.getPoolName();
      if (databaseName == null) {
        throw new IllegalStateException("Hikari's pool name for a database is not set.");
      }

      var spyqlDataSource = new SpyqlDataSource(dataSource, databaseName);
      if (dataSource instanceof ParentAwareDataSourceProxy) {
        ((ParentAwareDataSourceProxy) dataSource).setParentDataSource(spyqlDataSource);
      }

      return spyqlDataSource;
    });
  }

  /**
   * Set it close to top on HikariDataSource.
   */
  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 500;
  }
}
