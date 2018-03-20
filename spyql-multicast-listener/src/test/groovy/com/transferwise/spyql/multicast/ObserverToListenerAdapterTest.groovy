package com.transferwise.spyql.multicast

import com.transferwise.spyql.SpyqlListener
import com.transferwise.spyql.SpyqlTransactionDefinition
import com.transferwise.spyql.SpyqlTransactionListener
import com.transferwise.spyql.multicast.events.StatementExecuteEvent
import com.transferwise.spyql.multicast.events.TransactionBeginEvent
import com.transferwise.spyql.multicast.events.TransactionCommitEvent
import com.transferwise.spyql.multicast.events.TransactionRollbackEvent
import com.transferwise.spyql.multicast.events.TransactionalStatementExecuteEvent
import io.reactivex.disposables.Disposable
import org.slf4j.Logger
import spock.lang.Specification

class ObserverToListenerAdapterTest extends Specification {

	def "decodes StatementExecuteEvent"() {
		given:
		def listener = Mock(SpyqlListener)
		def observer = new ObserverToListenerAdapter(listener, 100)

		when:
		observer.onNext(new StatementExecuteEvent("SELECT 1", 123L))
		then:
		1 * listener.onStatementExecute("SELECT 1", 123L)
	}

	def "doesn't fail if there is no transaction when TransactionalStatementExecuteEvent received"() {
		given:
		def listener = Mock(SpyqlListener)
		def observer = new ObserverToListenerAdapter(listener, 100)

		when:
		observer.onNext(new TransactionalStatementExecuteEvent("SELECT 1", 123L, 42))
		then:
		0 * _
	}

	def "decodes TransactionBeginEvent"() {
		given:
		def listener = Mock(SpyqlListener)
		def observer = new ObserverToListenerAdapter(listener, 100)

		when:
		observer.onNext(new TransactionBeginEvent(42, "tx", true, 1))
		then:
		1 * listener.onTransactionBegin({ SpyqlTransactionDefinition transactionDefinition ->
			transactionDefinition.getName() == "tx"
			transactionDefinition.getReadOnly()
			transactionDefinition.getIsolationLevel() == 1
		}) >> Mock(SpyqlTransactionListener)
	}

	def "doesn't fail if more than transactionListenerMapMaxSize transactions"() {
		given:
		def listener = Mock(SpyqlListener)
		def observer = new ObserverToListenerAdapter(listener, 2)
		def txListener1 = Mock(SpyqlTransactionListener)
		def txListener2 = Mock(SpyqlTransactionListener)
		def txListener3 = Mock(SpyqlTransactionListener)

		expect:
		observer.getTransactionListenerMapSize() == 0

		when:
		observer.onNext(new TransactionBeginEvent(1, "tx", true, 1))
		then:
		1 * listener.onTransactionBegin(_) >> txListener1
		observer.getTransactionListenerMapSize() == 1

		when:
		observer.onNext(new TransactionBeginEvent(2, "tx", true, 1))
		then:
		1 * listener.onTransactionBegin(_) >> txListener2
		observer.getTransactionListenerMapSize() == 2

		when:
		observer.onNext(new TransactionBeginEvent(3, "tx", true, 1))
		then:
		1 * listener.onTransactionBegin(_) >> txListener3
		observer.getTransactionListenerMapSize() == 2

		when:
		observer.onNext(new TransactionalStatementExecuteEvent("SELECT 2", 123, 2))
		then:
		1 * txListener2.onStatementExecute("SELECT 2", 123)

		when:
		observer.onNext(new TransactionalStatementExecuteEvent("SELECT 1", 123, 1))
		then:
		1 * txListener1.onStatementExecute("SELECT 1", 123)

		when:
		observer.onNext(new TransactionalStatementExecuteEvent("SELECT 3", 123, 3))
		then:
		0 * _
	}

	def "decodes TransactionCommitEvent"() {
		given:
		def listener = Mock(SpyqlListener)
		def observer = new ObserverToListenerAdapter(listener, 100)
		def txListener = Mock(SpyqlTransactionListener)

		when:
		observer.onNext(new TransactionBeginEvent(42, "tx", true, 1))
		then:
		1 * listener.onTransactionBegin(_) >> txListener

		when:
		observer.onNext(new TransactionCommitEvent(42, 123))
		then:
		1 * txListener.onTransactionCommit(123)
		and:
		1 * txListener.onTransactionCommit()
		and:
		1 * txListener.onTransactionComplete(123)
	}

	def "doesn't fail if there is no transaction when TransactionCommitEvent received"() {
		given:
		def listener = Mock(SpyqlListener)
		def observer = new ObserverToListenerAdapter(listener, 100)

		when:
		observer.onNext(new TransactionCommitEvent(42, 123))
		then:
		0 * _
	}

