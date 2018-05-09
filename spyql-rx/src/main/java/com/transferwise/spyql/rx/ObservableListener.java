package com.transferwise.spyql.rx;

import com.transferwise.spyql.SpyqlConnectionListener;
import com.transferwise.spyql.SpyqlDataSourceListener;
import com.transferwise.spyql.SpyqlTransactionDefinition;
import com.transferwise.spyql.SpyqlTransactionListener;
import com.transferwise.spyql.rx.events.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import java.util.concurrent.atomic.AtomicLong;

public class ObservableListener extends Observable<Event> implements SpyqlDataSourceListener {
	private static final int MAX_CONCURRENT_TRANSACTIONS_DEFAULT = 5000;
	private static final int MAX_CONCURRENT_CONNECTIONS_DEFAULT = 5000;

	private Subject<Event> subject;
	private AtomicLong atomicConnectionId = new AtomicLong(1L);
	private AtomicLong atomicTransactionId = new AtomicLong(1L);
	private int maxConcurrentConnections;
	private int maxConcurrentTransactions;

	public ObservableListener() {
		this(MAX_CONCURRENT_CONNECTIONS_DEFAULT, MAX_CONCURRENT_TRANSACTIONS_DEFAULT);
	}

	public ObservableListener(int maxConcurrentConnections, int maxConcurrentTransactions) {
		this(maxConcurrentConnections, maxConcurrentTransactions, PublishSubject.create());
	}

	public ObservableListener(int maxConcurrentConnections, int maxConcurrentTransactions, Subject<Event> subject) {
		this.subject = subject;
		this.maxConcurrentConnections = maxConcurrentConnections;
		this.maxConcurrentTransactions = maxConcurrentTransactions;
	}

	@Override
	public SpyqlConnectionListener onGetConnection(Long acquireTimeNs) {
		long connectionId = atomicConnectionId.getAndIncrement();
		subject.onNext(new ConnectionAcquireEvent(connectionId, acquireTimeNs));
		return new ConnectionListener(connectionId);
	}

	@Override
	protected void subscribeActual(Observer<? super Event> observer) {
		subject.subscribe(observer);
	}

	public void attachListener(SpyqlDataSourceListener listener) {
		subscribe(new ObserverToListenerAdapter(listener, maxConcurrentConnections, maxConcurrentTransactions));
	}

	public void attachAsyncListener(SpyqlDataSourceListener listener) {
		this.observeOn(Schedulers.newThread())
				.subscribe(new ObserverToListenerAdapter(listener, maxConcurrentConnections, maxConcurrentTransactions));
	}

	public void close() {
		subject.onComplete();
	}

	class ConnectionListener implements SpyqlConnectionListener {
		private long connectionId;

		public ConnectionListener(long connectionId) {
			this.connectionId = connectionId;
		}

		@Override
		public SpyqlTransactionListener onTransactionBegin(SpyqlTransactionDefinition transactionDefinition) {
			long transactionId = atomicTransactionId.getAndIncrement();
			subject.onNext(new TransactionBeginEvent(connectionId, transactionId, transactionDefinition));
			return new TransactionListener(connectionId, transactionId);
		}

		@Override
		public void onStatementExecute(String sql, Long executionTimeNs) {
			StatementExecuteEvent event = new StatementExecuteEvent(connectionId, sql, executionTimeNs);
			subject.onNext(event);
		}

		@Override
		public void onClose() {
			subject.onNext(new ConnectionCloseEvent(connectionId));
		}
	}

	class TransactionListener implements SpyqlTransactionListener {
		private long connectionId;
		private long transactionId;

		TransactionListener(long connectionId, long transactionId) {
			this.connectionId = connectionId;
			this.transactionId = transactionId;
		}

		@Override
		public void onTransactionCommit(Long transactionExecutionTimeNs) {
			subject.onNext(new TransactionCommitEvent(connectionId, transactionId, transactionExecutionTimeNs));
		}

		@Override
		public void onTransactionRollback(Long transactionExecutionTimeNs) {
			subject.onNext(new TransactionRollbackEvent(connectionId, transactionId, transactionExecutionTimeNs));
		}

		@Override
		public void onStatementExecute(String sql, Long executionTimeNs) {
			subject.onNext(new TransactionalStatementExecuteEvent(connectionId, transactionId, sql, executionTimeNs));
		}

		@Override
		public void onStatementFailure(String sql, Long executionTimeNs, Throwable e) {

		}
	}
}
