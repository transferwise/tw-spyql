package com.transferwise.spyql;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

@Configuration
class TestConfig {

	@PostConstruct
	void init() {
		Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		root.setLevel(Level.DEBUG);
	}

	@Bean
	DataSource dataSource() {
		JdbcDataSource dataSource = new JdbcDataSource();
		dataSource.setUrl("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1");
		dataSource.setUser("user");
		dataSource.setPassword("password");
		return dataSource;
	}
}
