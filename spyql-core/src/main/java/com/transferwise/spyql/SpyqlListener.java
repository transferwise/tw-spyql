package com.transferwise.spyql;

public interface SpyqlListener {
	SpyqlTransactionListener onTransactionBegin(SpyqlTransactionDefinition transactionSpy);
	void onStatementExecute(String sql, Long executionTimeNs);
}
