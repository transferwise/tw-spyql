package com.transferwise.spyql;

public interface SpyqlListener {
	SpyqlTransactionListener onTransactionBegin(SpyqlTransactionDefinition transactionDefinition);
	void onStatementExecute(String sql, Long executionTimeNs);
}
