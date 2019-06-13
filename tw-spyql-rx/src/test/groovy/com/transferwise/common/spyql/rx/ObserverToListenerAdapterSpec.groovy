package com.transferwise.common.spyql.rx

import com.transferwise.common.spyql.event.*
import com.transferwise.common.spyql.listener.SpyqlConnectionListener
import com.transferwise.common.spyql.listener.SpyqlDataSourceListener
import io.reactivex.disposables.Disposable
import spock.lang.Specification

class ObserverToListenerAdapterSpec extends Specification {
    def "work for usual use case"() {
        given:
            def dataSourceListener = Mock(SpyqlDataSourceListener)
            def connectionListener = Mock(SpyqlConnectionListener)

            def observer = new ObserverToListenerAdapter(dataSourceListener, 100)
            def getConnectionEvent = new GetConnectionEvent().setConnectionId(1l)
            def transactionBeginEvent = new TransactionBeginEvent().setConnectionId(1l).setTransactionId(1l)
            def statementExecuteEvent = new StatementExecuteEvent().setConnectionId(1l).setTransactionId(1l)
            def transactionCommitEvent = new TransactionCommitEvent().setConnectionId(1l).setTransactionId(1l)
            def connectionCloseEvent = new ConnectionCloseEvent().setConnectionId(1l)
        when:
            observer.onNext(getConnectionEvent)
        then:
            1 * dataSourceListener.onGetConnection(getConnectionEvent) >> connectionListener
            observer.getConnectionListenerMapSize() == 1
        when:
            observer.onNext(transactionBeginEvent)
        then:
            1 * connectionListener.onTransactionBegin(transactionBeginEvent)
        when:
            observer.onNext(statementExecuteEvent)
        then:
            1 * connectionListener.onStatementExecute(statementExecuteEvent)
        when:
            observer.onNext(transactionCommitEvent)
        then:
            1 * connectionListener.onTransactionCommit(transactionCommitEvent)
        when:
            observer.onNext(connectionCloseEvent)
        then:
            1 * connectionListener.onConnectionClose(connectionCloseEvent)
            observer.getConnectionListenerMapSize() == 0
    }

    def "doesn't fail if more than maxConcurrentConnections connections were acquired"() {
        given:
            def listener = Mock(SpyqlDataSourceListener)
            def observer = new ObserverToListenerAdapter(listener, 2)
            def connectionListener1 = Mock(SpyqlConnectionListener)
            def connectionListener2 = Mock(SpyqlConnectionListener)
            def connectionListener3 = Mock(SpyqlConnectionListener)
        expect:
            observer.getConnectionListenerMapSize() == 0
        when:
            observer.onNext(new GetConnectionEvent().setConnectionId(1l))
        then:
            1 * listener.onGetConnection(_) >> connectionListener1
        and:
            observer.getConnectionListenerMapSize() == 1
        when:
            observer.onNext(new GetConnectionEvent().setConnectionId(2l))
        then:
            1 * listener.onGetConnection(_) >> connectionListener2
        and:
            observer.getConnectionListenerMapSize() == 2
        when:
            observer.onNext(new GetConnectionEvent().setConnectionId(3l))
        then:
            1 * listener.onGetConnection(_) >> connectionListener3
        and:
            observer.getConnectionListenerMapSize() == 2
    }

    def "automatically detaches when more than MAX_ERROR_COUNT mismatched ConnectionEvents are received"() {
        given:
            def listener = Mock(SpyqlDataSourceListener)
            def disposable = Mock(Disposable)
            def observer = new ObserverToListenerAdapter(listener, 100)
            observer.onSubscribe(disposable)

        when:
            (1..ObserverToListenerAdapter.MAX_ERROR_COUNT + 1).each {
                observer.onNext(new ConnectionCloseEvent())
            }
        then:
            1 * disposable.dispose()
    }
}
