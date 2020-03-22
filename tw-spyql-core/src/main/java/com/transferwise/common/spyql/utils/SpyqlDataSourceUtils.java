package com.transferwise.common.spyql.utils;

import com.transferwise.common.spyql.SpyqlDataSource;
import com.transferwise.common.spyql.listener.SpyqlConnectionListener;
import com.transferwise.common.spyql.listener.SpyqlDataSourceListener;
import java.sql.SQLException;
import javax.sql.DataSource;

public class SpyqlDataSourceUtils {

  public static void addDataSourceListener(DataSource dataSource, SpyqlDataSourceListener listener) throws SQLException {
    if (dataSource.isWrapperFor(SpyqlDataSource.class)) {
      dataSource
          .unwrap(SpyqlDataSource.class)
          .addListener(listener);
    } else {
      throw new SQLException("Unable to attach listener to the dataSource because it doesn't have SpyqlDataSource in the proxy chain");
    }
  }

  public static void addDataSourceListener(DataSource dataSource, SpyqlConnectionListener listener) throws SQLException {
    addDataSourceListener(dataSource, result -> listener);
  }
}
