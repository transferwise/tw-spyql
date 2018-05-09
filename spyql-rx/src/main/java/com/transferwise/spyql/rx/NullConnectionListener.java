package com.transferwise.spyql.rx;

import com.transferwise.spyql.SpyqlConnectionListener;
import com.transferwise.spyql.SpyqlTransactionDefinition;
import com.transferwise.spyql.SpyqlTransactionListener;

public class NullConnectionListener implements SpyqlConnectionListener {
	@Override
	public SpyqlTransactionListener onTransactionBegin(SpyqlTransactionDefinition transactionDefinition) {
		return null;
	}

	@Override
	public void onStatementExecute(String sql, Long executionTimeNs) {

	}

	@Override
	public void onClose() {

	}
}
