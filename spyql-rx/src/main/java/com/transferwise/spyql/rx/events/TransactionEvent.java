package com.transferwise.spyql.rx.events;

public interface TransactionEvent extends Event {
	long getTransactionId();
}
