package com.transferwise.common.spyql;

import com.transferwise.common.baseutils.jdbc.ConnectionProxyUtils;
import com.transferwise.common.baseutils.jdbc.ParentAwareConnectionProxy;
import com.transferwise.common.spyql.event.ConnectionCloseEvent;
import com.transferwise.common.spyql.event.ConnectionCloseFailureEvent;
import com.transferwise.common.spyql.event.ResultSetNextRowsEvent;
import com.transferwise.common.spyql.event.SpyqlTransaction;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SpyqlConnection implements ParentAwareConnectionProxy {

  private List<SpyqlConnectionListener> connectionListeners;
  private Connection targetConnection;
  private Connection parentConnection;

  private SpyqlDataSource spyqlDataSource;
  private long connectionId;
  private SpyqlTransaction transaction;

  public SpyqlConnection(SpyqlDataSource spyqlDataSource, Connection targetConnection, List<SpyqlConnectionListener> connectionListeners,
      long connectionId) {
    setTargetConnection(targetConnection);
    this.connectionListeners = connectionListeners;
    this.spyqlDataSource = spyqlDataSource;
    this.connectionId = connectionId;

    ConnectionProxyUtils.tieTogether(this, targetConnection);
  }

  @Override
  public void close() throws SQLException {
    long startTimeNs = System.nanoTime();
    try {
      targetConnection.close();
      onClose(System.nanoTime() - startTimeNs, null);
    } catch (Throwable t) {
      onClose(System.nanoTime() - startTimeNs, t);
      throw t;
    }
  }

  @Override
  public void setAutoCommit(boolean autoCommit) throws SQLException {
    boolean implicitCommit = autoCommit && !targetConnection.getAutoCommit() && isInTransaction();
    if (!implicitCommit) {
      targetConnection.setAutoCommit(autoCommit);
    } else {
      long startTimeNs = System.nanoTime();
      try {
        targetConnection.setAutoCommit(autoCommit);
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
      targetConnection.rollback();
      onRollback(System.nanoTime() - startTimeNs, null);

    } catch (Throwable t) {
      onRollback(System.nanoTime() - startTimeNs, t);
      throw t;
    }
  }

  // Not supported.
  @Override
  public void rollback(Savepoint savepoint) throws SQLException {
    targetConnection.rollback(savepoint);
  }

  @Override
  public void commit() throws SQLException {
    long startTimeNs = System.nanoTime();
    try {
      targetConnection.commit();
      onCommit(System.nanoTime() - startTimeNs, null);
    } catch (Throwable t) {
      onCommit(System.nanoTime() - startTimeNs, t);
      throw t;
    }
  }

  @Override
  public CallableStatement prepareCall(String sql) throws SQLException {
    return new SpyqlCallableStatement(sql, targetConnection.prepareCall(sql), this);
  }

  @Override
  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    return new SpyqlCallableStatement(sql, targetConnection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability), this);
  }

  @Override
  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
    return new SpyqlCallableStatement(sql, targetConnection.prepareCall(sql, resultSetType, resultSetConcurrency), this);
  }

  @Override
  public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
    return new SpyqlStatement(targetConnection.createStatement(resultSetType, resultSetConcurrency), this);
  }

  @Override
  public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    return new SpyqlStatement(targetConnection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability), this);
  }

  @Override
  public Statement createStatement() throws SQLException {
    return new SpyqlStatement(targetConnection.createStatement(), this);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
    return new SpyqlPreparedStatement(sql, targetConnection.prepareStatement(sql, columnIndexes), this);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
    return new SpyqlPreparedStatement(sql, targetConnection.prepareStatement(sql, columnNames), this);
  }

  @Override
  public PreparedStatement prepareStatement(String sql) throws SQLException {
    return new SpyqlPreparedStatement(sql, targetConnection.prepareStatement(sql), this);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
    return new SpyqlPreparedStatement(sql, targetConnection.prepareStatement(sql, resultSetType, resultSetConcurrency), this);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
    return new SpyqlPreparedStatement(sql, targetConnection.prepareStatement(sql, autoGeneratedKeys), this);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    return new SpyqlPreparedStatement(sql, targetConnection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), this);
  }

  //// Helper methods ////

  protected boolean isInTransaction() {
    return transaction != null;
  }

  protected void onCommit(long timeTakenNs, Throwable t) {
    if (!isInTransaction()) {
      onTransactionBegin(true);
    }
    transaction.setEndTime(Instant.now());
    if (t == null) {
      spyqlDataSource.onConnectionEvent(connectionListeners, new TransactionCommitEvent()
          .setExecutionTimeNs(timeTakenNs)
          .setConnectionId(connectionId)
          .setTransaction(transaction)
      );
      transaction = null;
    } else {
      spyqlDataSource.onConnectionEvent(connectionListeners, new TransactionCommitFailureEvent()
          .setExecutionTimeNs(timeTakenNs)
          .setConnectionId(connectionId)
          .setTransaction(transaction)
          .setThrowable(t));
    }
  }

  protected void onRollback(long timeTakenNs, Throwable t) {
    if (!isInTransaction()) {
      onTransactionBegin(true);
    }
    transaction.setEndTime(Instant.now());
    if (t == null) {
      spyqlDataSource.onConnectionEvent(connectionListeners, new TransactionRollbackEvent()
          .setConnectionId(connectionId)
          .setTransaction(transaction)
          .setExecutionTimeNs(timeTakenNs));
      transaction = null;
    } else {
      spyqlDataSource.onConnectionEvent(connectionListeners, new TransactionRollbackFailureEvent()
          .setConnectionId(connectionId)
          .setTransaction(transaction)
          .setExecutionTimeNs(timeTakenNs)
          .setThrowable(t));
    }
  }

  protected void onClose(long timeTakenNs, Throwable t) {
    if (isInTransaction()) {
      transaction.setEndTime(Instant.now());
    }
    if (t == null) {
      spyqlDataSource.onConnectionEvent(connectionListeners, new ConnectionCloseEvent()
          .setConnectionId(connectionId)
          .setTransaction(transaction)
          .setExecutionTimeNs(timeTakenNs));
    } else {
      spyqlDataSource.onConnectionEvent(connectionListeners, new ConnectionCloseFailureEvent()
          .setExecutionTimeNs(timeTakenNs)
          .setConnectionId(connectionId)
          .setTransaction(transaction)
          .setThrowable(t));
    }
  }

  protected void onTransactionBegin(boolean emptyTransaction) {
    SpyqlTransaction transaction = new SpyqlTransaction()
        .setId(spyqlDataSource.nextTransactionId())
        .setStartTime(Instant.now())
        .setDefinition(spyqlDataSource.getTransactionDefinition())
        .setEmpty(emptyTransaction);

    this.transaction = transaction;

    spyqlDataSource.onConnectionEvent(connectionListeners, new TransactionBeginEvent()
        .setConnectionId(connectionId)
        .setTransaction(transaction));
  }

  protected void onStatementExecute(long timeTakenNs, String sql, long affectedRowsCount, Throwable t) throws SQLException {
    if (t == null) {
      if (!isInTransaction() && !targetConnection.getAutoCommit()) {
        onTransactionBegin(false);
      }
      spyqlDataSource.onConnectionEvent(connectionListeners, new StatementExecuteEvent()
          .setConnectionId(connectionId)
          .setTransaction(transaction)
          .setExecutionTimeNs(timeTakenNs)
          .setAffectedRowsCount(affectedRowsCount)
          .setSql(sql));
    } else {
      spyqlDataSource.onConnectionEvent(connectionListeners, new StatementExecuteFailureEvent()
          .setConnectionId(connectionId)
          .setTransaction(transaction)
          .setExecutionTimeNs(timeTakenNs)
          .setSql(sql)
          .setThrowable(t));
    }
  }

  protected void onNextRecords(long recordsCount) {
    spyqlDataSource.onConnectionEvent(connectionListeners, new ResultSetNextRowsEvent()
        .setConnectionId(connectionId)
        .setTransaction(transaction)
        .setRowsCount(recordsCount));
  }

  //// Default behaviour ////
  @Override
  public String getCatalog() throws SQLException {
    return targetConnection.getCatalog();
  }

  @Override
  public boolean getAutoCommit() throws SQLException {
    return targetConnection.getAutoCommit();
  }

  @Override
  public SQLXML createSQLXML() throws SQLException {
    return targetConnection.createSQLXML();
  }

  @Override
  public void clearWarnings() throws SQLException {
    targetConnection.clearWarnings();
  }

  @Override
  public void setTransactionIsolation(int level) throws SQLException {
    targetConnection.setTransactionIsolation(level);
  }

  @Override
  public void abort(Executor executor) throws SQLException {
    targetConnection.abort(executor);
  }

  @Override
  public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
    targetConnection.setNetworkTimeout(executor, milliseconds);
  }

  @Override
  public Savepoint setSavepoint() throws SQLException {
    return targetConnection.setSavepoint();
  }

  @Override
  public Savepoint setSavepoint(String name) throws SQLException {
    return targetConnection.setSavepoint(name);
  }

  // Not supported.
  @Override
  public void releaseSavepoint(Savepoint savepoint) throws SQLException {
    targetConnection.releaseSavepoint(savepoint);
  }

  @Override
  public void setClientInfo(Properties properties) throws SQLClientInfoException {
    targetConnection.setClientInfo(properties);
  }

  @Override
  public void setClientInfo(String name, String value) throws SQLClientInfoException {
    targetConnection.setClientInfo(name, value);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return targetConnection.isWrapperFor(iface);
  }

  @Override
  public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
    return targetConnection.createStruct(typeName, attributes);
  }

  @Override
  public void setReadOnly(boolean readOnly) throws SQLException {
    targetConnection.setReadOnly(readOnly);
  }

  @Override
  public int getTransactionIsolation() throws SQLException {
    return targetConnection.getTransactionIsolation();
  }

  @Override
  public void setHoldability(int holdability) throws SQLException {
    targetConnection.setHoldability(holdability);
  }

  @Override
  public NClob createNClob() throws SQLException {
    return targetConnection.createNClob();
  }

  @Override
  public int getNetworkTimeout() throws SQLException {
    return targetConnection.getNetworkTimeout();
  }

  @Override
  public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
    return targetConnection.createArrayOf(typeName, elements);
  }

  @Override
  public void setCatalog(String catalog) throws SQLException {
    targetConnection.setCatalog(catalog);
  }

  @Override
  public Map<String, Class<?>> getTypeMap() throws SQLException {
    return targetConnection.getTypeMap();
  }

  @Override
  public String nativeSQL(String sql) throws SQLException {
    return targetConnection.nativeSQL(sql);
  }

  @Override
  public int getHoldability() throws SQLException {
    return targetConnection.getHoldability();
  }

  @Override
  public Properties getClientInfo() throws SQLException {
    return targetConnection.getClientInfo();
  }

  @Override
  public String getClientInfo(String name) throws SQLException {
    return targetConnection.getClientInfo(name);
  }

  @Override
  public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
    targetConnection.setTypeMap(map);
  }

  @Override
  public Blob createBlob() throws SQLException {
    return targetConnection.createBlob();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    return targetConnection.unwrap(iface);
  }

  @Override
  public boolean isClosed() throws SQLException {
    return targetConnection.isClosed();
  }

  @Override
  public void setSchema(String schema) throws SQLException {
    targetConnection.setSchema(schema);
  }

  @Override
  public SQLWarning getWarnings() throws SQLException {
    return targetConnection.getWarnings();
  }

  @Override
  public boolean isValid(int timeout) throws SQLException {
    return targetConnection.isValid(timeout);
  }

  @Override
  public String getSchema() throws SQLException {
    return targetConnection.getSchema();
  }

  @Override
  public Clob createClob() throws SQLException {
    return targetConnection.createClob();
  }

  @Override
  public DatabaseMetaData getMetaData() throws SQLException {
    return targetConnection.getMetaData();
  }

  @Override
  public boolean isReadOnly() throws SQLException {
    return targetConnection.isReadOnly();
  }

  @Override
  public Connection getTargetConnection() {
    return targetConnection;
  }

  @Override
  public void setTargetConnection(Connection targetConnection) {
    this.targetConnection = targetConnection;
  }

  @Override
  public Connection getParentConnection() {
    return parentConnection;
  }

  @Override
  public void setParentConnection(Connection parentConnection) {
    this.parentConnection = parentConnection;
  }
}
