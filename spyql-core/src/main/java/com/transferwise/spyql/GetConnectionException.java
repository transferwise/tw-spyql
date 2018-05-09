package com.transferwise.spyql;

public class GetConnectionException extends GetConnectionResult {
	private String exceptionName;
	private String exceptionMessage;

	public GetConnectionException(String exceptionName, String exceptionMessage, long executionTime) {
		super(executionTime);
		this.exceptionName = exceptionName;
		this.exceptionMessage = exceptionMessage;
	}

	public String getExceptionName() {
		return exceptionName;
	}

	public String getExceptionMessage() {
		return exceptionMessage;
	}

	@Override
	public String toString() {
		return "GetConnectionException{" +
				"executionTimeNs=" + getExecutionTimeNs() +
				", exceptionName='" + exceptionName + '\'' +
				", exceptionMessage='" + exceptionMessage + '\'' +
				'}';
	}
}
