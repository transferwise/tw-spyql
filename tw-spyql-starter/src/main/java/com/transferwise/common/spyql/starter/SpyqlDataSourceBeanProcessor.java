package com.transferwise.common.spyql.starter;

import com.transferwise.common.baseutils.ExceptionUtils;
import com.transferwise.common.baseutils.jdbc.DataSourceProxyUtils;
import com.transferwise.common.spyql.SpyqlDataSource;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

@Slf4j
public class SpyqlDataSourceBeanProcessor implements BeanPostProcessor, Ordered {

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    return ExceptionUtils.doUnchecked(() -> {
      if (!(bean instanceof DataSource)) {
        return bean;
      }

      var dataSource = (DataSource) bean;

      if (dataSource.isWrapperFor(SpyqlDataSource.class)) {
        // No need to add starter library, if it is already wrapped.
        log.warn("Datasource '" + dataSource + "' is already wrapped with `SpyqlDataSource`.");
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
      DataSourceProxyUtils.tieTogether(spyqlDataSource, dataSource);

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
