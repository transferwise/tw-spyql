package com.transferwise.spyql;

public interface SpyqlTransactionListener {

	default void onTransactionCommit(Long transactionExecutionTimeNs) {}

	default void onTransactionRollback(Long transactionExecutionTimeNs) {}

	void onStatementExecute(String sql, Long executionTimeNs);

	void onStatementFailure(String sql, Long executionTimeNs, Throwable e);

}
