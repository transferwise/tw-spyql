package com.transferwise.spyql.rx.events;

public class TransactionalStatementExecuteEvent extends StatementExecuteEvent implements TransactionEvent {
	private long transactionId;

	public TransactionalStatementExecuteEvent(long connectionId, long transactionId, String sql, Long executionTimeNs) {
		super(connectionId, sql, executionTimeNs);
		this.transactionId = transactionId;
	}

	@Override
	public long getTransactionId() {
		return transactionId;
	}

	@Override
	public String toString() {
		return "TransactionalStatementExecuteEvent{" +
				"connectionId=" + getConnectionId() +
				", transactionId=" + transactionId +
				", sql='" + getSql() + '\'' +
				", executionTimeNs=" + getExecutionTimeNs() +
				'}';
	}
}
