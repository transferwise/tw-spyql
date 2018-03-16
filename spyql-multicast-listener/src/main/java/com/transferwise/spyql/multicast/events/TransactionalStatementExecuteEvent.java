package com.transferwise.spyql.multicast.events;

public class TransactionalStatementExecuteEvent extends StatementExecuteEvent {
	private long transactionId;

	public TransactionalStatementExecuteEvent(String sql, Long executionTimeNs, long transactionId) {
		super(sql, executionTimeNs);
		this.transactionId = transactionId;
	}

	public long getTransactionId() {
		return transactionId;
	}

	@Override
	public String toString() {
		return "TransactionalStatementExecuteEvent{" +
				"transactionId=" + transactionId +
				", sql='" + getSql() + '\'' +
				", executionTimeNs=" + getExecutionTimeNs() +
				'}';
	}
}
