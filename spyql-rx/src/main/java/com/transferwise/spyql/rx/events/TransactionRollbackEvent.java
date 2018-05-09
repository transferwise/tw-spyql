package com.transferwise.spyql.rx.events;

public class TransactionRollbackEvent extends TransactionCompleteEvent {
	public TransactionRollbackEvent(long connectionId, long transactionId, long executionTimeNs) {
		super(connectionId, transactionId, executionTimeNs);
	}

	@Override
	public String toString() {
		return "TransactionRollbackEvent{" +
				"connectionId=" + getConnectionId() +
				", transactionId=" + getTransactionId() +
				", executionTimeNs=" + getExecutionTimeNs() +
				'}';
	}
}
