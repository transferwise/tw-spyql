package com.transferwise.spyql.multicast.events;

public interface TransactionEvent extends Event {
	long getTransactionId();
}
