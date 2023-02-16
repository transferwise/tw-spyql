package com.transferwise.common.spyql.starter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.transferwise.common.spyql.SpyqlDataSource;
import com.transferwise.common.spyql.starter.test.TestApplication;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TestApplication.class)
class AutoConfigurationIntTest {

  @Autowired
  private DataSource dataSource;

  @Test
  void testIfDataSourceIsCorrectlyWrapped() {
    var spyqlDataSource = (SpyqlDataSource) dataSource;

    var hikariDataSource = spyqlDataSource.getTargetDataSource();
    assertNotNull(hikariDataSource);
  }
}
