package com.transferwise.common.spyql;

import com.transferwise.common.spyql.event.ConnectionCloseEvent;
import com.transferwise.common.spyql.event.ConnectionCloseFailureEvent;
import com.transferwise.common.spyql.event.ResultSetNextRowsEvent;
import com.transferwise.common.spyql.event.StatementExecuteEvent;
import com.transferwise.common.spyql.event.StatementExecuteFailureEvent;
import com.transferwise.common.spyql.event.TransactionBeginEvent;
import com.transferwise.common.spyql.event.TransactionCommitEvent;
import com.transferwise.common.spyql.event.TransactionCommitFailureEvent;
import com.transferwise.common.spyql.event.TransactionRollbackEvent;
import com.transferwise.common.spyql.event.TransactionRollbackFailureEvent;
import com.transferwise.common.spyql.listener.SpyqlConnectionListener;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SpyqlConnection implements Connection {

  private List<SpyqlConnectionListener> connectionListeners;
  private Connection connection;
  private SpyqlDataSource spyqlDataSource;
  private long connectionId;
  private Long transactionId;

  public SpyqlConnection(SpyqlDataSource spyqlDataSource, Connection connection, List<SpyqlConnectionListener> connectionListeners,
      long connectionId) {
    this.connection = connection;
    this.connectionListeners = connectionListeners;
    this.spyqlDataSource = spyqlDataSource;
    this.connectionId = connectionId;
  }

  @Override
  public void close() throws SQLException {
    long startTimeNs = System.nanoTime();
    try {
      connection.close();
      onClose(System.nanoTime() - startTimeNs, null);
    } catch (Throwable t) {
      onClose(System.nanoTime() - startTimeNs, t);
      throw t;
    }
  }

  @Override
  public void setAutoCommit(boolean autoCommit) throws SQLException {
    boolean implicitCommit = autoCommit && !connection.getAutoCommit() && isInTransaction();
    if (!implicitCommit) {
      connection.setAutoCommit(autoCommit);
    } else {
      long startTimeNs = System.nanoTime();
      try {
        connection.setAutoCommit(autoCommit);
        onCommit(System.nanoTime() - startTimeNs, null);
      } catch (Throwable t) {
        onCommit(System.nanoTime() - startTimeNs, t);
        throw t;
      }
    }
  }

  @Override
  public void rollback() throws SQLException {
    long startTimeNs = System.nanoTime();
    try {
      connection.rollback();
      onRollback(System.nanoTime() - startTimeNs, null);

    } catch (Throwable t) {
      onRollback(System.nanoTime() - startTimeNs, t);
      throw t;
    }
  }

  // Not supported.
  @Override
  public void rollback(Savepoint savepoint) throws SQLException {
    connection.rollback(savepoint);
  }

  @Override
  public void commit() throws SQLException {
    long startTimeNs = System.nanoTime();
    try {
      connection.commit();
      onCommit(System.nanoTime() - startTimeNs, null);
    } catch (Throwable t) {
      onCommit(System.nanoTime() - startTimeNs, t);
      throw t;
    }
  }

  @Override
  public CallableStatement prepareCall(String sql) throws SQLException {
    return new SpyqlCallableStatement(sql, connection.prepareCall(sql), this);
  }

  @Override
  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    return new SpyqlCallableStatement(sql, connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability), this);
  }

  @Override
  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
    return new SpyqlCallableStatement(sql, connection.prepareCall(sql, resultSetType, resultSetConcurrency), this);
  }

  @Override
  public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
    return new SpyqlStatement(connection.createStatement(resultSetType, resultSetConcurrency), this);
  }

  @Override
  public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    return new SpyqlStatement(connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability), this);
  }

  @Override
  public Statement createStatement() throws SQLException {
    return new SpyqlStatement(connection.createStatement(), this);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
    return new SpyqlPreparedStatement(sql, connection.prepareStatement(sql, columnIndexes), this);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
    return new SpyqlPreparedStatement(sql, connection.prepareStatement(sql, columnNames), this);
  }

  @Override
  public PreparedStatement prepareStatement(String sql) throws SQLException {
    return new SpyqlPreparedStatement(sql, connection.prepareStatement(sql), this);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
    return new SpyqlPreparedStatement(sql, connection.prepareStatement(sql, resultSetType, resultSetConcurrency), this);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
    return new SpyqlPreparedStatement(sql, connection.prepareStatement(sql, autoGeneratedKeys), this);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    return new SpyqlPreparedStatement(sql, connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), this);
  }

  //// Helper methods ////

  protected boolean isInTransaction() {
    return transactionId != null;
  }

  protected void onCommit(long timeTakenNs, Throwable t) {
    if (!isInTransaction()) {
      onTransactionBegin(true);
    }
    if (t == null) {
      spyqlDataSource.onConnectionEvent(connectionListeners, new TransactionCommitEvent()
          .setExecutionTimeNs(timeTakenNs)
          .setConnectionId(connectionId)
          .setTransactionId(transactionId));

      transactionId = null;
    } else {
      spyqlDataSource.onConnectionEvent(connectionListeners, new TransactionCommitFailureEvent()
          .setExecutionTimeNs(timeTakenNs)
          .setConnectionId(connectionId)
          .setTransactionId(transactionId)
          .setThrowable(t));
    }
  }

  protected void onRollback(long timeTakenNs, Throwable t) {
    if (!isInTransaction()) {
      onTransactionBegin(true);
    }

    if (t == null) {
      spyqlDataSource.onConnectionEvent(connectionListeners, new TransactionRollbackEvent()
          .setConnectionId(connectionId)
          .setTransactionId(transactionId)
          .setExecutionTimeNs(timeTakenNs));

      transactionId = null;
    } else {
      spyqlDataSource.onConnectionEvent(connectionListeners, new TransactionRollbackFailureEvent()
          .setConnectionId(connectionId)
          .setTransactionId(transactionId)
          .setExecutionTimeNs(timeTakenNs)
          .setThrowable(t));
    }
  }

  protected void onClose(long timeTakenNs, Throwable t) {
    if (t == null) {
      spyqlDataSource.onConnectionEvent(connectionListeners, new ConnectionCloseEvent()
          .setConnectionId(connectionId)
          .setTransactionId(transactionId)
          .setExecutionTimeNs(timeTakenNs));
    } else {
      spyqlDataSource.onConnectionEvent(connectionListeners, new ConnectionCloseFailureEvent()
          .setExecutionTimeNs(timeTakenNs)
          .setConnectionId(connectionId)
          .setTransactionId(transactionId)
          .setThrowable(t));
    }
  }

  protected void onTransactionBegin(boolean emptyTransaction) {
    transactionId = spyqlDataSource.nextTransactionId();

    spyqlDataSource.onConnectionEvent(connectionListeners, new TransactionBeginEvent()
        .setConnectionId(connectionId)
        .setTransactionId(transactionId)
        .setEmptyTransaction(emptyTransaction)
        .setTransactionDefinition(spyqlDataSource.getTransactionDefinition()));
  }

  protected void onStatementExecute(long timeTakenNs, String sql, long affectedRowsCount, Throwable t) throws SQLException {
    if (t == null) {
      if (!isInTransaction() && !connection.getAutoCommit()) {
        onTransactionBegin(false);
      }
      spyqlDataSource.onConnectionEvent(connectionListeners, new StatementExecuteEvent()
          .setConnectionId(connectionId)
          .setTransactionId(transactionId)
          .setExecutionTimeNs(timeTakenNs)
          .setAffectedRowsCount(affectedRowsCount)
          .setSql(sql));
    } else {
      spyqlDataSource.onConnectionEvent(connectionListeners, new StatementExecuteFailureEvent()
          .setConnectionId(connectionId)
          .setTransactionId(transactionId)
          .setExecutionTimeNs(timeTakenNs)
          .setSql(sql)
          .setThrowable(t));
    }
  }

  protected void onNextRecords(long recordsCount) {
    spyqlDataSource.onConnectionEvent(connectionListeners, new ResultSetNextRowsEvent()
        .setConnectionId(connectionId)
        .setTransactionId(transactionId)
        .setRowsCount(recordsCount));
  }

  //// Default behaviour ////
  @Override
  public String getCatalog() throws SQLException {
    return connection.getCatalog();
  }

  @Override
  public boolean getAutoCommit() throws SQLException {
    return connection.getAutoCommit();
  }

  @Override
  public SQLXML createSQLXML() throws SQLException {
    return connection.createSQLXML();
  }

  @Override
  public void clearWarnings() throws SQLException {
    connection.clearWarnings();
  }

  @Override
  public void setTransactionIsolation(int level) throws SQLException {
    connection.setTransactionIsolation(level);
  }

  @Override
  public void abort(Executor executor) throws SQLException {
    connection.abort(executor);
  }

  @Override
  public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
    connection.setNetworkTimeout(executor, milliseconds);
  }

  @Override
  public Savepoint setSavepoint() throws SQLException {
    return connection.setSavepoint();
  }

  @Override
  public Savepoint setSavepoint(String name) throws SQLException {
    return connection.setSavepoint(name);
  }

  // Not supported.
  @Override
  public void releaseSavepoint(Savepoint savepoint) throws SQLException {
    connection.releaseSavepoint(savepoint);
  }

  @Override
  public void setClientInfo(Properties properties) throws SQLClientInfoException {
    connection.setClientInfo(properties);
  }

  @Override
  public void setClientInfo(String name, String value) throws SQLClientInfoException {
    connection.setClientInfo(name, value);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return connection.isWrapperFor(iface);
  }

  @Override
  public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
    return connection.createStruct(typeName, attributes);
  }

  @Override
  public void setReadOnly(boolean readOnly) throws SQLException {
    connection.setReadOnly(readOnly);
  }

  @Override
  public int getTransactionIsolation() throws SQLException {
    return connection.getTransactionIsolation();
  }

  @Override
  public void setHoldability(int holdability) throws SQLException {
    connection.setHoldability(holdability);
  }

  @Override
  public NClob createNClob() throws SQLException {
    return connection.createNClob();
  }

  @Override
  public int getNetworkTimeout() throws SQLException {
    return connection.getNetworkTimeout();
  }

  @Override
  public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
    return connection.createArrayOf(typeName, elements);
  }

  @Override
  public void setCatalog(String catalog) throws SQLException {
    connection.setCatalog(catalog);
  }

  @Override
  public Map<String, Class<?>> getTypeMap() throws SQLException {
    return connection.getTypeMap();
  }

  @Override
  public String nativeSQL(String sql) throws SQLException {
    return connection.nativeSQL(sql);
  }

  @Override
  public int getHoldability() throws SQLException {
    return connection.getHoldability();
  }

  @Override
  public Properties getClientInfo() throws SQLException {
    return connection.getClientInfo();
  }

  @Override
  public String getClientInfo(String name) throws SQLException {
    return connection.getClientInfo(name);
  }

  @Override
  public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
    connection.setTypeMap(map);
  }

  @Override
  public Blob createBlob() throws SQLException {
    return connection.createBlob();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    return connection.unwrap(iface);
  }

  @Override
  public boolean isClosed() throws SQLException {
    return connection.isClosed();
  }

  @Override
  public void setSchema(String schema) throws SQLException {
    connection.setSchema(schema);
  }

  @Override
  public SQLWarning getWarnings() throws SQLException {
    return connection.getWarnings();
  }

  @Override
  public boolean isValid(int timeout) throws SQLException {
    return connection.isValid(timeout);
  }

  @Override
  public String getSchema() throws SQLException {
    return connection.getSchema();
  }

  @Override
  public Clob createClob() throws SQLException {
    return connection.createClob();
  }

  @Override
  public DatabaseMetaData getMetaData() throws SQLException {
    return connection.getMetaData();
  }

  @Override
  public boolean isReadOnly() throws SQLException {
    return connection.isReadOnly();
  }
}
