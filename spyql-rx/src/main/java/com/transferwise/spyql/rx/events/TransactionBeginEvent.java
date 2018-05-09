package com.transferwise.spyql.rx.events;

import com.transferwise.spyql.SpyqlTransactionDefinition;

public class TransactionBeginEvent implements TransactionEvent {
	private long connectionId;
	private long transactionId;
	private String name;
	private Boolean readOnly;
	private Integer isolationLevel;

	public TransactionBeginEvent(long connectionId, long transactionId, String name, Boolean readOnly, Integer isolationLevel) {
		this.connectionId = connectionId;
		this.transactionId = transactionId;
		this.name = name;
		this.readOnly = readOnly;
		this.isolationLevel = isolationLevel;
	}

	public TransactionBeginEvent(long connectionId, long transactionId, SpyqlTransactionDefinition transaction) {
		this(connectionId, transactionId, transaction.getName(), transaction.getReadOnly(), transaction.getIsolationLevel());
	}

	@Override
	public long getConnectionId() {
		return connectionId;
	}

	@Override
	public long getTransactionId() {
		return transactionId;
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
				"connectionId=" + getConnectionId() +
				", transactionId=" + getTransactionId() +
				", name='" + name + '\'' +
				", readOnly=" + readOnly +
				", isolationLevel=" + isolationLevel +
				'}';
	}
}
