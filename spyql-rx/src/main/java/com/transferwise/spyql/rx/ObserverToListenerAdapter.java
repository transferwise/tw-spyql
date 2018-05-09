package com.transferwise.spyql.rx;

import com.transferwise.spyql.SpyqlConnectionListener;
import com.transferwise.spyql.SpyqlDataSourceListener;
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

	private int maxConcurrentConnections;
	private int maxConcurrentTransactions;

	private Map<Long, SpyqlConnectionListener> connectionListenerMap = new ConcurrentHashMap<>();
	private Map<Long, SpyqlTransactionListener> transactionListenerMap = new ConcurrentHashMap<>();
	private Disposable connectionDisposable;
	private AtomicInteger errorCount = new AtomicInteger(0);

	private SpyqlDataSourceListener listener;

	ObserverToListenerAdapter(SpyqlDataSourceListener listener, int maxConcurrentConnections, int maxConcurrentTransactions) {
		if (listener == null) {
			throw new IllegalArgumentException("listener cannot be null");
		}
		this.listener = listener;
		this.maxConcurrentConnections = maxConcurrentConnections;
		this.maxConcurrentTransactions = maxConcurrentTransactions;
	}

	@Override
	public void onSubscribe(Disposable d) {
		connectionDisposable = d;
	}

	@Override
	public void onNext(Event event) {
		// RANT: This looks so ugly in Java
		if (event instanceof ConnectionAcquireEvent) {
			ConnectionAcquireEvent e = (ConnectionAcquireEvent) event;
			SpyqlConnectionListener connectionListener = listener.onGetConnection(e.getResult());
			if (connectionListenerMap.size() < maxConcurrentConnections) {
				if (connectionListener == null) {
					connectionListener = new NullConnectionListener();
				}
				connectionListenerMap.put(e.getConnectionId(), connectionListener);
			} else {
				log.error("Trying to create more than {} connections", maxConcurrentConnections);
				registerErrorAndDetachIfNeeded();
			}
		} else if (event instanceof ConnectionCloseEvent) {
			ConnectionCloseEvent e = (ConnectionCloseEvent) event;
			SpyqlConnectionListener connectionListener = connectionListenerMap.get(e.getConnectionId());
			if (connectionListener != null) {
				connectionListenerMap.remove(e.getConnectionId());
				connectionListener.onClose();
			} else {
				logConnectionErrorAndDetachIfNeeded(e);
			}
		} else if (event instanceof TransactionalStatementExecuteEvent) {
			TransactionalStatementExecuteEvent e = (TransactionalStatementExecuteEvent) event;
			SpyqlTransactionListener transactionListener = transactionListenerMap.get(e.getTransactionId());
			if (transactionListener != null) {
				if (!(transactionListener instanceof NullTransactionListener)) {
					transactionListener.onStatementExecute(e.getSql(), e.getExecutionTimeNs());
				}
			} else {
				logTransactionErrorAndDetachIfNeeded(e);
			}
		} else if (event instanceof StatementExecuteEvent) {
			StatementExecuteEvent e = (StatementExecuteEvent) event;
			SpyqlConnectionListener connectionListener = connectionListenerMap.get(e.getConnectionId());
			if (connectionListener != null) {
				if (!(connectionListener instanceof NullConnectionListener)) {
					connectionListener.onStatementExecute(e.getSql(), e.getExecutionTimeNs());
				}
			} else {
				logConnectionErrorAndDetachIfNeeded(e);
			}
		} else if (event instanceof TransactionBeginEvent) {
			TransactionBeginEvent e = (TransactionBeginEvent) event;
			SpyqlConnectionListener connectionListener = connectionListenerMap.get(e.getConnectionId());
			SpyqlTransactionListener transactionListener = null;
			if (connectionListener != null) {
				if (!(connectionListener instanceof NullConnectionListener)) {
					transactionListener = connectionListener.onTransactionBegin(e.getTransactionDefinition());
				}
			} else {
				logConnectionErrorAndDetachIfNeeded(e);
			}
			if (transactionListenerMap.size() < maxConcurrentTransactions) {
				if (transactionListener == null) {
					transactionListener = new NullTransactionListener();
				}
				transactionListenerMap.put(e.getTransactionId(), transactionListener);
			} else {
				log.error("Trying to open more than {} transactions", maxConcurrentTransactions);
				registerErrorAndDetachIfNeeded();
			}
		} else if (event instanceof TransactionCommitEvent) {
			TransactionCommitEvent e = (TransactionCommitEvent) event;
			SpyqlTransactionListener transactionListener = transactionListenerMap.get(e.getTransactionId());
			if (transactionListener != null) {
				transactionListenerMap.remove(e.getTransactionId());
				transactionListener.onTransactionCommit(e.getExecutionTimeNs());
			} else {
				logTransactionErrorAndDetachIfNeeded(e);
			}
		} else if (event instanceof TransactionRollbackEvent) {
			TransactionRollbackEvent e = (TransactionRollbackEvent) event;
			SpyqlTransactionListener transactionListener = transactionListenerMap.get(e.getTransactionId());
			if (transactionListener != null) {
				transactionListenerMap.remove(e.getTransactionId());
				transactionListener.onTransactionRollback(e.getExecutionTimeNs());
			} else {
				logTransactionErrorAndDetachIfNeeded(e);
			}
		}
	}

	private void logConnectionErrorAndDetachIfNeeded(ConnectionEvent e) {
		log.error("{} was received but no connection with id {} found", e, e.getConnectionId());
		registerErrorAndDetachIfNeeded();
	}

	@Override
	public void onError(Throwable e) {
		resetState();
	}

	@Override
	public void onComplete() {
		resetState();
	}

	public int getConnectionListenerMapSize() {
		return connectionListenerMap.size();
	}

	public int getTransactionListenerMapSize() {
		return transactionListenerMap.size();
	}

	private void logTransactionErrorAndDetachIfNeeded(TransactionEvent event) {
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
