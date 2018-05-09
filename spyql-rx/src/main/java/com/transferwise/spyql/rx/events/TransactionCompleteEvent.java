package com.transferwise.spyql.rx.events;

public abstract class TransactionCompleteEvent implements TransactionEvent {
	private long connectionId;
	private long transactionId;
	private long executionTimeNs;

	public TransactionCompleteEvent(long connectionId, long transactionId, long executionTimeNs) {
		this.connectionId = connectionId;
		this.transactionId = transactionId;
		this.executionTimeNs = executionTimeNs;
	}

	@Override
	public long getConnectionId() {
		return connectionId;
	}

	@Override
	public long getTransactionId() {
		return transactionId;
	}

	public long getExecutionTimeNs() {
		return executionTimeNs;
	}
}
