package com.transferwise.common.spyql.utils;

import com.transferwise.common.spyql.event.ConnectionCloseEvent;
import com.transferwise.common.spyql.event.ConnectionCloseFailureEvent;
import com.transferwise.common.spyql.event.ConnectionEvent;
import com.transferwise.common.spyql.event.StatementExecuteEvent;
import com.transferwise.common.spyql.event.StatementExecuteFailureEvent;
import com.transferwise.common.spyql.event.TransactionBeginEvent;
import com.transferwise.common.spyql.event.TransactionCommitEvent;
import com.transferwise.common.spyql.event.TransactionCommitFailureEvent;
import com.transferwise.common.spyql.event.TransactionRollbackEvent;
import com.transferwise.common.spyql.event.TransactionRollbackFailureEvent;
import com.transferwise.common.spyql.listener.SpyqlConnectionListener;
import java.util.List;
import org.slf4j.Logger;

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
