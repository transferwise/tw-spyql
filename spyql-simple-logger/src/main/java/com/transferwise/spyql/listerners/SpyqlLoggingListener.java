package com.transferwise.spyql.listerners;

import com.transferwise.spyql.SpyqlListener;
import com.transferwise.spyql.SpyqlTransactionDefinition;
import com.transferwise.spyql.SpyqlTransactionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class SpyqlLoggingListener implements SpyqlListener {
	private static final Logger log = LoggerFactory.getLogger(SpyqlTransactionListener.class);
	private static AtomicLong transactionId = new AtomicLong(0L);

	@Override
	public SpyqlTransactionListener onTransactionBegin(SpyqlTransactionDefinition transactionDefinition) {
		if (!log.isInfoEnabled()) return null;
		long id = transactionId.incrementAndGet();
		log.info("TRANSACTION BEGIN id: {}, name: {}, readOnly {}, isolationLevel: {}",
				id, transactionDefinition.getName(), transactionDefinition.getReadOnly(), transactionDefinition.getIsolationLevel());
		return new TransactionListener(transactionDefinition, id);
	}

	@Override
	public void onStatementExecute(String sql, Long executionTimeNs) {
		if (!log.isInfoEnabled()) return;
		log.info("EXECUTE WITHOUT TRANSACTION id: {}, sql: {}, after: {} ns", sql, executionTimeNs);
	}

	static class TransactionListener implements SpyqlTransactionListener {
		SpyqlTransactionDefinition transactionDefinition;
		Long id;
		Long startTime;

		TransactionListener(SpyqlTransactionDefinition transactionDefinition, Long id) {
			this.transactionDefinition = transactionDefinition;
			this.id = id;
			this.startTime = System.nanoTime();
		}

		@Override
		public void onTransactionCommit() {
			log.info("TRANSACTION COMMIT id: {}, name: {}", id, transactionDefinition.getName());
		}

		@Override
		public void onTransactionRollback() {
			log.info("TRANSACTION ROLLBACK id: {}, name: {}", id, transactionDefinition.getName());
		}

		@Override
		public void onTransactionComplete() {
			long currentTime = System.nanoTime();
			log.info("TRANSACTION COMPLETE id: {}, name: {}, after: {} ns", id, transactionDefinition.getName(), currentTime - startTime);
		}

		@Override
		public void onStatementExecute(String sql, Long executionTimeNs) {
			log.info("EXECUTE IN TRANSACTION id: {}, sql: {}, after: {} ns", id, sql, executionTimeNs);
		}
	}
}
