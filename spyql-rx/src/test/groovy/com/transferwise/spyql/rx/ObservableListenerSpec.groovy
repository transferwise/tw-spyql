package com.transferwise.spyql.rx

import com.transferwise.spyql.event.GetConnectionEvent
import com.transferwise.spyql.event.GetConnectionFailureEvent
import com.transferwise.spyql.event.TransactionBeginEvent
import com.transferwise.spyql.listener.SpyqlDataSourceListener
import com.transferwise.spyql.utils.ConnectionListenersHelper
import com.transferwise.spyql.utils.SimpleThrottler
import io.reactivex.subjects.Subject
import spock.lang.Specification

import java.time.Duration

class ObservableListenerSpec extends Specification {
    ConnectionListenersHelper connectionListenersHelper

    def setup() {
        connectionListenersHelper = new ConnectionListenersHelper(new SimpleThrottler(Duration.ofMinutes(1), 100l));
    }

    def "correct datasource events are created on listener events"() {
        given:
            def subject = Mock(Subject)
            def listener = new ObservableListener(100, subject)
            def getConnectionEvent = new GetConnectionEvent().setConnectionId(1).setExecutionTimeNs(123l)
            def getConnectionFailureEvent = new GetConnectionFailureEvent().setConnectionId(1).setExecutionTimeNs(123L)
        when:
            listener.onGetConnection(getConnectionEvent)
        then:
            1 * subject.onNext(getConnectionEvent)
        when:
            listener.onGetConnectionFailure(getConnectionFailureEvent)
        then:
            1 * subject.onNext(getConnectionFailureEvent)
    }

    def "correct connection events are created on listener events"() {
        given:
            def subject = Mock(Subject)
            def listener = new ObservableListener(100, subject)
            def connectionListener = listener.onGetConnection(new GetConnectionEvent().setConnectionId(1))

            def transactionBeginEvent = new TransactionBeginEvent()
        when:
            connectionListenersHelper.onEvent(connectionListener, transactionBeginEvent);
        then:
            1 * subject.onNext(transactionBeginEvent)
    }

    def "attachAsyncListener does not fail"() {
        given:
            def subject = Mock(Subject)
            def listener = new ObservableListener(100, subject)

        when:
            listener.attachAsyncListener(Mock(SpyqlDataSourceListener))
        then:
            noExceptionThrown()
    }

    def "attachListener does not fail"() {
        given:
            def subject = Mock(Subject)
            def listener = new ObservableListener(100, subject)

        when:
            listener.attachListener(Mock(SpyqlDataSourceListener))
        then:
            noExceptionThrown()
    }
}
