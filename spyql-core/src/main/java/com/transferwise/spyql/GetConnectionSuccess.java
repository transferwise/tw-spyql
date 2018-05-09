package com.transferwise.spyql;

public class GetConnectionSuccess extends GetConnectionResult {
	public GetConnectionSuccess(long executionTime) {
		super(executionTime);
	}

	@Override
	public String toString() {
		return "GetConnectionSuccess{" +
				"executionTimeNs=" + getExecutionTimeNs() +
				'}';
	}
}
