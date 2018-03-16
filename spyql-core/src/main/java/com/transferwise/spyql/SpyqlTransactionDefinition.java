package com.transferwise.spyql;

public class SpyqlTransactionDefinition {
	private String name;
	private Boolean readOnly;
	private Integer isolationLevel;

	public SpyqlTransactionDefinition(String name, Boolean readOnly, Integer isolationLevel) {
		this.name = name;
		this.readOnly = readOnly;
		this.isolationLevel = isolationLevel;
	}

	public String getName() {
		return name;
	}

	public Boolean getReadOnly() {
		return readOnly;
	}

	public Integer getIsolationLevel() {
		return isolationLevel;
	}
}