	def "doesn't fail if there is no transaction when TransactionRollbackEvent received"() {
		given:
		def listener = Mock(SpyqlListener)
		def observer = new ObserverToListenerAdapter(listener, 100)

		when:
		observer.onNext(new TransactionRollbackEvent(42, 123))
		then:
		0 * _
	}

	def "removes transaction listener from the map on TransactionCommitEvent"() {
		given:
		def listener = Mock(SpyqlListener)
		def observer = new ObserverToListenerAdapter(listener, 2)
		def txListener1 = Mock(SpyqlTransactionListener)
		def txListener2 = Mock(SpyqlTransactionListener)

		expect:
		observer.getTransactionListenerMapSize() == 0

		when:
		observer.onNext(new TransactionBeginEvent(1, "tx", true, 1))
		then:
		1 * listener.onTransactionBegin(_) >> txListener1
		observer.getTransactionListenerMapSize() == 1

		when:
		observer.onNext(new TransactionBeginEvent(2, "tx", true, 1))
		then:
		1 * listener.onTransactionBegin(_) >> txListener2
		observer.getTransactionListenerMapSize() == 2

		when:
		observer.onNext(new TransactionCommitEvent(2, 123))
		then:
		txListener2.onTransactionCommit(123)
		observer.getTransactionListenerMapSize() == 1

		when:
		observer.onNext(new TransactionCommitEvent(1, 123))
		then:
		txListener1.onTransactionCommit(123)
		observer.getTransactionListenerMapSize() == 0
	}

	def "removes transaction listener from the map on TransactionRollbackEvent"() {
		given:
		def listener = Mock(SpyqlListener)
		def observer = new ObserverToListenerAdapter(listener, 2)
		def txListener1 = Mock(SpyqlTransactionListener)
		def txListener2 = Mock(SpyqlTransactionListener)

		expect:
		observer.getTransactionListenerMapSize() == 0

		when:
		observer.onNext(new TransactionBeginEvent(1, "tx", true, 1))
		then:
		1 * listener.onTransactionBegin(_) >> txListener1
		observer.getTransactionListenerMapSize() == 1

		when:
		observer.onNext(new TransactionBeginEvent(2, "tx", true, 1))
		then:
		1 * listener.onTransactionBegin(_) >> txListener2
		observer.getTransactionListenerMapSize() == 2

		when:
		observer.onNext(new TransactionRollbackEvent(2, 123))
		then:
		txListener2.onTransactionCommit(123)
		observer.getTransactionListenerMapSize() == 1

		when:
		observer.onNext(new TransactionRollbackEvent(1, 123))
		then:
		txListener1.onTransactionCommit(123)
		observer.getTransactionListenerMapSize() == 0
	}

	def "cannot create ObserverToListenerAdapter with null listener"() {
		when:
		new ObserverToListenerAdapter(null, 123)
		then:
		thrown(IllegalArgumentException)
	}

	def "onError resets the state"() {
		given:
		def listener = Mock(SpyqlListener)
		def observer = new ObserverToListenerAdapter(listener, 2)
		def txListener1 = Mock(SpyqlTransactionListener)

		when:
		observer.onNext(new TransactionBeginEvent(1, "tx", true, 1))
		then:
		1 * listener.onTransactionBegin(_) >> txListener1
		observer.getTransactionListenerMapSize() == 1

		when:
		observer.onError(new Exception())
		then:
		observer.getTransactionListenerMapSize() == 0
	}

	def "onComplete resets the state"() {
		given:
		def listener = Mock(SpyqlListener)
		def observer = new ObserverToListenerAdapter(listener, 2)
		def txListener1 = Mock(SpyqlTransactionListener)

		when:
		observer.onNext(new TransactionBeginEvent(1, "tx", true, 1))
		then:
		1 * listener.onTransactionBegin(_) >> txListener1
		observer.getTransactionListenerMapSize() == 1

		when:
		observer.onComplete()
		then:
		observer.getTransactionListenerMapSize() == 0
	}

	def "automatically detaches when MAX_ERROR_COUNT is reached"() {
		given:
		def listener = Mock(SpyqlListener)
		def disposable = Mock(Disposable)
		def observer = new ObserverToListenerAdapter(listener, 2)
		observer.onSubscribe(disposable)
		def txListener1 = Mock(SpyqlTransactionListener)
		_ * listener.onTransactionBegin(_) >> txListener1

		when:
		(1..ObserverToListenerAdapter.MAX_ERROR_COUNT + 1).each {
			observer.onNext(new TransactionCommitEvent(1, 123))
		}
		then:
		1 * disposable.dispose();
	}
}
