package com.transferwise.spyql;

public interface SpyqlConnectionListener {
	SpyqlTransactionListener onTransactionBegin(SpyqlTransactionDefinition transactionDefinition);
	void onStatementExecute(String sql, Long executionTimeNs);
	void onClose();
}
