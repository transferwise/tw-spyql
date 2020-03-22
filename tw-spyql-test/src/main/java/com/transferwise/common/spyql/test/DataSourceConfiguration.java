package com.transferwise.common.spyql.test;

import com.transferwise.common.spyql.SpyqlDataSource;
import com.transferwise.common.spyql.listerners.SpyqlLoggingListener;
import com.transferwise.common.spyql.rx.ObservableListener;
import com.transferwise.common.spyql.utils.SpyqlDataSourceUtils;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfiguration implements BeanPostProcessor {

  private static final Logger log = LoggerFactory.getLogger(DataSourceConfiguration.class);

  @Bean
  @Qualifier("synchronousLoggingListener")
  public SpyqlLoggingListener synchronousLoggingListener() {
    return new SpyqlLoggingListener();
  }

  @Bean
  @Qualifier("asynchronousLoggingListener")
  public SpyqlLoggingListener asynchronousLoggingListener() {
    return new SpyqlLoggingListener();
  }

  @Bean
  public ObservableListener rxListener(DataSource dataSource,
      @Qualifier("synchronousLoggingListener") SpyqlLoggingListener synchronousLoggingListener,
      @Qualifier("asynchronousLoggingListener") SpyqlLoggingListener asynchronousLoggingListener) throws SQLException {
    ObservableListener listener = new ObservableListener();
    listener.attachListener(synchronousLoggingListener);
    listener.attachAsyncListener(asynchronousLoggingListener);
    SpyqlDataSourceUtils.addDataSourceListener(dataSource, listener);
    return listener;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof DataSource) {
      log.info("Wrapping bean '{}' ({}) into SpyqlDataSourceProxy", beanName, bean.getClass().getName());
      DataSource dataSource = (DataSource) bean;
      return new SpyqlDataSource(dataSource);
    }
    return bean;
  }

}
