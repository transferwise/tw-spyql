# Tw-Spyql documentation

A professional spy for your JDBC data source. It's a `javax.sql.DataSource` wrapper, which provides multiple event hooks.

It doesn't provide any implementation out of the box. Users are expected to provide the implementation. However, there is an example project `tw-spyql-simple-logger` which logs certain events.

## Usage

If you are using a Spring Boot service then the following steps apply:

1. Add a dependency `com.transferwise.common:tw-spyql-starter`. It will automatically wrap your data sources, exposed as beans, with `SpyqlDataSource`.
1. Add a Spring configuration which gets all the `DataSource` beans, unwrap the `SpySqlDataSource` and add your custom `SpyqlDataSourceListener` objects. Example snippet:
   ```
   @Configuration
   public class ExampleBeanPostProcessor implements BeanPostProcessor {

       @Override
       public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
         return bean;
       }

       @Override
       public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
         if (!(bean instanceof DataSource)) {
           return bean;
         }
         SpyqlDataSource dataSource = ((DataSource) bean).unwrap(SpyqlDataSource.class);
         dataSource.addListener(new MySpyqlDataSourceListener());
       }
   }
   ```

## Features

Datasource that is wrapped into `SpyqlDataSource` allows you to add listeners to different events related to datasources and execute actions during those events.

For example, you can execute an action when a statement is executed, transaction is commited or rolled back.
Reference `SpyqlDataSourceListener` and `SpyqlConnectionListener` interfaces to see which event listeners are possible.