package com.transferwise.common.spyql.starter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpyqlAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public static SpyqlDataSourceBeanProcessor spyqlDataSourceBeanProcessor() {
    return new SpyqlDataSourceBeanProcessor();
  }
}
