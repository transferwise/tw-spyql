package com.transferwise.spyql.multicast.events;

import com.transferwise.spyql.SpyqlTransactionDefinition;

public class TransactionBeginEvent implements Event {
	private long id;
	private String name;
	private Boolean readOnly;
	private Integer isolationLevel;

	public TransactionBeginEvent(long id, String name, Boolean readOnly, Integer isolationLevel) {
		this.id = id;
		this.name = name;
		this.readOnly = readOnly;
		this.isolationLevel = isolationLevel;
	}

	public TransactionBeginEvent(long id, SpyqlTransactionDefinition transaction) {
		this.id = id;
		this.name = transaction.getName();
		this.readOnly = transaction.getReadOnly();
		this.isolationLevel = transaction.getIsolationLevel();
	}

	public long getId() {
		return id;
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

	public SpyqlTransactionDefinition getTransactionDefinition() {
		return new SpyqlTransactionDefinition(name, readOnly, isolationLevel);
	}
	@Override
	public String toString() {
		return "TransactionBeginEvent{" +
				"id=" + id +
				", name='" + name + '\'' +
				", readOnly=" + readOnly +
				", isolationLevel=" + isolationLevel +
				'}';
	}
}
