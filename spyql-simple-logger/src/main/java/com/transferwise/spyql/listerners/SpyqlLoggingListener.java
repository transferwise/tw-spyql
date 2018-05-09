package com.transferwise.spyql.listerners;

import com.transferwise.spyql.SpyqlConnectionListener;
import com.transferwise.spyql.SpyqlDataSourceListener;
import com.transferwise.spyql.SpyqlTransactionDefinition;
import com.transferwise.spyql.SpyqlTransactionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class SpyqlLoggingListener implements SpyqlDataSourceListener {
	private static final Logger log = LoggerFactory.getLogger(SpyqlLoggingListener.class);

	private AtomicLong connectionId = new AtomicLong(0L);
	private AtomicLong transactionId = new AtomicLong(0L);

	@Override
	public SpyqlConnectionListener onGetConnection(Long acquireTimeNs) {
		long conId = connectionId.incrementAndGet();
		log.info("GET CONNECTION id: {}, time: {} ns", conId, acquireTimeNs);
		return new ConnectionListener(conId);
	}

	class ConnectionListener implements SpyqlConnectionListener {
		long connectionId;

		public ConnectionListener(long connectionId) {
			this.connectionId = connectionId;
		}

		@Override
		public SpyqlTransactionListener onTransactionBegin(SpyqlTransactionDefinition transactionDefinition) {
			if (!log.isInfoEnabled()) return null;
			long txId = transactionId.incrementAndGet();
			log.info("TRANSACTION BEGIN connectionId: {}, transactionId: {}, name: {}, readOnly {}, isolationLevel: {}",
					connectionId, txId, transactionDefinition.getName(), transactionDefinition.getReadOnly(), transactionDefinition.getIsolationLevel());
			return new TransactionListener(transactionDefinition, connectionId, txId);
		}

		@Override
		public void onStatementExecute(String sql, Long executionTimeNs) {
			if (!log.isInfoEnabled()) return;
			log.info("EXECUTE WITHOUT TRANSACTION connectionId: {}, transactionId: {}, sql: {}, after: {} ns", sql, executionTimeNs);
		}

		@Override
		public void onClose() {
			log.info("CLOSE CONNECTION connectionId: {}", connectionId);
		}
	}

	static class TransactionListener implements SpyqlTransactionListener {
		SpyqlTransactionDefinition transactionDefinition;
		long connectionId;
		long transactionId;

		TransactionListener(SpyqlTransactionDefinition transactionDefinition, long connectionId, long transactionId) {
			this.transactionDefinition = transactionDefinition;
			this.connectionId = connectionId;
			this.transactionId = transactionId;
		}

		@Override
		public void onTransactionCommit(Long transactionExecutionTimeNs) {
			log.info("TRANSACTION COMMIT connectionId: {}, transactionId: {}, name: {}, after: {} ns", connectionId, transactionId, transactionDefinition.getName(), transactionExecutionTimeNs);
		}

		@Override
		public void onTransactionRollback(Long transactionExecutionTimeNs) {
			log.info("TRANSACTION ROLLBACK connectionId: {}, transactionId: {}, name: {}, after: {} ns", connectionId, transactionId, transactionDefinition.getName(), transactionExecutionTimeNs);
		}

		@Override
		public void onStatementExecute(String sql, Long executionTimeNs) {
			log.info("EXECUTE IN TRANSACTION connectionId: {}, transactionId: {}, sql: {}, after: {} ns", connectionId, transactionId, sql, executionTimeNs);
		}

		@Override
		public void onStatementFailure(String sql, Long executionTimeNs, Throwable e) {
			log.info("Exception was thrown: {} after: {} ns", e, executionTimeNs);
		}
	}
}
