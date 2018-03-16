package com.transferwise.spyql.multicast;

import com.transferwise.spyql.SpyqlListener;
import com.transferwise.spyql.SpyqlTransactionDefinition;
import com.transferwise.spyql.SpyqlTransactionListener;
import com.transferwise.spyql.multicast.events.*;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import java.util.concurrent.atomic.AtomicLong;

public class AsyncMulticastListener implements SpyqlListener {
	private PublishSubject<Event> subject = PublishSubject.create();
	private AtomicLong transactionId = new AtomicLong(1L);

	@Override
	public SpyqlTransactionListener onTransactionBegin(SpyqlTransactionDefinition transaction) {
		long transactionId = this.transactionId.getAndIncrement();
		subject.onNext(new TransactionBeginEvent(transactionId, transaction));
		return new TransactionListener(transactionId);
	}

	@Override
	public void onStatementExecute(String sql, Long executionTimeNs) {
		StatementExecuteEvent event = new StatementExecuteEvent(sql, executionTimeNs);
		subject.onNext(event);
	}

	public Disposable subscribeAsync(Consumer<? super Event> onNext) {
		return subject
				.observeOn(Schedulers.newThread())
				.subscribe(onNext);
	}

	public void subscribeAsync(Observer<? super Event> onNext) {
		subject
				.observeOn(Schedulers.newThread())
				.subscribe(onNext);
	}

	public Disposable subscribe(Consumer<? super Event> onNext) {
		return subject.subscribe(onNext);
	}

	public void subscribe(Observer<? super Event> onNext) {
		subject.subscribe(onNext);
	}

	public void attachListenerAsync(SpyqlListener listener) {
		subscribeAsync(new ObserverListener(listener));
	}

	public void attachListener(SpyqlListener listener) {
		subscribe(new ObserverListener(listener));
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
