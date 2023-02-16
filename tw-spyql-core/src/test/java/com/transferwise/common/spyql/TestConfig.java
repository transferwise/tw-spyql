package com.transferwise.common.spyql;

import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
class TestConfig {

  @Bean
  DataSource dataSource() {
    JdbcDataSource dataSource = new JdbcDataSource();
    dataSource.setUrl("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1");
    dataSource.setUser("user");
    dataSource.setPassword("password");

    initDatabase(dataSource);

    return dataSource;
  }

  protected void initDatabase(DataSource dataSource) {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.update("CREATE TABLE PERSON(ID INT PRIMARY KEY, NAME VARCHAR(255) NOT NULL)");

    for (int i = 0; i < 250; i++) {
      jdbcTemplate.update("INSERT INTO PERSON (ID, NAME) VALUES (?,?)", i, "Person Nr " + i);
    }
  }
}
