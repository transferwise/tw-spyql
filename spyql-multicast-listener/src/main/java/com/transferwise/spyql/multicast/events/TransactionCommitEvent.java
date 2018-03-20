package com.transferwise.spyql.multicast.events;

public class TransactionCommitEvent extends TransactionCompleteEvent {
	public TransactionCommitEvent(long id, Long executionTimeNs) {
		super(id, executionTimeNs);
	}

	@Override
	public String toString() {
		return "TransactionCommitEvent{" +
				"transactionId=" + getTransactionId() +
				", executionTimeNs=" + getExecutionTimeNs() +
				'}';
	}

}
