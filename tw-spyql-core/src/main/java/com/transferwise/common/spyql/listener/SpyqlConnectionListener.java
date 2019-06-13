package com.transferwise.common.spyql.listener;

import com.transferwise.common.spyql.event.*;

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

}

