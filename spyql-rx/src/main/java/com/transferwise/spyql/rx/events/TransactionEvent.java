package com.transferwise.spyql.rx.events;

public interface TransactionEvent extends ConnectionEvent {
	long getTransactionId();
}
