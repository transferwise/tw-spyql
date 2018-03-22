package com.transferwise.spyql.rx;

import com.transferwise.spyql.SpyqlListener;
import com.transferwise.spyql.SpyqlTransactionDefinition;
import com.transferwise.spyql.SpyqlTransactionListener;
import com.transferwise.spyql.rx.events.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import java.util.concurrent.atomic.AtomicLong;

public class ObservableListener extends Observable<Event> implements SpyqlListener {
	private static final int TRANSACTION_LISTENER_MAP_MAX_SIZE_DEFAULT = 5000;

	private Subject<Event> subject;
	private AtomicLong transactionId = new AtomicLong(1L);
	private int transactionListenerMapMaxSize;

	public ObservableListener() {
		this(TRANSACTION_LISTENER_MAP_MAX_SIZE_DEFAULT);
	}

	public ObservableListener(int transactionListenerMapMaxSize) {
		this.transactionListenerMapMaxSize = transactionListenerMapMaxSize;
		subject = PublishSubject.create();
	}

	public ObservableListener(int transactionListenerMapMaxSize, Subject<Event> subject) {
		this.subject = subject;
		this.transactionListenerMapMaxSize = transactionListenerMapMaxSize;
	}

	@Override
	public SpyqlTransactionListener onTransactionBegin(SpyqlTransactionDefinition transactionDefinition) {
		long transactionId = this.transactionId.getAndIncrement();
		subject.onNext(new TransactionBeginEvent(transactionId, transactionDefinition));
		return new TransactionListener(transactionId);
	}

	@Override
	public void onStatementExecute(String sql, Long executionTimeNs) {
		StatementExecuteEvent event = new StatementExecuteEvent(sql, executionTimeNs);
		subject.onNext(event);
	}

	@Override
	protected void subscribeActual(Observer<? super Event> observer) {
		subject.subscribe(observer);
	}

	public void attachListener(SpyqlListener listener) {
		subscribe(new ObserverToListenerAdapter(listener, transactionListenerMapMaxSize));
	}

	public void attachAsyncListener(SpyqlListener listener) {
		this.observeOn(Schedulers.newThread())
				.subscribe(new ObserverToListenerAdapter(listener, transactionListenerMapMaxSize));
	}

	public void close() {
		subject.onComplete();
	}

	class TransactionListener implements SpyqlTransactionListener {
		private long id;

		TransactionListener(long id) {
			this.id = id;
		}

		@Override
		public void onTransactionCommit(Long transactionExecutionTimeNs) {
			subject.onNext(new TransactionCommitEvent(id, transactionExecutionTimeNs));
		}

		@Override
		public void onTransactionRollback(Long transactionExecutionTimeNs) {
			subject.onNext(new TransactionRollbackEvent(id, transactionExecutionTimeNs));
		}

		@Override
		public void onStatementExecute(String sql, Long executionTimeNs) {
			subject.onNext(new TransactionalStatementExecuteEvent(sql, executionTimeNs, id));
		}

		@Override
		public void onStatementFailure(String sql, Long executionTimeNs, Throwable e) {

		}
	}
}
