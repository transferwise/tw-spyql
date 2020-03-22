package com.transferwise.common.spyql.listener;

import com.transferwise.common.spyql.event.ConnectionCloseEvent;
import com.transferwise.common.spyql.event.ConnectionCloseFailureEvent;
import com.transferwise.common.spyql.event.ResultSetNextRowsEvent;
import com.transferwise.common.spyql.event.SpyqlEvent;
import com.transferwise.common.spyql.event.StatementExecuteEvent;
import com.transferwise.common.spyql.event.StatementExecuteFailureEvent;
import com.transferwise.common.spyql.event.TransactionBeginEvent;
import com.transferwise.common.spyql.event.TransactionCommitEvent;
import com.transferwise.common.spyql.event.TransactionCommitFailureEvent;
import com.transferwise.common.spyql.event.TransactionRollbackEvent;
import com.transferwise.common.spyql.event.TransactionRollbackFailureEvent;

public interface SpyqlConnectionListener {

  default void onEvent(SpyqlEvent event) {
  }

  default void onTransactionBegin(TransactionBeginEvent event) {
  }

  default void onTransactionCommit(TransactionCommitEvent event) {
  }

  default void onTransactionCommitFailure(TransactionCommitFailureEvent event) {
  }

  default void onTransactionRollback(TransactionRollbackEvent event) {
  }

  default void onTransactionRollbackFailure(TransactionRollbackFailureEvent event) {
  }

  default void onStatementExecute(StatementExecuteEvent event) {
  }

  default void onStatementExecuteFailure(StatementExecuteFailureEvent event) {
  }

  default void onConnectionClose(ConnectionCloseEvent event) {
  }

  default void onConnectionCloseFailure(ConnectionCloseFailureEvent event) {
  }

  default void onResultSetNextRecords(ResultSetNextRowsEvent event) {
  }
}

