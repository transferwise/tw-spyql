package com.transferwise.spyql.utils;

import com.transferwise.spyql.event.*;
import com.transferwise.spyql.listener.SpyqlConnectionListener;
import org.slf4j.Logger;

import java.util.List;

public class ConnectionListenersHelper {
    private static Logger log = org.slf4j.LoggerFactory.getLogger(ConnectionListenersHelper.class);

    private SimpleThrottler errorLogThrottler;

    public ConnectionListenersHelper(SimpleThrottler errorLogThrottler) {
        this.errorLogThrottler = errorLogThrottler;
    }

    public void onEvent(List<SpyqlConnectionListener> listeners, ConnectionEvent event) {
        for (SpyqlConnectionListener listener : listeners) {
            onEvent(listener, event);
        }
    }

    public void onEvent(SpyqlConnectionListener listener, ConnectionEvent event) {
        if (listener == null) {
            return;
        }
        runQuietly(() -> {
            if (event instanceof TransactionBeginEvent) {
                listener.onTransactionBegin((TransactionBeginEvent) event);
            } else if (event instanceof TransactionCommitEvent) {
                listener.onTransactionCommit((TransactionCommitEvent) event);
            } else if (event instanceof TransactionCommitFailureEvent) {
                listener.onTransactionCommitFailure((TransactionCommitFailureEvent) event);
            } else if (event instanceof TransactionRollbackEvent) {
                listener.onTransactionRollback((TransactionRollbackEvent) event);
            } else if (event instanceof TransactionRollbackFailureEvent) {
                listener.onTransactionRollbackFailure((TransactionRollbackFailureEvent) event);
            } else if (event instanceof StatementExecuteEvent) {
                listener.onStatementExecute((StatementExecuteEvent) event);
            } else if (event instanceof StatementExecuteFailureEvent) {
                listener.onStatementExecuteFailure((StatementExecuteFailureEvent) event);
            } else if (event instanceof ConnectionCloseEvent) {
                listener.onConnectionClose((ConnectionCloseEvent) event);
            } else if (event instanceof ConnectionCloseFailureEvent) {
                listener.onConnectionCloseFailure((ConnectionCloseFailureEvent) event);
            }
            listener.onEvent(event);
        });
    }

    protected void runQuietly(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            if (!errorLogThrottler.doThrottleAnEvent()) {
                log.error(t.getMessage(), t);
            }
        }
    }
}
