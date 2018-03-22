package com.transferwise.spyql.rx.events;

import com.transferwise.spyql.SpyqlTransactionDefinition;

public class TransactionBeginEvent implements TransactionEvent {
	private long transactionId;
	private String name;
	private Boolean readOnly;
	private Integer isolationLevel;

	public TransactionBeginEvent(long transactionId, String name, Boolean readOnly, Integer isolationLevel) {
		this.transactionId = transactionId;
		this.name = name;
		this.readOnly = readOnly;
		this.isolationLevel = isolationLevel;
	}

	public TransactionBeginEvent(long transactionId, SpyqlTransactionDefinition transaction) {
		this(transactionId, transaction.getName(), transaction.getReadOnly(), transaction.getIsolationLevel());
	}

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
				"transactionId=" + transactionId +
				", name='" + name + '\'' +
				", readOnly=" + readOnly +
				", isolationLevel=" + isolationLevel +
				'}';
	}
}
