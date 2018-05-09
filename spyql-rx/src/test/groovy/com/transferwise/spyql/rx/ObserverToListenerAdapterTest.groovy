package com.transferwise.spyql.rx

import com.transferwise.spyql.SpyqlConnectionListener
import com.transferwise.spyql.SpyqlDataSourceListener
import com.transferwise.spyql.SpyqlTransactionDefinition
import com.transferwise.spyql.SpyqlTransactionListener
import com.transferwise.spyql.rx.events.ConnectionAcquireEvent
import com.transferwise.spyql.rx.events.ConnectionCloseEvent
import com.transferwise.spyql.rx.events.StatementExecuteEvent
import com.transferwise.spyql.rx.events.TransactionBeginEvent
import com.transferwise.spyql.rx.events.TransactionCommitEvent
import com.transferwise.spyql.rx.events.TransactionRollbackEvent
import com.transferwise.spyql.rx.events.TransactionalStatementExecuteEvent
import io.reactivex.disposables.Disposable
import spock.lang.Specification

class ObserverToListenerAdapterTest extends Specification {
	def "decodes ConnectionAcquireEvent"() {
		given:
		def listener = Mock(SpyqlDataSourceListener)
		def observer = new ObserverToListenerAdapter(listener, 100, 100)

		when:
		observer.onNext(new ConnectionAcquireEvent(42, 321L))
		then:
		1 * listener.onGetConnection(321L) >> null
	}

	def "decodes StatementExecuteEvent"() {
		given:
		def listener = Mock(SpyqlDataSourceListener)
		def observer = new ObserverToListenerAdapter(listener, 100, 100)
		def connectionListener = Mock(SpyqlConnectionListener)

		when:
		observer.onNext(new ConnectionAcquireEvent(42, 321L))
		then:
		1 * listener.onGetConnection(321L) >> connectionListener

		when:
		observer.onNext(new StatementExecuteEvent(42, "SELECT 1", 123L))
		then:
		1 * connectionListener.onStatementExecute("SELECT 1", 123L)
	}

	def "doesn't fail if there is no transaction when TransactionalStatementExecuteEvent received"() {
		given:
		def listener = Mock(SpyqlDataSourceListener)
		def observer = new ObserverToListenerAdapter(listener, 100, 100)
		def connectionListener = Mock(SpyqlConnectionListener)

		when:
		observer.onNext(new ConnectionAcquireEvent(42, 321L))
		then:
		1 * listener.onGetConnection(321L) >> connectionListener

		when:
		observer.onNext(new TransactionalStatementExecuteEvent(42, 43, "SELECT 1", 123L))
		then:
		0 * _
	}

	def "decodes TransactionBeginEvent"() {
		given:
		def listener = Mock(SpyqlDataSourceListener)
		def observer = new ObserverToListenerAdapter(listener, 100, 100)
		def connectionListener = Mock(SpyqlConnectionListener)

		when:
		observer.onNext(new ConnectionAcquireEvent(42, 321L))
		then:
		1 * listener.onGetConnection(321L) >> connectionListener

		when:
		observer.onNext(new TransactionBeginEvent(42, 43, "tx", true, 1))
		then:
		1 * connectionListener.onTransactionBegin({ SpyqlTransactionDefinition transactionDefinition ->
			transactionDefinition.getName() == "tx"
			transactionDefinition.getReadOnly()
			transactionDefinition.getIsolationLevel() == 1
		}) >> Mock(SpyqlTransactionListener)
	}

