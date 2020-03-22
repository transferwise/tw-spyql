package com.transferwise.common.spyql;

import com.transferwise.common.spyql.utils.CallableWithSqlException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

public class SpyqlStatement implements Statement {

  protected Statement statement;
  protected SpyqlConnection spyqlConnection;
  protected String batchSql;

  public SpyqlStatement(Statement statement, SpyqlConnection spyqlConnection) {
    this.statement = statement;
    this.spyqlConnection = spyqlConnection;
  }

  protected <T> T executeStatement(String sql, CallableWithSqlException<T> callable) throws SQLException {
    long startTimeNs = System.nanoTime();
    try {
      T result = callable.call();
      long rowsAffected = 0;
      if (result instanceof Integer) {
        rowsAffected = (Integer) result;
      } else if (result instanceof Long) {
        rowsAffected = (Long) result;
      } else if (result instanceof int[]) {
        for (int i : (int[]) result) {
          rowsAffected += i;
        }
      } else if (result instanceof long[]) {
        for (long i : (long[]) result) {
          rowsAffected += i;
        }
      }
      spyqlConnection.onStatementExecute(System.nanoTime() - startTimeNs, sql, rowsAffected, null);
      return result;
    } catch (Throwable t) {
      spyqlConnection.onStatementExecute(System.nanoTime() - startTimeNs, sql, 0, t);
      throw t;
    }
  }

  @Override
  public int executeUpdate(String sql) throws java.sql.SQLException {
    return executeStatement(sql, () -> statement.executeUpdate(sql));
  }

  @Override
  public int executeUpdate(String sql, int autoGeneratedKeys) throws java.sql.SQLException {
    return executeStatement(sql, () -> statement.executeUpdate(sql, autoGeneratedKeys));
  }

  @Override
  public int executeUpdate(String sql, int[] columnIndexes) throws java.sql.SQLException {
    return executeStatement(sql, () -> statement.executeUpdate(sql, columnIndexes));
  }

  @Override
  public int executeUpdate(String sql, String[] columnNames) throws java.sql.SQLException {
    return executeStatement(sql, () -> statement.executeUpdate(sql, columnNames));
  }


  @Override
  public long[] executeLargeBatch() throws java.sql.SQLException {
    return executeStatement(batchSql, () -> statement.executeLargeBatch());
  }

  @Override
  public long executeLargeUpdate(String sql, int[] columnIndexes) throws java.sql.SQLException {
    return executeStatement(sql, () -> statement.executeLargeUpdate(sql, columnIndexes));
  }

  @Override
  public long executeLargeUpdate(String sql, String[] columnNames) throws java.sql.SQLException {
    return executeStatement(sql, () -> statement.executeLargeUpdate(sql, columnNames));
  }

  @Override
  public long executeLargeUpdate(String sql, int autoGeneratedKeys) throws java.sql.SQLException {
    return executeStatement(sql, () -> statement.executeLargeUpdate(sql, autoGeneratedKeys));
  }

  @Override
  public long executeLargeUpdate(String sql) throws java.sql.SQLException {
    return executeStatement(sql, () -> statement.executeLargeUpdate(sql));
  }

  @Override
  public void addBatch(String sql) throws java.sql.SQLException {
    this.batchSql = sql;
    statement.addBatch(sql);
  }

  @Override
  public ResultSet executeQuery(String sql) throws java.sql.SQLException {
    return new SpyqlResultSet(executeStatement(sql, () -> statement.executeQuery(sql)), spyqlConnection);
  }

  @Override
  public boolean execute(String sql, int autoGeneratedKeys) throws java.sql.SQLException {
    return executeStatement(sql, () -> statement.execute(sql, autoGeneratedKeys));
  }

  @Override
  public boolean execute(String sql) throws java.sql.SQLException {
    return executeStatement(sql, () -> statement.execute(sql));
  }

  @Override
  public boolean execute(String sql, String[] columnNames) throws java.sql.SQLException {
    return executeStatement(sql, () -> statement.execute(sql, columnNames));
  }

  @Override
  public boolean execute(String sql, int[] columnIndexes) throws java.sql.SQLException {
    return executeStatement(sql, () -> statement.execute(sql, columnIndexes));
  }

  @Override
  public int[] executeBatch() throws java.sql.SQLException {
    return executeStatement(batchSql, () -> statement.executeBatch());
  }

