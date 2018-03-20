package com.transferwise.spyql.test;

import com.transferwise.spyql.SpyqlDataSourceProxy;
import com.transferwise.spyql.SpyqlException;
import com.transferwise.spyql.SpyqlHelper;
import com.transferwise.spyql.listerners.SpyqlLoggingListener;
import com.transferwise.spyql.multicast.MulticastListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;

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
	public MulticastListener multicastListener(DataSource dataSource,
											   @Qualifier("synchronousLoggingListener") SpyqlLoggingListener synchronousLoggingListener,
											   @Qualifier("asynchronousLoggingListener") SpyqlLoggingListener asynchronousLoggingListener) throws SQLException, SpyqlException {
		MulticastListener listener = new MulticastListener();
		listener.attachListener(synchronousLoggingListener);
		listener.attachAsyncListener(asynchronousLoggingListener);
		SpyqlHelper.setDataSourceListener(dataSource, listener);
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
			return new SpyqlDataSourceProxy(dataSource);
		}
		return bean;
	}

}
