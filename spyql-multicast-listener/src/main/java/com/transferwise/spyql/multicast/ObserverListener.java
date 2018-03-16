package com.transferwise.spyql.multicast;

import com.transferwise.spyql.SpyqlListener;
import com.transferwise.spyql.SpyqlTransactionListener;
import com.transferwise.spyql.multicast.events.*;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
class ObserverListener implements Observer<Event> {
	private static final int TRANSACTION_LISTENER_MAP_MAX_SIZE = 5000;

	private Map<Long, SpyqlTransactionListener> transactionListenerMap = new HashMap<>();

	ObserverListener(SpyqlListener listener) {
		this.listener = listener;
	}

	private SpyqlListener listener;

	@Override
	public void onSubscribe(Disposable d) {

	}

	@Override
	@SuppressWarnings("deprecation")
	public void onNext(Event event) {
		// RANT: This looks so ugly in Java
		if (event instanceof TransactionalStatementExecuteEvent) {
			TransactionalStatementExecuteEvent e = (TransactionalStatementExecuteEvent) event;
			SpyqlTransactionListener transactionListener = transactionListenerMap.get(e.getTransactionId());
			if (transactionListener != null) {
				transactionListener.onStatementExecute(e.getSql(), e.getExecutionTimeNs());
			}
		} else if (event instanceof StatementExecuteEvent) {
			StatementExecuteEvent e = (StatementExecuteEvent) event;
			listener.onStatementExecute(e.getSql(), e.getExecutionTimeNs());
		} else if (event instanceof TransactionBeginEvent) {
			TransactionBeginEvent e = (TransactionBeginEvent) event;
			SpyqlTransactionListener transactionListener = listener.onTransactionBegin(e.getTransactionDefinition());
			if (transactionListenerMap.size() < TRANSACTION_LISTENER_MAP_MAX_SIZE) {
				transactionListenerMap.put(e.getId(), transactionListener);
			} else {
				log.error("Unable to add new transaction listener");
			}
		} else if (event instanceof TransactionCommitEvent) {
			TransactionCommitEvent e = (TransactionCommitEvent) event;
			SpyqlTransactionListener transactionListener = transactionListenerMap.get(e.getId());
			if (transactionListener != null) {
				transactionListenerMap.remove(e.getId());
				transactionListener.onTransactionCommit(e.getExecutionTimeNs());
				transactionListener.onTransactionCommit();
				transactionListener.onTransactionComplete(e.getExecutionTimeNs());
			}
		} else if (event instanceof TransactionRollbackEvent) {
			TransactionRollbackEvent e = (TransactionRollbackEvent) event;
			SpyqlTransactionListener transactionListener = transactionListenerMap.get(e.getId());
			if (transactionListener != null) {
				transactionListenerMap.remove(e.getId());
				transactionListener.onTransactionRollback(e.getExecutionTimeNs());
				transactionListener.onTransactionRollback();
				transactionListener.onTransactionComplete(e.getExecutionTimeNs());
			}
		}
	}

	@Override
	public void onError(Throwable e) {

	}

	@Override
	public void onComplete() {

	}
}
