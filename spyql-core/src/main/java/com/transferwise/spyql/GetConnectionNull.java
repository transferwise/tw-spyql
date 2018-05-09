package com.transferwise.spyql;

public class GetConnectionNull extends GetConnectionResult {
	public GetConnectionNull(long executionTime) {
		super(executionTime);
	}

	@Override
	public String toString() {
		return "GetConnectionNull{" +
				"executionTimeNs=" + getExecutionTimeNs() +
				'}';
	}
}
