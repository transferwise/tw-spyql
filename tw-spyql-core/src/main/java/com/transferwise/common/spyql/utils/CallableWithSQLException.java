package com.transferwise.common.spyql.utils;

import java.sql.SQLException;

@FunctionalInterface
public interface CallableWithSQLException<T> {
    T call() throws SQLException;
}
