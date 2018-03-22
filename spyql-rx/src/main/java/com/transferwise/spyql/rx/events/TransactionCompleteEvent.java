package com.transferwise.spyql.rx.events;

public abstract class TransactionCompleteEvent implements TransactionEvent {
	private long transactionId;
	private Long executionTimeNs;

	public TransactionCompleteEvent(long id, Long executionTimeNs) {
		this.transactionId = id;
		this.executionTimeNs = executionTimeNs;
	}

	public Long getExecutionTimeNs() {
		return executionTimeNs;
	}

	@Override
	public long getTransactionId() {
		return transactionId;
	}
}
