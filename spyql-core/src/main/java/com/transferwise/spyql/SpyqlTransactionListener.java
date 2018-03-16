package com.transferwise.spyql;

public interface SpyqlTransactionListener {
	@Deprecated
	default void onTransactionCommit() {}

	default void onTransactionCommit(Long transactionExecutionTimeNs) {}

	@Deprecated
	default void onTransactionRollback() {}

	default void onTransactionRollback(Long transactionExecutionTimeNs) {}

	@Deprecated
	default void onTransactionComplete(Long transactionExecutionTimeNs) {}

	void onStatementExecute(String sql, Long executionTimeNs);

	void onStatementFailure(String sql, Long executionTimeNs, Throwable e);

}
