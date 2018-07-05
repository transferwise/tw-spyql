package com.transferwise.spyql;

import com.transferwise.spyql.event.ConnectionEvent;
import com.transferwise.spyql.event.GetConnectionEvent;
import com.transferwise.spyql.event.GetConnectionFailureEvent;
import com.transferwise.spyql.listener.SpyqlConnectionListener;
import com.transferwise.spyql.listener.SpyqlDataSourceListener;
import com.transferwise.spyql.spring.SpringTransactionDefinition;
import com.transferwise.spyql.utils.CallableWithSQLException;
import com.transferwise.spyql.utils.ConnectionListenersHelper;
import com.transferwise.spyql.utils.SimpleThrottler;
import lombok.Getter;
import lombok.Setter;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class SpyqlDataSource implements DataSource {
    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SpyqlDataSource.class);
    @Getter
    @Setter
    private String databaseName;
    private DataSource dataSource;
    private SimpleThrottler errorLogThrottler;
    private ConnectionListenersHelper connectionListenersHelper;
    @Setter
    @Getter
    private TransactionDefinitionProvider transactionDefinitionProvider = new SpringTransactionDefinition();
    private AtomicLong connectionIdSequence = new AtomicLong();
    private AtomicLong transactionIdSequence = new AtomicLong();

    @Getter
    private List<SpyqlDataSourceListener> dataSourceListeners = new ArrayList<>();

    public SpyqlDataSource(DataSource dataSource, String databaseName, SpyqlDataSourceListener dataSourceListener){
        this.dataSource = dataSource;
        this.databaseName = databaseName;
        this.errorLogThrottler = new SimpleThrottler(Duration.ofMinutes(1), 100);
        connectionListenersHelper = new ConnectionListenersHelper(errorLogThrottler);
        addListener(dataSourceListener);
    }
    public SpyqlDataSource(DataSource dataSource) {
        this(dataSource, null, null);
    }

    public SpyqlDataSource(DataSource dataSource, SpyqlDataSourceListener dataSourceListener) {
        this(dataSource, null, dataSourceListener);
    }

    public SpyqlDataSource(DataSource dataSource, String databaseName) {
        this(dataSource, databaseName, null);
    }

    public void addListener(SpyqlDataSourceListener dataSourceListener) {
        if(dataSourceListener != null){
            dataSourceListeners.add(dataSourceListener);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return onGetConnection(() -> dataSource.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return onGetConnection(() -> dataSource.getConnection(username, password));
    }

    //// Helper methods ////

    protected Connection onGetConnection(CallableWithSQLException<Connection> callable) throws SQLException {
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
                dataSourceListeners.forEach((dataSourceListener) -> {
                    callQuietly(() -> {
                        dataSourceListener.onGetConnectionFailure(new GetConnectionFailureEvent()
                            .setExecutionTimeNs(timeTakenNs)
                            .setNullReturned(true)
                            .setConnectionId(connectionId));
                        return null;
                    });
                });

                return null;
            }
        } catch (Throwable t) {
            long timeTakenNs = System.nanoTime() - startTimeNs;
            dataSourceListeners.forEach((dataSourceListener) -> {
                callQuietly(() -> {
                    dataSourceListener.onGetConnectionFailure(new GetConnectionFailureEvent()
                        .setExecutionTimeNs(timeTakenNs)
                        .setConnectionId(connectionId)
                        .setThrowable(t));
                    return null;
                });
            });
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
        return dataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        dataSource.setLogWriter(out);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return dataSource.getLoginTimeout();
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        dataSource.setLoginTimeout(seconds);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return (T) this;
        }
        return dataSource.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return (iface.isInstance(this) || dataSource.isWrapperFor(iface));
    }

    @Override
    public Logger getParentLogger() {
        return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }

}
