package com.transferwise.spyql.rx

import com.transferwise.spyql.GetConnectionException
import com.transferwise.spyql.GetConnectionNull
import com.transferwise.spyql.GetConnectionSuccess
import com.transferwise.spyql.SpyqlDataSourceListener
import com.transferwise.spyql.SpyqlTransactionDefinition
import com.transferwise.spyql.rx.events.ConnectionAcquireEvent
import com.transferwise.spyql.rx.events.StatementExecuteEvent
import com.transferwise.spyql.rx.events.TransactionBeginEvent
import com.transferwise.spyql.rx.events.TransactionCommitEvent
import com.transferwise.spyql.rx.events.TransactionRollbackEvent
import com.transferwise.spyql.rx.events.TransactionalStatementExecuteEvent
import io.reactivex.subjects.Subject
import spock.lang.Specification

class ObservableListenerTest extends Specification {
	def "onGetConnection produces correct GetConnectionEvent"() {
		given:
		def subject = Mock(Subject)
		def listener = new ObservableListener(100, 100, subject)
		def exception = new RuntimeException("some exception message")
		def exceptionName = exception.getClass().getName()
		def exceptionMessage = exception.getMessage()

		when:
		listener.onGetConnection(new GetConnectionSuccess(123L))
		then:
		1 * subject.onNext({ ConnectionAcquireEvent e ->
			e.getConnectionId() == 1L
			def result = (GetConnectionSuccess) e.getResult()
			result.executionTimeNs == 123L
		})

		when:
		listener.onGetConnection(new GetConnectionNull(123L))
		then:
		1 * subject.onNext({ ConnectionAcquireEvent e ->
			e.getConnectionId() == 1L
			def result = (GetConnectionNull) e.getResult()
			result.executionTimeNs == 123L
		})

		when:
		listener.onGetConnection(new GetConnectionException(exceptionName, exceptionMessage, 123L))
		then:
		1 * subject.onNext({ ConnectionAcquireEvent e ->
			def result = (GetConnectionException) e.getResult()
			e.getConnectionId() == 1L
			result.exceptionName == exceptionName
			result.exceptionMessage == exceptionMessage
			result.executionTimeNs == 123L
		})
	}

	def "onTransactionBegin produces correct TransactionBeginEvent"() {
		given:
		def subject = Mock(Subject)
		def listener = new ObservableListener(100, 100, subject)
		def connectionListener = listener.onGetConnection(new GetConnectionSuccess(123L))

		when:
		connectionListener.onTransactionBegin(new SpyqlTransactionDefinition("tx", true, 1))
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
		def listener = new ObservableListener(100, 100, subject)
		def connectionListener = listener.onGetConnection(new GetConnectionSuccess(123L))

		when:
		connectionListener.onTransactionBegin(new SpyqlTransactionDefinition("tx", true, 1))
		then:
		1 * subject.onNext({ TransactionBeginEvent e -> e.getTransactionId() == 1 })

		when:
		connectionListener.onTransactionBegin(new SpyqlTransactionDefinition("tx", true, 1))
		then:
		1 * subject.onNext({ TransactionBeginEvent e -> e.getTransactionId() == 2 })
	}

	def "onStatementExecute produces StatementExecuteEvent"() {
		given:
		def subject = Mock(Subject)
		def listener = new ObservableListener(100, 100, subject)
		def connectionListener = listener.onGetConnection(new GetConnectionSuccess(123L))

		when:
		connectionListener.onStatementExecute("SELECT 1", 123L)
		then:
		1 * subject.onNext({ StatementExecuteEvent e ->
			e.getSql() == "SELECT 1"
			e.getExecutionTimeNs() == 123L
		})
	}

	def "TransactionListener.onTransactionCommit produces TransactionCommitEvent"() {
		given:
		def subject = Mock(Subject)
		def listener = new ObservableListener(100, 100, subject)
		def connectionListener = listener.onGetConnection(new GetConnectionSuccess(123L))
		def txListener = connectionListener.onTransactionBegin(new SpyqlTransactionDefinition("tx", true, 1))

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
		def listener = new ObservableListener(100, 100, subject)
		def connectionListener = listener.onGetConnection(new GetConnectionSuccess(123L))
		def txListener = connectionListener.onTransactionBegin(new SpyqlTransactionDefinition("tx", true, 1))

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
		def listener = new ObservableListener(100, 100, subject)
		def connectionListener = listener.onGetConnection(new GetConnectionSuccess(123L))
		def txListener = connectionListener.onTransactionBegin(new SpyqlTransactionDefinition("tx", true, 1))

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
		def listener = new ObservableListener(100, 100, subject)

		when:
		listener.attachAsyncListener(Mock(SpyqlDataSourceListener))
		then:
		noExceptionThrown()
	}

	def "attachListener does not fail"() {
		given:
		def subject = Mock(Subject)
		def listener = new ObservableListener(100, 100, subject)

		when:
		listener.attachListener(Mock(SpyqlDataSourceListener))
		then:
		noExceptionThrown()
	}
}
