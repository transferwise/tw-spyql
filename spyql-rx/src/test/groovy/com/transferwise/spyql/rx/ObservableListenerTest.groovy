package com.transferwise.spyql.rx

import com.transferwise.spyql.SpyqlListener
import com.transferwise.spyql.SpyqlTransactionDefinition
import com.transferwise.spyql.rx.events.StatementExecuteEvent
import com.transferwise.spyql.rx.events.TransactionBeginEvent
import com.transferwise.spyql.rx.events.TransactionCommitEvent
import com.transferwise.spyql.rx.events.TransactionRollbackEvent
import com.transferwise.spyql.rx.events.TransactionalStatementExecuteEvent
import io.reactivex.subjects.Subject
import spock.lang.Specification

class ObservableListenerTest extends Specification {
	def "onTransactionBegin produces correct TransactionBeginEvent"() {
		given:
		def subject = Mock(Subject)
		def listener = new ObservableListener(100, subject)

		when:
		listener.onTransactionBegin(new SpyqlTransactionDefinition("tx", true, 1))

		then:
		1 * subject.onNext({ TransactionBeginEvent e ->
			e.getName() == 'tx'
			e.getReadOnly()
			e.getIsolationLevel() == 1
		})
	}

	def "onTransactionBegin produces correct TransactionBeginEvent with autoincrement id"() {
		given:
		def subject = Mock(Subject)
		def listener = new ObservableListener(100, subject)

		when:
		listener.onTransactionBegin(new SpyqlTransactionDefinition("tx", true, 1))
		then:
		1 * subject.onNext({ TransactionBeginEvent e -> e.getTransactionId() == 1 })

		when:
		listener.onTransactionBegin(new SpyqlTransactionDefinition("tx", true, 1))
		then:
		1 * subject.onNext({ TransactionBeginEvent e -> e.getTransactionId() == 2 })
	}

	def "onStatementExecute produces StatementExecuteEvent"() {
		given:
		def subject = Mock(Subject)
		def listener = new ObservableListener(100, subject)

		when:
		listener.onStatementExecute("SELECT 1", 123L)
		then:
		1 * subject.onNext({ StatementExecuteEvent e ->
			e.getSql() == "SELECT 1"
			e.getExecutionTimeNs() == 123L
		})
	}

	def "TransactionListener.onTransactionCommit produces TransactionCommitEvent"() {
		given:
		def subject = Mock(Subject)
		def listener = new ObservableListener(100, subject)
		def txListener = listener.onTransactionBegin(new SpyqlTransactionDefinition("tx", true, 1))

		when:
		txListener.onTransactionCommit(123L)
		then:
		1 * subject.onNext({ TransactionCommitEvent e ->
			e.getTransactionId() == 1
			e.getExecutionTimeNs() == 123L
		})
	}

	def "TransactionListener.onTransactionRollback produces TransactionRollbackEvent"() {
		given:
		def subject = Mock(Subject)
		def listener = new ObservableListener(100, subject)
		def txListener = listener.onTransactionBegin(new SpyqlTransactionDefinition("tx", true, 1))

		when:
		txListener.onTransactionRollback(123L)
		then:
		1 * subject.onNext({ TransactionRollbackEvent e ->
			e.getTransactionId() == 1
			e.getExecutionTimeNs() == 123L
		})
	}

	def "TransactionListener.onStatementExecute produces TransactionalStatementExecuteEvent"() {
		given:
		def subject = Mock(Subject)
		def listener = new ObservableListener(100, subject)
		def txListener = listener.onTransactionBegin(new SpyqlTransactionDefinition("tx", true, 1))

		when:
		txListener.onStatementExecute("SELECT 1", 123L)
		then:
		1 * subject.onNext({ TransactionalStatementExecuteEvent e ->
			e.getTransactionId() == 1
			e.getSql() == "SELECT 1"
			e.getExecutionTimeNs() == 123L
		})
	}

	def "attachAsyncListener does not fail"() {
		given:
		def subject = Mock(Subject)
		def listener = new ObservableListener(100, subject)

		when:
		listener.attachAsyncListener(Mock(SpyqlListener))
		then:
		noExceptionThrown()
	}

	def "attachListener does not fail"() {
		given:
		def subject = Mock(Subject)
		def listener = new ObservableListener(100, subject)

		when:
		listener.attachListener(Mock(SpyqlListener))
		then:
		noExceptionThrown()
	}
}