	def "doesn't fail if more than maxConcurrentConnections connections were acquired"() {
		given:
		def listener = Mock(SpyqlDataSourceListener)
		def observer = new ObserverToListenerAdapter(listener, 2, 100)
		def connectionListener1 = Mock(SpyqlConnectionListener)
		def connectionListener2 = Mock(SpyqlConnectionListener)
		def connectionListener3 = Mock(SpyqlConnectionListener)

		expect:
		observer.getConnectionListenerMapSize() == 0

		when:
		observer.onNext(new ConnectionAcquireEvent(1, 321L))
		then:
		1 * listener.onGetConnection(321L) >> connectionListener1
		and:
		observer.getConnectionListenerMapSize() == 1

		when:
		observer.onNext(new ConnectionAcquireEvent(2, 321L))
		then:
		1 * listener.onGetConnection(321L) >> connectionListener2
		and:
		observer.getConnectionListenerMapSize() == 2

		when:
		observer.onNext(new ConnectionAcquireEvent(3, 321L))
		then:
		1 * listener.onGetConnection(321L) >> connectionListener3
		and:
		observer.getConnectionListenerMapSize() == 2
	}

	def "doesn't fail if more than maxConcurrentTransactions transactions"() {
		given:
		def listener = Mock(SpyqlDataSourceListener)
		def observer = new ObserverToListenerAdapter(listener, 100, 2)
		def connectionListener = Mock(SpyqlConnectionListener)
		def txListener1 = Mock(SpyqlTransactionListener)
		def txListener2 = Mock(SpyqlTransactionListener)
		def txListener3 = Mock(SpyqlTransactionListener)

		when:
		observer.onNext(new ConnectionAcquireEvent(42, 321L))
		then:
		1 * listener.onGetConnection(321L) >> connectionListener

		expect:
		observer.getTransactionListenerMapSize() == 0

		when:
		observer.onNext(new TransactionBeginEvent(42, 1, "tx", true, 1))
		then:
		1 * connectionListener.onTransactionBegin(_) >> txListener1
		observer.getTransactionListenerMapSize() == 1

		when:
		observer.onNext(new TransactionBeginEvent(42, 2, "tx", true, 1))
		then:
		1 * connectionListener.onTransactionBegin(_) >> txListener2
		observer.getTransactionListenerMapSize() == 2

		when:
		observer.onNext(new TransactionBeginEvent(42, 3, "tx", true, 1))
		then:
		1 * connectionListener.onTransactionBegin(_) >> txListener3
		observer.getTransactionListenerMapSize() == 2

		when:
		observer.onNext(new TransactionalStatementExecuteEvent(42, 2, "SELECT 2", 123))
		then:
		1 * txListener2.onStatementExecute("SELECT 2", 123)

		when:
		observer.onNext(new TransactionalStatementExecuteEvent(42, 1, "SELECT 1", 123))
		then:
		1 * txListener1.onStatementExecute("SELECT 1", 123)

		when:
		observer.onNext(new TransactionalStatementExecuteEvent(42, 3, "SELECT 3", 123))
		then:
		0 * _
	}

	def "decodes TransactionCommitEvent"() {
		given:
		def listener = Mock(SpyqlDataSourceListener)
		def observer = new ObserverToListenerAdapter(listener, 100, 100)
		def txListener = Mock(SpyqlTransactionListener)
		def connectionListener = Mock(SpyqlConnectionListener)

		when:
		observer.onNext(new ConnectionAcquireEvent(42, 321L))
		then:
		1 * listener.onGetConnection(321L) >> connectionListener

		when:
		observer.onNext(new TransactionBeginEvent(42, 43, "tx", true, 1))
		then:
		1 * connectionListener.onTransactionBegin(_) >> txListener

		when:
		observer.onNext(new TransactionCommitEvent(42, 43, 123))
		then:
		1 * txListener.onTransactionCommit(123)
	}

	def "doesn't fail if there is no transaction when TransactionCommitEvent received"() {
		given:
		def listener = Mock(SpyqlDataSourceListener)
		def observer = new ObserverToListenerAdapter(listener, 100, 100)
		def connectionListener = Mock(SpyqlConnectionListener)

		when:
		observer.onNext(new ConnectionAcquireEvent(42, 321L))
		then:
		1 * listener.onGetConnection(321L) >> connectionListener

		when:
		observer.onNext(new TransactionCommitEvent(42, 43, 123))
		then:
		0 * _
	}

