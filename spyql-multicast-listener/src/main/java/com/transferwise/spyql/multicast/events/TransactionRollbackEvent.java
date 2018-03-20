package com.transferwise.spyql.multicast.events;

public class TransactionRollbackEvent extends TransactionCompleteEvent {
	public TransactionRollbackEvent(long id, Long executionTimeNs) {
		super(id, executionTimeNs);
	}

	@Override
	public String toString() {
		return "TransactionRollbackEvent{" +
				"transactionId=" + getTransactionId() +
				", executionTimeNs=" + getExecutionTimeNs() +
				'}';
	}
}
