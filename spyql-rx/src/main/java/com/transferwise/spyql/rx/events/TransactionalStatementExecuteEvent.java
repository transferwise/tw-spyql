package com.transferwise.spyql.rx.events;

public class TransactionalStatementExecuteEvent extends StatementExecuteEvent implements TransactionEvent {
	private long transactionId;

	public TransactionalStatementExecuteEvent(String sql, Long executionTimeNs, long transactionId) {
		super(sql, executionTimeNs);
		this.transactionId = transactionId;
	}

	@Override
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
