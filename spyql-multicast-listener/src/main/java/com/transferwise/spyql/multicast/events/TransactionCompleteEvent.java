package com.transferwise.spyql.multicast.events;

public abstract class TransactionCompleteEvent implements Event {
	private long id;
	private Long executionTimeNs;

	public TransactionCompleteEvent(long id, Long executionTimeNs) {
		this.id = id;
		this.executionTimeNs = executionTimeNs;
	}

	public Long getExecutionTimeNs() {
		return executionTimeNs;
	}

	public long getId() {
		return id;
	}
}
