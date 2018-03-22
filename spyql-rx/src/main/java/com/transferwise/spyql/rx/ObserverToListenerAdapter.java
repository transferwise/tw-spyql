package com.transferwise.spyql.rx;

import com.transferwise.spyql.SpyqlListener;
import com.transferwise.spyql.SpyqlTransactionListener;
import com.transferwise.spyql.rx.events.*;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

class ObserverToListenerAdapter implements Observer<Event> {
	public static final int MAX_ERROR_COUNT = 1000;

	private static final Logger log = LoggerFactory.getLogger(ObserverToListenerAdapter.class);

	private int transactionListenerMapMaxSize;
	private Map<Long, SpyqlTransactionListener> transactionListenerMap = new ConcurrentHashMap<>();
	private Disposable connectionDisposable;
	private AtomicInteger errorCount = new AtomicInteger(0);

	ObserverToListenerAdapter(SpyqlListener listener, int transactionListenerMapMaxSize) {
		if (listener == null) {
			throw new IllegalArgumentException("listener cannot be null");
		}
		this.listener = listener;
		this.transactionListenerMapMaxSize = transactionListenerMapMaxSize;
	}

	private SpyqlListener listener;

	@Override
	public void onSubscribe(Disposable d) {
		connectionDisposable = d;
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
			} else {
				logErrorAndDetachIfNeeded(e);
			}
		} else if (event instanceof StatementExecuteEvent) {
			StatementExecuteEvent e = (StatementExecuteEvent) event;
			listener.onStatementExecute(e.getSql(), e.getExecutionTimeNs());
		} else if (event instanceof TransactionBeginEvent) {
			TransactionBeginEvent e = (TransactionBeginEvent) event;
			SpyqlTransactionListener transactionListener = listener.onTransactionBegin(e.getTransactionDefinition());
			if (transactionListenerMap.size() < transactionListenerMapMaxSize) {
				transactionListenerMap.put(e.getTransactionId(), transactionListener);
			} else {
				log.error("Trying to open more than {} transactions", transactionListenerMapMaxSize);
				registerErrorAndDetachIfNeeded();
			}
		} else if (event instanceof TransactionCommitEvent) {
			TransactionCommitEvent e = (TransactionCommitEvent) event;
			SpyqlTransactionListener transactionListener = transactionListenerMap.get(e.getTransactionId());
			if (transactionListener != null) {
				transactionListenerMap.remove(e.getTransactionId());
				transactionListener.onTransactionCommit(e.getExecutionTimeNs());
				transactionListener.onTransactionCommit();
				transactionListener.onTransactionComplete(e.getExecutionTimeNs());
			} else {
				logErrorAndDetachIfNeeded(e);
			}
		} else if (event instanceof TransactionRollbackEvent) {
			TransactionRollbackEvent e = (TransactionRollbackEvent) event;
			SpyqlTransactionListener transactionListener = transactionListenerMap.get(e.getTransactionId());
			if (transactionListener != null) {
				transactionListenerMap.remove(e.getTransactionId());
				transactionListener.onTransactionRollback(e.getExecutionTimeNs());
				transactionListener.onTransactionRollback();
				transactionListener.onTransactionComplete(e.getExecutionTimeNs());
			} else {
				logErrorAndDetachIfNeeded(e);
			}
		}
	}

	@Override
	public void onError(Throwable e) {
		resetState();
	}

	@Override
	public void onComplete() {
		resetState();
	}

	public int getTransactionListenerMapSize() {
		return transactionListenerMap.size();
	}

	private void logErrorAndDetachIfNeeded(TransactionEvent event) {
		log.error("{} was received but no transaction with id {} found", event, event.getTransactionId());
		registerErrorAndDetachIfNeeded();
	}

	private void registerErrorAndDetachIfNeeded() {
		if (errorCount.incrementAndGet() > MAX_ERROR_COUNT) {
			log.error("Number of errors surpassed {}", MAX_ERROR_COUNT);
			detach();
			resetState();
		}
	}

	private void detach() {
		log.info("Detaching the observer");
		connectionDisposable.dispose();
	}

	private void resetState() {
		log.info("Resetting the state");
		transactionListenerMap.clear();
		errorCount.set(0);
	}
}
