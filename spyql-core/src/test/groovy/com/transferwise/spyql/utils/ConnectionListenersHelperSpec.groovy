package com.transferwise.spyql.utils

import com.transferwise.spyql.event.*
import com.transferwise.spyql.listener.SpyqlConnectionListener
import org.slf4j.Logger
import spock.lang.Specification

import java.time.Duration

class ConnectionListenersHelperSpec extends Specification {
    def "all listener methods are correctly called"() {
        given:
            SpyqlConnectionListener connectionListener = Mock(SpyqlConnectionListener)
            SpyqlConnectionListener connectionListener1 = Mock(SpyqlConnectionListener)
            List<SpyqlConnectionListener> connectionListeners = [connectionListener, connectionListener1, null]

            TransactionBeginEvent transactionBeginEvent = new TransactionBeginEvent()
            StatementExecuteEvent statementExecuteEvent = new StatementExecuteEvent()
            ConnectionCloseFailureEvent connectionCloseFailureEvent = new ConnectionCloseFailureEvent()
            TransactionRollbackFailureEvent transactionRollbackFailureEvent = new TransactionRollbackFailureEvent()
            TransactionCommitFailureEvent transactionCommitFailureEvent = new TransactionCommitFailureEvent()
            StatementExecuteFailureEvent statementExecuteFailureEvent = new StatementExecuteFailureEvent()
            TransactionCommitEvent transactionCommitEvent = new TransactionCommitEvent()
            TransactionRollbackEvent transactionRollbackEvent = new TransactionRollbackEvent()
            ConnectionCloseEvent connectionCloseEvent = new ConnectionCloseEvent()

            ConnectionListenersHelper helper = new ConnectionListenersHelper(new SimpleThrottler(Duration.ofMinutes(1), 100l))
            ConnectionListenersHelper.log = Mock(Logger)
        when:
            helper.onEvent(connectionListeners, transactionBeginEvent)
        then:
            1 * connectionListener.onEvent(transactionBeginEvent)
            1 * connectionListener.onTransactionBegin(transactionBeginEvent)
            1 * connectionListener1.onTransactionBegin(transactionBeginEvent)
            1 * connectionListener1.onEvent(transactionBeginEvent)
        when:
            helper.onEvent(connectionListeners, statementExecuteEvent)
        then:
            1 * connectionListener.onEvent(statementExecuteEvent)
            1 * connectionListener.onStatementExecute(statementExecuteEvent)
        when:
            helper.onEvent(connectionListeners, connectionCloseFailureEvent)
        then:
            1 * connectionListener.onEvent(connectionCloseFailureEvent)
            1 * connectionListener.onConnectionCloseFailure(connectionCloseFailureEvent)
        when:
            helper.onEvent(connectionListeners, transactionRollbackFailureEvent)
        then:
            1 * connectionListener.onEvent(transactionRollbackFailureEvent)
            1 * connectionListener.onTransactionRollbackFailure(transactionRollbackFailureEvent)
        when:
            helper.onEvent(connectionListeners, transactionCommitFailureEvent)
        then:
            1 * connectionListener.onEvent(transactionCommitFailureEvent)
            1 * connectionListener.onTransactionCommitFailure(transactionCommitFailureEvent)
        when:
            helper.onEvent(connectionListeners, statementExecuteFailureEvent)
        then:
            1 * connectionListener.onEvent(statementExecuteFailureEvent)
            1 * connectionListener.onStatementExecuteFailure(statementExecuteFailureEvent)
        when:
            helper.onEvent(connectionListeners, transactionCommitEvent)
        then:
            1 * connectionListener.onEvent(transactionCommitEvent)
            1 * connectionListener.onTransactionCommit(transactionCommitEvent)
        when:
            helper.onEvent(connectionListeners, transactionRollbackEvent)
        then:
            1 * connectionListener.onEvent(transactionRollbackEvent)
            1 * connectionListener.onTransactionRollback(transactionRollbackEvent)
        when:
            helper.onEvent(connectionListeners, connectionCloseEvent)
        then:
            1 * connectionListener.onEvent(connectionCloseEvent)
            1 * connectionListener.onConnectionClose(connectionCloseEvent)
        when:
            101.times {
                helper.onEvent(connectionListeners, connectionCloseEvent)
            }
        then: 'error log throttling works'
            connectionListener.onEvent(_) >> { throw new RuntimeException("Something went wrong.") }
            100 * helper.log.error(_, _)
    }
}
