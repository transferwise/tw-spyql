package com.transferwise.spyql.multicast;

import com.transferwise.spyql.SpyqlTransactionDefinition;
import com.transferwise.spyql.SpyqlTransactionListener;
import com.transferwise.spyql.multicast.events.Event;

public class TestRxJava {

	public static void main(String[] args) throws InterruptedException {
		AsyncMulticastListener listener = new AsyncMulticastListener();
		listener.subscribeAsync(s -> printMessage(3, s));
		listener.subscribeAsync(s -> printMessage(4, s));

		SpyqlTransactionListener transactionListener = listener.onTransactionBegin(new SpyqlTransactionDefinition("tx1", false, 0));
		listener.onStatementExecute("SELECT 2", 123L);
		transactionListener.onStatementExecute("SELECT 3", 123L);
		transactionListener.onTransactionRollback(321L);

		Thread.sleep(100);
	}

	public static void printMessage(Integer subscriberId, Event s) {
		System.out.print("From subscriber " + subscriberId + " on thread " + Thread.currentThread() + ": " + s + "\n");
	}
}
