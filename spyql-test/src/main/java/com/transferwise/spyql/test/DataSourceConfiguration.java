package com.transferwise.spyql.test;

import com.transferwise.spyql.SpyqlDataSourceProxy;
import com.transferwise.spyql.SpyqlException;
import com.transferwise.spyql.SpyqlHelper;
import com.transferwise.spyql.multicast.AsyncMulticastListener;
import com.transferwise.spyql.listerners.SpyqlLoggingListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;

@Slf4j
@Configuration
public class DataSourceConfiguration implements BeanPostProcessor {

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
	public AsyncMulticastListener multicastListener(DataSource dataSource,
													@Qualifier("synchronousLoggingListener") SpyqlLoggingListener synchronousLoggingListener,
													@Qualifier("asynchronousLoggingListener") SpyqlLoggingListener asynchronousLoggingListener)  throws SQLException, SpyqlException {
		AsyncMulticastListener listener = new AsyncMulticastListener();
		listener.attachListener(synchronousLoggingListener);
		listener.attachListenerAsync(asynchronousLoggingListener);
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