  @Override
  public void setEscapeProcessing(boolean enable) throws java.sql.SQLException {
    statement.setEscapeProcessing(enable);
  }

  @Override
  public void clearBatch() throws java.sql.SQLException {
    statement.clearBatch();
  }

  @Override
  public long getLargeMaxRows() throws java.sql.SQLException {
    return statement.getLargeMaxRows();
  }

  @Override
  public void setLargeMaxRows(long max) throws java.sql.SQLException {
    statement.setLargeMaxRows(max);
  }

  @Override
  public int getFetchSize() throws java.sql.SQLException {
    return statement.getFetchSize();
  }

  @Override
  public boolean isCloseOnCompletion() throws java.sql.SQLException {
    return statement.isCloseOnCompletion();
  }

  @Override
  public SQLWarning getWarnings() throws java.sql.SQLException {
    return statement.getWarnings();
  }

  @Override
  public void setMaxFieldSize(int max) throws java.sql.SQLException {
    statement.setMaxFieldSize(max);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws java.sql.SQLException {
    return statement.isWrapperFor(iface);
  }

  @Override
  public void setCursorName(String name) throws java.sql.SQLException {
    statement.setCursorName(name);
  }

  @Override
  public ResultSet getGeneratedKeys() throws java.sql.SQLException {
    return new SpyqlResultSet(statement.getGeneratedKeys(), spyqlConnection);
  }

  @Override
  public int getResultSetHoldability() throws java.sql.SQLException {
    return statement.getResultSetHoldability();
  }

  @Override
  public boolean isClosed() throws java.sql.SQLException {
    return statement.isClosed();
  }

  @Override
  public void clearWarnings() throws java.sql.SQLException {
    statement.clearWarnings();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws java.sql.SQLException {
    return statement.unwrap(iface);
  }

  @Override
  public ResultSet getResultSet() throws java.sql.SQLException {
    return new SpyqlResultSet(statement.getResultSet(), spyqlConnection);
  }

  @Override
  public int getResultSetType() throws java.sql.SQLException {
    return statement.getResultSetType();
  }

  @Override
  public int getUpdateCount() throws java.sql.SQLException {
    return statement.getUpdateCount();
  }

  @Override
  public void setQueryTimeout(int seconds) throws java.sql.SQLException {
    statement.setQueryTimeout(seconds);
  }

  @Override
  public long getLargeUpdateCount() throws java.sql.SQLException {
    return statement.getLargeUpdateCount();
  }

  @Override
  public void setFetchDirection(int direction) throws java.sql.SQLException {
    statement.setFetchDirection(direction);
  }

  @Override
  public int getResultSetConcurrency() throws java.sql.SQLException {
    return statement.getResultSetConcurrency();
  }

  @Override
  public int getMaxRows() throws java.sql.SQLException {
    return statement.getMaxRows();
  }

  @Override
  public Connection getConnection() {
    return spyqlConnection;
  }

  @Override
  public int getQueryTimeout() throws java.sql.SQLException {
    return statement.getQueryTimeout();
  }

  @Override
  public void cancel() throws java.sql.SQLException {
    statement.cancel();
  }

  @Override
  public void setMaxRows(int max) throws java.sql.SQLException {
    statement.setMaxRows(max);
  }

  @Override
  public boolean getMoreResults() throws java.sql.SQLException {
    return statement.getMoreResults();
  }

  @Override
  public boolean getMoreResults(int current) throws java.sql.SQLException {
    return statement.getMoreResults(current);
  }

  @Override
  public void closeOnCompletion() throws java.sql.SQLException {
    statement.closeOnCompletion();
  }

  @Override
  public int getFetchDirection() throws java.sql.SQLException {
    return statement.getFetchDirection();
  }

  @Override
  public void setPoolable(boolean poolable) throws java.sql.SQLException {
    statement.setPoolable(poolable);
  }

  @Override
  public void setFetchSize(int rows) throws java.sql.SQLException {
    statement.setFetchSize(rows);
  }

  @Override
  public void close() throws java.sql.SQLException {
    statement.close();
  }

  @Override
  public boolean isPoolable() throws java.sql.SQLException {
    return statement.isPoolable();
  }

  @Override
  public int getMaxFieldSize() throws java.sql.SQLException {
    return statement.getMaxFieldSize();
  }
}
