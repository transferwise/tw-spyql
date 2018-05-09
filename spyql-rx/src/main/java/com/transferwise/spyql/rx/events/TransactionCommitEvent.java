package com.transferwise.spyql.rx.events;

public class TransactionCommitEvent extends TransactionCompleteEvent {
	public TransactionCommitEvent(long connectionId, long transactionId, long executionTimeNs) {
		super(connectionId, transactionId, executionTimeNs);
	}

	@Override
	public String toString() {
		return "TransactionCommitEvent{" +
				"connectionId=" + getConnectionId() +
				", transactionId=" + getTransactionId() +
				", executionTimeNs=" + getExecutionTimeNs() +
				'}';
	}

}
