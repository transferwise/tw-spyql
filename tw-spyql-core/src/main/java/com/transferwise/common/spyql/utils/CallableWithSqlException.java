package com.transferwise.common.spyql.utils;

import java.sql.SQLException;

@FunctionalInterface
public interface CallableWithSqlException<T> {

  T call() throws SQLException;
}