	def "doesn't fail if there is no transaction when TransactionRollbackEvent received"() {
		given:
		def listener = Mock(SpyqlDataSourceListener)
		def observer = new ObserverToListenerAdapter(listener, 100, 100)
		def connectionListener = Mock(SpyqlConnectionListener)

		when:
		observer.onNext(new ConnectionAcquireEvent(42, 321L))
		then:
		1 * listener.onGetConnection(321L) >> connectionListener

		when:
		observer.onNext(new TransactionRollbackEvent(42, 43, 123))
		then:
		0 * _
	}

	def "removes transaction listener from the map on TransactionCommitEvent"() {
		given:
		def listener = Mock(SpyqlDataSourceListener)
		def observer = new ObserverToListenerAdapter(listener, 100, 2)
		def connectionListener = Mock(SpyqlConnectionListener)
		def txListener1 = Mock(SpyqlTransactionListener)
		def txListener2 = Mock(SpyqlTransactionListener)

		when:
		observer.onNext(new ConnectionAcquireEvent(42, 321L))
		then:
		1 * listener.onGetConnection(321L) >> connectionListener

		expect:
		observer.getTransactionListenerMapSize() == 0

		when:
		observer.onNext(new TransactionBeginEvent(42, 1, "tx", true, 1))
		then:
		1 * connectionListener.onTransactionBegin(_) >> txListener1
		observer.getTransactionListenerMapSize() == 1

		when:
		observer.onNext(new TransactionBeginEvent(42, 2, "tx", true, 1))
		then:
		1 * connectionListener.onTransactionBegin(_) >> txListener2
		observer.getTransactionListenerMapSize() == 2

		when:
		observer.onNext(new TransactionCommitEvent(42, 2, 123))
		then:
		txListener2.onTransactionCommit(123)
		observer.getTransactionListenerMapSize() == 1

		when:
		observer.onNext(new TransactionCommitEvent(42, 1, 123))
		then:
		txListener1.onTransactionCommit(123)
		observer.getTransactionListenerMapSize() == 0
	}

	def "removes transaction listener from the map on TransactionRollbackEvent"() {
		given:
		def listener = Mock(SpyqlDataSourceListener)
		def observer = new ObserverToListenerAdapter(listener, 100, 2)
		def connectionListener = Mock(SpyqlConnectionListener)
		def txListener1 = Mock(SpyqlTransactionListener)
		def txListener2 = Mock(SpyqlTransactionListener)

		when:
		observer.onNext(new ConnectionAcquireEvent(42, 321L))
		then:
		1 * listener.onGetConnection(321L) >> connectionListener

		expect:
		observer.getTransactionListenerMapSize() == 0

		when:
		observer.onNext(new TransactionBeginEvent(42, 1, "tx", true, 1))
		then:
		1 * connectionListener.onTransactionBegin(_) >> txListener1
		observer.getTransactionListenerMapSize() == 1

		when:
		observer.onNext(new TransactionBeginEvent(42, 2, "tx", true, 1))
		then:
		1 * connectionListener.onTransactionBegin(_) >> txListener2
		observer.getTransactionListenerMapSize() == 2

		when:
		observer.onNext(new TransactionRollbackEvent(42, 2, 123))
		then:
		txListener2.onTransactionCommit(123)
		observer.getTransactionListenerMapSize() == 1

		when:
		observer.onNext(new TransactionRollbackEvent(42, 1, 123))
		then:
		txListener1.onTransactionCommit(123)
		observer.getTransactionListenerMapSize() == 0
	}

	def "cannot create ObserverToListenerAdapter with null listener"() {
		when:
		new ObserverToListenerAdapter(null, 100, 100)
		then:
		thrown(IllegalArgumentException)
	}

