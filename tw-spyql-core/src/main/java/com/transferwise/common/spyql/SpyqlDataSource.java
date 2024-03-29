package com.transferwise.common.spyql;

import com.transferwise.common.baseutils.jdbc.DataSourceProxyUtils;
import com.transferwise.common.baseutils.jdbc.ParentAwareDataSourceProxy;
import com.transferwise.common.spyql.event.ConnectionEvent;
import com.transferwise.common.spyql.event.GetConnectionEvent;
import com.transferwise.common.spyql.event.GetConnectionFailureEvent;
import com.transferwise.common.spyql.listener.SpyqlConnectionListener;
import com.transferwise.common.spyql.listener.SpyqlDataSourceListener;
import com.transferwise.common.spyql.spring.SpringTransactionDefinition;
import com.transferwise.common.spyql.utils.CallableWithSqlException;
import com.transferwise.common.spyql.utils.ConnectionListenersHelper;
import com.transferwise.common.spyql.utils.SimpleThrottler;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import javax.sql.DataSource;
import lombok.Getter;
import lombok.Setter;

public class SpyqlDataSource implements ParentAwareDataSourceProxy {

  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SpyqlDataSource.class);

  private static final int ERRORS_PER_MINUTE = 100;

  @Getter
  @Setter
  private String databaseName;
  private DataSource targetDataSource;
  private DataSource parentDataSource;
  private SimpleThrottler errorLogThrottler;
  private ConnectionListenersHelper connectionListenersHelper;
  @Setter
  @Getter
  private TransactionDefinitionProvider transactionDefinitionProvider = new SpringTransactionDefinition();
  private AtomicLong connectionIdSequence = new AtomicLong();
  private AtomicLong transactionIdSequence = new AtomicLong();

  @Getter
  private List<SpyqlDataSourceListener> dataSourceListeners = new ArrayList<>();

  public SpyqlDataSource(DataSource targetDataSource, String databaseName, SpyqlDataSourceListener dataSourceListener) {
    setTargetDataSource(targetDataSource);
    this.databaseName = databaseName;
    this.errorLogThrottler = new SimpleThrottler(Duration.ofMinutes(1), ERRORS_PER_MINUTE);
    connectionListenersHelper = new ConnectionListenersHelper(errorLogThrottler);
    addListener(dataSourceListener);

    DataSourceProxyUtils.tieTogether(this, targetDataSource);
  }

  public SpyqlDataSource(DataSource targetDataSource) {
    this(targetDataSource, null, null);
  }

  public SpyqlDataSource(DataSource targetDataSource, SpyqlDataSourceListener dataSourceListener) {
    this(targetDataSource, null, dataSourceListener);
  }

  public SpyqlDataSource(DataSource targetDataSource, String databaseName) {
    this(targetDataSource, databaseName, null);
  }

  public void addListener(SpyqlDataSourceListener dataSourceListener) {
    if (dataSourceListener != null) {
      dataSourceListeners.add(dataSourceListener);
    }
  }

  @Override
  public Connection getConnection() throws SQLException {
    return onGetConnection(() -> targetDataSource.getConnection());
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return onGetConnection(() -> targetDataSource.getConnection(username, password));
  }

  //// Helper methods ////

  protected Connection onGetConnection(CallableWithSqlException<Connection> callable) throws SQLException {
    if (dataSourceListeners.isEmpty()) {
      return callable.call();
    }
    long startTimeNs = System.nanoTime();
    long connectionId = connectionIdSequence.incrementAndGet();
    try {
      Connection con = callable.call();
      long timeTakenNs = System.nanoTime() - startTimeNs;
      if (con != null) {
        List<SpyqlConnectionListener> connectionListeners = new ArrayList<>();
        dataSourceListeners.forEach((dataSourceListener) -> {
          SpyqlConnectionListener connectionListener = callQuietly(() ->
              dataSourceListener.onGetConnection(new GetConnectionEvent()
                  .setExecutionTimeNs(timeTakenNs)
                  .setConnectionId(connectionId)));
          if (connectionListener != null) {
            connectionListeners.add(connectionListener);
          }
        });
        if (connectionListeners.isEmpty()) {
          return con;
        }
        return new SpyqlConnection(this, con, connectionListeners, connectionId);
      } else {
        dataSourceListeners.forEach((dataSourceListener) -> callQuietly(() -> {
          dataSourceListener.onGetConnectionFailure(new GetConnectionFailureEvent()
              .setExecutionTimeNs(timeTakenNs)
              .setNullReturned(true)
              .setConnectionId(connectionId));
          return null;
        }));

        return null;
      }
    } catch (Throwable t) {
      long timeTakenNs = System.nanoTime() - startTimeNs;
      dataSourceListeners.forEach((dataSourceListener) -> callQuietly(() -> {
        dataSourceListener.onGetConnectionFailure(new GetConnectionFailureEvent()
            .setExecutionTimeNs(timeTakenNs)
            .setConnectionId(connectionId)
            .setThrowable(t));
        return null;
      }));
      throw t;
    }
  }

  protected <T> T callQuietly(Callable<T> callable) {
    try {
      return callable.call();
    } catch (Throwable t) {
      if (!errorLogThrottler.doThrottleAnEvent()) {
        log.error(t.getMessage(), t);
      }
      return null;
    }
  }

  protected void onConnectionEvent(List<SpyqlConnectionListener> connectionListeners, ConnectionEvent event) {
    connectionListenersHelper.onEvent(connectionListeners, event);
  }

  protected SpyqlTransactionDefinition getTransactionDefinition() {
    return transactionDefinitionProvider.get();
  }

  protected long nextTransactionId() {
    return transactionIdSequence.incrementAndGet();
  }

  //// Default behaviour ////

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return targetDataSource.getLogWriter();
  }

  @Override
  public void setLogWriter(PrintWriter out) throws SQLException {
    targetDataSource.setLogWriter(out);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return targetDataSource.getLoginTimeout();
  }

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    targetDataSource.setLoginTimeout(seconds);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T unwrap(Class<T> iface) throws SQLException {
    if (iface.isInstance(this)) {
      return (T) this;
    }
    return targetDataSource.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return (iface.isInstance(this) || targetDataSource.isWrapperFor(iface));
  }

  @Override
  public Logger getParentLogger() {
    return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  }

  @Override
  public DataSource getParentDataSource() {
    return parentDataSource;
  }

  @Override
  public void setParentDataSource(DataSource parentDataSource) {
    this.parentDataSource = parentDataSource;
  }

  @Override
  public void setTargetDataSource(DataSource targetDataSource) {
    this.targetDataSource = targetDataSource;
  }

  @Override
  public DataSource getTargetDataSource() {
    return targetDataSource;
  }
}
