package com.transferwise.spyql;

public abstract class GetConnectionResult {
	private long executionTimeNs;

	GetConnectionResult(long executionTimeNs) {
		this.executionTimeNs = executionTimeNs;
	}

	public long getExecutionTimeNs() {
		return executionTimeNs;
	}
}