	def "onError resets the state"() {
		given:
		def listener = Mock(SpyqlDataSourceListener)
		def observer = new ObserverToListenerAdapter(listener, 100, 2)
		def txListener1 = Mock(SpyqlTransactionListener)
		def connectionListener = Mock(SpyqlConnectionListener)

		when:
		observer.onNext(new ConnectionAcquireEvent(42, 321L))
		then:
		1 * listener.onGetConnection(321L) >> connectionListener

		when:
		observer.onNext(new TransactionBeginEvent(42, 1, "tx", true, 1))
		then:
		1 * connectionListener.onTransactionBegin(_) >> txListener1
		observer.getTransactionListenerMapSize() == 1

		when:
		observer.onError(new Exception())
		then:
		observer.getTransactionListenerMapSize() == 0
	}

	def "onComplete resets the state"() {
		given:
		def listener = Mock(SpyqlDataSourceListener)
		def observer = new ObserverToListenerAdapter(listener, 100, 2)
		def txListener1 = Mock(SpyqlTransactionListener)
		def connectionListener = Mock(SpyqlConnectionListener)

		when:
		observer.onNext(new ConnectionAcquireEvent(42, 321L))
		then:
		1 * listener.onGetConnection(321L) >> connectionListener

		when:
		observer.onNext(new TransactionBeginEvent(42, 1, "tx", true, 1))
		then:
		1 * connectionListener.onTransactionBegin(_) >> txListener1
		observer.getTransactionListenerMapSize() == 1

		when:
		observer.onComplete()
		then:
		observer.getTransactionListenerMapSize() == 0
	}

	def "automatically detaches when more than MAX_ERROR_COUNT mismatched ConnectionCloseEvent are received"() {
		given:
		def listener = Mock(SpyqlDataSourceListener)
		def disposable = Mock(Disposable)
		def observer = new ObserverToListenerAdapter(listener, 100, 2)
		observer.onSubscribe(disposable)

		when:
		(1..ObserverToListenerAdapter.MAX_ERROR_COUNT + 1).each {
			observer.onNext(new ConnectionCloseEvent(42))
		}
		then:
		1 * disposable.dispose();
	}

	def "automatically detaches when more than MAX_ERROR_COUNT mismatched TransactionBeginEvent are received"() {
		given:
		def listener = Mock(SpyqlDataSourceListener)
		def disposable = Mock(Disposable)
		def observer = new ObserverToListenerAdapter(listener, 100, 2)
		observer.onSubscribe(disposable)

		when:
		(1..ObserverToListenerAdapter.MAX_ERROR_COUNT + 1).each {
			observer.onNext(new TransactionBeginEvent(42, 43, 'foo.bar', false, 1))
		}
		then:
		1 * disposable.dispose();
	}

	def "automatically detaches when more than MAX_ERROR_COUNT mismatched StatementExecuteEvent are received"() {
		given:
		def listener = Mock(SpyqlDataSourceListener)
		def disposable = Mock(Disposable)
		def observer = new ObserverToListenerAdapter(listener, 100, 2)
		observer.onSubscribe(disposable)

		when:
		(1..ObserverToListenerAdapter.MAX_ERROR_COUNT + 1).each {
			observer.onNext(new StatementExecuteEvent(42, "SELECT 1", 123))
		}
		then:
		1 * disposable.dispose();
	}

	def "automatically detaches when more than MAX_ERROR_COUNT mismatched TransactionCommitEvent are received"() {
		given:
		def listener = Mock(SpyqlDataSourceListener)
		def disposable = Mock(Disposable)
		def observer = new ObserverToListenerAdapter(listener, 100, 2)
		observer.onSubscribe(disposable)
		def connectionListener = Mock(SpyqlConnectionListener)

		when:
		observer.onNext(new ConnectionAcquireEvent(42, 321L))
		then:
		1 * listener.onGetConnection(321L) >> connectionListener

		when:
		(1..ObserverToListenerAdapter.MAX_ERROR_COUNT + 1).each {
			observer.onNext(new TransactionCommitEvent(42, 1, 123))
		}
		then:
		1 * disposable.dispose();
	}
}
