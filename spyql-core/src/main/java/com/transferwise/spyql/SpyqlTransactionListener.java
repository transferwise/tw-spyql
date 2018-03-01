package com.transferwise.spyql;

public interface SpyqlTransactionListener {
	void onTransactionCommit();
	void onTransactionRollback();
	void onTransactionComplete();
	void onStatementExecute(String sql, Long executionTimeNs);
}
