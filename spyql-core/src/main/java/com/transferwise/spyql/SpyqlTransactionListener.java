package com.transferwise.spyql;

public interface SpyqlTransactionListener {
	void onTransactionCommit();
	void onTransactionRollback();
	void onTransactionComplete(Long transactionExecutionTimeNs);
	void onStatementExecute(String sql, Long executionTimeNs);
	void onStatementFailure(String sql, Long executionTimeNs, Throwable e);
}
