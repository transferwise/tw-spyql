package com.transferwise.common.spyql.listerners;

import com.transferwise.common.spyql.SpyqlTransactionDefinition;
import com.transferwise.common.spyql.event.ConnectionCloseEvent;
import com.transferwise.common.spyql.event.GetConnectionEvent;
import com.transferwise.common.spyql.event.StatementExecuteEvent;
import com.transferwise.common.spyql.event.StatementExecuteFailureEvent;
import com.transferwise.common.spyql.event.TransactionBeginEvent;
import com.transferwise.common.spyql.event.TransactionCommitEvent;
import com.transferwise.common.spyql.event.TransactionRollbackEvent;
import com.transferwise.common.spyql.listener.SpyqlConnectionListener;
import com.transferwise.common.spyql.listener.SpyqlDataSourceListener;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpyqlLoggingListener implements SpyqlDataSourceListener {

  private static final Logger log = LoggerFactory.getLogger(SpyqlLoggingListener.class);

  private AtomicLong connectionIdSequence = new AtomicLong(0L);
  private AtomicLong transactionIdSequnce = new AtomicLong(0L);

  @Override
  public SpyqlConnectionListener onGetConnection(GetConnectionEvent event) {
    long conId = connectionIdSequence.incrementAndGet();
    log.info("GET CONNECTION id: {}, result: {}", conId, event);
    return new ConnectionListener(conId);
  }

  private class ConnectionListener implements SpyqlConnectionListener {

    private long connectionId;
    private SpyqlTransactionDefinition transactionDefinition;
    private long transactionId;

    public ConnectionListener(long connectionId) {
      this.connectionId = connectionId;
    }

    @Override
    public void onTransactionBegin(TransactionBeginEvent event) {
      if (!log.isInfoEnabled()) {
        return;
      }
      transactionDefinition = event.getTransactionDefinition();
      transactionId = transactionIdSequnce.incrementAndGet();
      log.info("TRANSACTION BEGIN connectionId: {}, transactionId: {}, name: {}, readOnly {}, isolationLevel: {}",
          connectionId, transactionId, transactionDefinition.getName(), transactionDefinition.getReadOnly(),
          transactionDefinition.getIsolationLevel());
    }

    @Override
    public void onStatementExecute(StatementExecuteEvent event) {
      if (!log.isInfoEnabled()) {
        return;
      }
      if (event.isInTransaction()) {
        log.info("EXECUTE IN TRANSACTION connectionId: {}, transactionId: {}, sql: {}, after: {} ns", connectionId, transactionId, event.getSql(),
            event.getExecutionTimeNs());
      } else {
        log.info("EXECUTE WITHOUT TRANSACTION connectionId: {}, transactionId: {}, sql: {}, after: {} ns", event.getSql(),
            event.getExecutionTimeNs());
      }
    }

    @Override
    public void onStatementExecuteFailure(StatementExecuteFailureEvent result) {
      log.info("Exception was thrown: {} after: {} ns", result.getThrowable(), result.getExecutionTimeNs());
    }

    @Override
    public void onTransactionCommit(TransactionCommitEvent event) {
      log.info("TRANSACTION COMMIT connectionId: {}, transactionId: {}, name: {}, after: {} ns", connectionId, transactionId,
          transactionDefinition == null ? null : transactionDefinition.getName(), event.getExecutionTimeNs());
    }

    @Override
    public void onTransactionRollback(TransactionRollbackEvent event) {
      log.info("TRANSACTION ROLLBACK connectionId: {}, transactionId: {}, name: {}, after: {} ns", connectionId, transactionId,
          transactionDefinition == null ? null : transactionDefinition.getName(), event.getExecutionTimeNs());
    }


    @Override
    public void onConnectionClose(ConnectionCloseEvent event) {
      log.info("CLOSE CONNECTION connectionId: {}", connectionId);
    }
  }
}
