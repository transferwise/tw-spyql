package com.transferwise.spyql.rx;

import com.transferwise.spyql.SpyqlTransactionListener;

class NullTransactionListener implements SpyqlTransactionListener {
	@Override
	public void onTransactionCommit(Long transactionExecutionTimeNs) {

	}

	@Override
	public void onTransactionRollback(Long transactionExecutionTimeNs) {

	}

	@Override
	public void onStatementExecute(String sql, Long executionTimeNs) {

	}

	@Override
	public void onStatementFailure(String sql, Long executionTimeNs, Throwable e) {

	}
}
