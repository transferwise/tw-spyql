package com.transferwise.common.spyql.starter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpyqlConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public SpyqlDataSourceBeanProcessor gafferJtaDataSourceBeanProcessor() {
    return new SpyqlDataSourceBeanProcessor();
  }
}
