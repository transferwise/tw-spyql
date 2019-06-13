package com.transferwise.common.spyql

import com.transferwise.common.spyql.event.*
import com.transferwise.common.spyql.listener.SpyqlConnectionListener
import com.transferwise.common.spyql.listener.SpyqlDataSourceListener
import org.slf4j.Logger
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.*

class SpyqlDataSourceSpec extends Specification {
    DataSource originalDataSource
    SpyqlDataSourceListener listener
    SpyqlConnectionListener connectionListener
    SpyqlDataSource spyqlDataSource
    Connection originalConnection
    PreparedStatement originalStatement

    protected long transactionIdInBegin

    def setupSpyql(boolean attachListener = true) {
        originalDataSource = Mock(DataSource)
        if (attachListener) {
            listener = Mock(SpyqlDataSourceListener)
            connectionListener = Mock(SpyqlConnectionListener)
            spyqlDataSource = new SpyqlDataSource(originalDataSource, listener)
            originalConnection = Mock(Connection)
            originalStatement = Mock(PreparedStatement)
        } else {
            spyqlDataSource = new SpyqlDataSource(originalDataSource)
        }
        spyqlDataSource.log = Mock(Logger)
    }

    def "Spyql data source can be initialized in multiple ways"() {
        given:
            def originalDataSource = Mock(DataSource)
            def dataSourceListener = Mock(SpyqlDataSourceListener)
        when:
            def dataSource = new SpyqlDataSource(originalDataSource)
        then:
            dataSource.getDatabaseName() == null
            dataSource.dataSource == originalDataSource
            dataSource.getDataSourceListeners().isEmpty()
        when:
            dataSource = new SpyqlDataSource(originalDataSource, "SuperDb")
        then:
            dataSource.getDatabaseName() == "SuperDb"
            dataSource.dataSource == originalDataSource
            dataSource.getDataSourceListeners().isEmpty()
        when:
            dataSource = new SpyqlDataSource(originalDataSource, "SuperDb", dataSourceListener)
        then:
            dataSource.getDatabaseName() == "SuperDb"
            dataSource.dataSource == originalDataSource
            dataSource.getDataSourceListeners()[0] == dataSourceListener
        when:
            dataSource = new SpyqlDataSource(originalDataSource, dataSourceListener)
        then:
            dataSource.getDatabaseName() == null
            dataSource.dataSource == originalDataSource
            dataSource.getDataSourceListeners()[0] == dataSourceListener
    }

    def "getConnection calls original getConnection when no listener attached"() {
        given:
            setupSpyql(false)

        when:
            spyqlDataSource.getConnection()

        then:
            1 * originalDataSource.getConnection() >> Mock(Connection)
    }

    def "getConnection with username and password calls original getConnection when no listener attached"() {
        given:
            setupSpyql(false)

        when:
            spyqlDataSource.getConnection("username", "password")

        then:
            1 * originalDataSource.getConnection("username", "password") >> Mock(Connection)
    }

    def "onGetConnection is called when connection is acquired using getConnection"() {
        given:
            setupSpyql()

        when:
            spyqlDataSource.getConnection()

        then:
            1 * originalDataSource.getConnection() >> Mock(Connection)
        and:
            1 * listener.onGetConnection({ GetConnectionEvent result -> result.executionTimeNs > 0 })
    }

    def "onGetConnection is called when connection is acquired using getConnection with username and password"() {
        given:
            setupSpyql()

        when:
            spyqlDataSource.getConnection("username", "password")

        then:
            1 * originalDataSource.getConnection("username", "password") >> Mock(Connection)
        and:
            1 * listener.onGetConnection({ GetConnectionEvent result -> result.executionTimeNs > 0 })
    }

    def "if target data source returns null when getConnection is called then proxy also returns null"() {
        given:
            setupSpyql()

        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> null
        and:
            1 * listener.onGetConnectionFailure({ GetConnectionFailureEvent result ->
                result.getExecutionTimeNs() > 0 && result.nullReturned
            })
        and:
            connection == null
    }

    def "if target data source throws when getConnection is called then proxy also throws"() {
        given:
            setupSpyql()
            Throwable t = new SQLException("foo")
        when:
            spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> {
                throw t
            }
        and:
            1 * listener.onGetConnectionFailure({ GetConnectionFailureEvent result ->
                result.getExecutionTimeNs() > 0
                result.throwable == t
            })
        and:
            def ex = thrown(SQLException)
            ex.message == "foo"
    }

    def "onClose is called when connection is closed"() {
        given:
            setupSpyql()

        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> originalConnection
        and:
            1 * listener.onGetConnection({ GetConnectionEvent success -> success.executionTimeNs > 0 }) >> connectionListener

        when:
            connection.close()
        then:
            1 * connectionListener.onConnectionClose(_)
    }

    def "when commit is called without transaction being created before, an empty transaction event is sent"() {
        given:
            setupSpyql()
        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> originalConnection
        and:
            1 * listener.onGetConnection({ GetConnectionEvent success -> success.executionTimeNs > 0 }) >> connectionListener
        when:
            connection.commit()
        then:
            1 * originalConnection.commit()
        and: 'new empty transaction was registered'
            1 * connectionListener.onTransactionBegin({ TransactionBeginEvent event ->
                transactionIdInBegin = event.getTransactionId()
                event.isEmptyTransaction()
            })
        and: 'transaction commit event is sent'
            1 * connectionListener.onTransactionCommit({ TransactionCommitEvent event -> event.getTransactionId() == transactionIdInBegin })
        and:
            0 * connectionListener.onStatementExecute(_, _)
    }

    def "when rollback is called without transaction being created before, an empty transaction event is sent"() {
        given:
            setupSpyql()
        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> originalConnection
        and:
            1 * listener.onGetConnection({ GetConnectionEvent success -> success.executionTimeNs > 0 }) >> connectionListener

        when:
            connection.rollback()
        then:
            1 * originalConnection.rollback()
        and: 'new empty transaction was registered'
            1 * connectionListener.onTransactionBegin({ TransactionBeginEvent event ->
                transactionIdInBegin = event.getTransactionId()
                event.isEmptyTransaction()
            })
        and: 'transaction commit event is sent'
            1 * connectionListener.onTransactionRollback({ TransactionRollbackEvent event -> event.getTransactionId() == transactionIdInBegin })
        and:
            0 * connectionListener.onStatementExecute(_, _)
    }

    def "when outside transaction SpyqlListener.onStatementExecute is called when statement is executed"() {
        given:
            setupSpyql()

        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> originalConnection
        and:
            1 * listener.onGetConnection({ GetConnectionEvent success -> success.executionTimeNs > 0 }) >> connectionListener

        when:
            def statement = connection.prepareStatement("SELECT 1")
        then:
            1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement
            0 * connectionListener.onTransactionBegin(_)

        when:
            statement.execute()
        then:
            1 * originalConnection.getAutoCommit() >> true
            1 * originalStatement.execute() >> true
            0 * connectionListener.onTransactionBegin(_)
            1 * connectionListener.onStatementExecute({ StatementExecuteEvent result ->
                result.sql == "SELECT 1" && result.transactionId == null && result.executionTimeNs > 0
            })
    }

    def "when outside transaction SpyqlconnectionListener.onTransactionBegin is called when transaction is started"() {
        given:
            setupSpyql()

        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> originalConnection
        and:
            1 * listener.onGetConnection({ GetConnectionEvent success -> success.executionTimeNs > 0 }) >> connectionListener

        when:
            def statement = connection.prepareStatement("SELECT 1")
        then:
            1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement
            0 * connectionListener.onTransactionBegin(_)

        when:
            statement.execute()
        then:
            1 * originalConnection.getAutoCommit() >> false
            1 * originalStatement.execute() >> true
            1 * connectionListener.onTransactionBegin({ TransactionBeginEvent event ->
                !event.transactionDefinition.readOnly && !event.emptyTransaction
            })
    }

    def "when inside transaction SpyqlconnectionListener.onTransactionBegin is not called again"() {
        given: "a connection with autoCommit = false"
            setupSpyql()
            originalConnection.getAutoCommit() >> false
            def originalStatement2 = Mock(PreparedStatement)

        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> originalConnection
        and:
            1 * listener.onGetConnection({ GetConnectionEvent success -> success.executionTimeNs > 0 }) >> connectionListener

        when: "the first statement is prepared"
            def statement = connection.prepareStatement("SELECT 1")
        then: "transaction is not started"
            1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement
            0 * connectionListener.onTransactionBegin(_)

        when: "the first statement is executed"
            statement.execute()
        then: "transaction is started"
            1 * originalStatement.execute() >> true
            1 * connectionListener.onTransactionBegin(_)

        when: "setting autocommit to false"
            connection.setAutoCommit(false)
        then: "transaction is not started again"
            0 * connectionListener.onTransactionBegin(_)

        when: "the same statement is executed"
            statement.execute()
        then: "transaction is not started again"
            0 * originalConnection.getAutoCommit()
            0 * connectionListener.onTransactionBegin(_)

        when: "a new statement is created"
            def statement2 = connection.prepareStatement("SELECT 2")
        then: "transaction is not started again"
            1 * originalConnection.prepareStatement("SELECT 2") >> originalStatement2
            0 * connectionListener.onTransactionBegin(_)

        when: "the new statement is executed"
            statement2.execute()
        then: "transaction is not started again"
            0 * originalStatement2.getConnection()
            0 * originalConnection.getAutoCommit()
            0 * connectionListener.onTransactionBegin(_)
    }

    def "when inside transaction onStatementExecute reports we are inside transaction"() {
        given: "a connection with autoCommit = false"
            setupSpyql()
            def originalStatement2 = Mock(PreparedStatement)

        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> originalConnection
        and:
            1 * listener.onGetConnection({ GetConnectionEvent success -> success.executionTimeNs > 0 }) >> connectionListener

        when: "the first statement is executed"
            def statement = connection.prepareStatement("SELECT 1")
            statement.execute()
        then: "transaction is started"
            1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement
            1 * originalConnection.getAutoCommit() >> false
            1 * originalStatement.execute() >> true
            1 * connectionListener.onTransactionBegin(_)
        and: "SpyqlTransactionListener.onStatementExecute is called"
            1 * connectionListener.onStatementExecute({ StatementExecuteEvent result ->
                result.sql == "SELECT 1" && result.transactionId != null && result.executionTimeNs > 0
            })
        when: "the same statement is executed"
            statement.execute()
        then: "SpyqlTransactionListener.onStatementExecute is called"
            1 * connectionListener.onStatementExecute({ it -> it.transactionId != null })
        and: "onTransactionBegin is not called"
            0 * connectionListener.onTransactionBegin(_)

        when: "a new statement is created"
            def statement2 = connection.prepareStatement("SELECT 2")
        then: "statement is prepared"
            1 * originalConnection.prepareStatement("SELECT 2") >> originalStatement2
        when: "the new statement is executed"
            statement2.execute()
        then: "original statement is executed"
            1 * originalStatement2.execute() >> true
        and: "SpyqlTransactionListener.onStatementExecute is called"
            1 * connectionListener.onStatementExecute(_)
    }

    def "when inside transaction commit will call onTransactionCommit"() {
        given: "a connection with autoCommit = false"
            setupSpyql()

        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> originalConnection
        and:
            1 * listener.onGetConnection({ GetConnectionEvent success -> success.executionTimeNs > 0 }) >> connectionListener

        when: "the first statement is prepared"
            def statement = connection.prepareStatement("SELECT 1")
        then: "original prepareStatement is called"
            1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement

        when: "the statement is executed"
            statement.execute()
        then: "checks if transaction is required"
            1 * originalConnection.getAutoCommit() >> false
        and: "transaction is started"
            1 * connectionListener.onTransactionBegin({ TransactionBeginEvent event ->
                !event.emptyTransaction
            })
        and: "original execute is called"
            1 * originalStatement.execute() >> true

        when:
            connection.commit()
        then:
            1 * originalConnection.commit()
        and:
            1 * connectionListener.onTransactionCommit({ TransactionCommitEvent result ->
                result.executionTimeNs > 0
            })
            1 * connectionListener.onEvent(_)
        and:
            0 * _
    }

    def "after commit transaction is closed"() {
        given: "a connection with autoCommit = false"
            setupSpyql()

        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> originalConnection
        and:
            1 * listener.onGetConnection({ GetConnectionEvent success -> success.executionTimeNs > 0 }) >> connectionListener
        when: "the first statement is prepared"
            def statement = connection.prepareStatement("SELECT 1")
        then: "original prepareStatement is called"
            1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement

        when: "the statement is executed"
            statement.execute()
        then: "checks if transaction is required"
            1 * originalConnection.getAutoCommit() >> false
        and: "transaction is started"
            1 * connectionListener.onTransactionBegin(_)
        and: "original execute is called"
            1 * originalStatement.execute() >> true

        when:
            connection.commit()
        then:
            1 * originalConnection.commit()
        and:
            1 * connectionListener.onTransactionCommit(_)

        when: "the statement executed again"
            statement.execute()
        then: "checks if transaction is required"
            1 * originalConnection.getAutoCommit() >> true
            1 * originalStatement.execute() >> true
        and: "calls onStatementExecute outside of transaction"
            1 * connectionListener.onStatementExecute({ StatementExecuteEvent result ->
                result.transactionId == null
            })
            1 * connectionListener.onEvent(_)
        and:
            0 * _
    }

    def "when inside transaction rollback will call onTransactionRollback"() {
        given: "a connection with autoCommit = false"
            setupSpyql()

        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> originalConnection
        and:
            1 * listener.onGetConnection({ GetConnectionEvent success -> success.executionTimeNs > 0 }) >> connectionListener

        when: "the first statement is prepared"
            def statement = connection.prepareStatement("SELECT 1")
        then: "original prepareStatement is called"
            1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement

        when: "the statement is executed"
            statement.execute()
        then: "checks if transaction is required"
            1 * originalConnection.getAutoCommit() >> false
        and: "transaction is started"
            1 * connectionListener.onTransactionBegin(_)
        and: "original execute is called"
            1 * originalStatement.execute() >> true

        when:
            connection.rollback()
        then:
            1 * originalConnection.rollback()
        and:
            1 * connectionListener.onTransactionRollback(_)
            1 * connectionListener.onEvent(_)
        and:
            0 * _
    }

    def "after rollback transaction is closed"() {
        given: "a connection with autoCommit = false"
            setupSpyql()

        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> originalConnection
        and:
            1 * listener.onGetConnection({ GetConnectionEvent success -> success.executionTimeNs > 0 }) >> connectionListener

        when: "the first statement is prepared"
            def statement = connection.prepareStatement("SELECT 1")
        then: "original prepareStatement is called"
            1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement

        when: "the statement is executed"
            statement.execute()
        then: "checks if transaction is required"
            1 * originalConnection.getAutoCommit() >> false
        and: "transaction is started"
            1 * connectionListener.onTransactionBegin(_)
        and: "original execute is called"
            1 * originalStatement.execute() >> true

        when:
            connection.rollback()
        then:
            1 * originalConnection.rollback()
        and:
            1 * connectionListener.onTransactionRollback(_)

        when: "the statement executed again"
            statement.execute()
        then: "checks if transaction is required"
            1 * originalConnection.getAutoCommit() >> true
            1 * originalStatement.execute() >> true
        and: "calls onStatementExecute outside of transaction"
            1 * connectionListener.onStatementExecute({ StatementExecuteEvent result ->
                result.transactionId == null
            })
    }

    def "when SpyqlListener.onStatementExecute throws exception is not propagated"() {
        given:
            setupSpyql()

        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> originalConnection
        and:
            1 * listener.onGetConnection({ GetConnectionEvent success -> success.executionTimeNs > 0 }) >> connectionListener

        when: "the first statement is prepared"
            def statement = connection.prepareStatement("SELECT 1")
        then: "original prepareStatement is called"
            1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement

        when: "the statement is executed"
            statement.execute()
        then: "checks if transaction is required"
            1 * originalConnection.getAutoCommit() >> true
        and: "original execute is called"
            1 * originalStatement.execute() >> true
        and:
            1 * connectionListener.onStatementExecute(_) >> {
                throw new RuntimeException("Foo Bar")
            }
        and:
            0 * _
    }

    def "when SpyqlconnectionListener.onTransactionBegin throws exception is not propagated"() {
        given:
            setupSpyql()

        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> originalConnection
        and:
            1 * listener.onGetConnection({ GetConnectionEvent success -> success.executionTimeNs > 0 }) >> connectionListener

        when: "the first statement is prepared"
            def statement = connection.prepareStatement("SELECT 1")
        then: "original prepareStatement is called"
            1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement

        when: "the statement is executed"
            statement.execute()
        then: "exception is ignored and original statement is still executed"
            1 * originalStatement.execute() >> true
        and: "checks if transaction is required"
            1 * originalConnection.getAutoCommit() >> false
        and: "calls onTransactionBegin which throws exception"
            1 * connectionListener.onTransactionBegin(_) >> {
                throw new RuntimeException("Foo Bar")
            }
        and:
            1 * connectionListener.onStatementExecute(_)
            1 * connectionListener.onEvent(_)
        and: "and nothing else"
            0 * _
    }

    def "when SpyqlTransactionListener.onStatementExecute throws exception is not propagated"() {
        given:
            setupSpyql()

        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> originalConnection
        and:
            1 * listener.onGetConnection({ GetConnectionEvent success -> success.executionTimeNs > 0 }) >> connectionListener

        when: "the first statement is prepared"
            def statement = connection.prepareStatement("SELECT 1")
        then: "original prepareStatement is called"
            1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement

        when: "the statement is executed"
            statement.execute()
        then: "checks if transaction is required"
            1 * originalConnection.getAutoCommit() >> false
        and:
            1 * connectionListener.onTransactionBegin(_)
        and: "original execute is called"
            1 * originalStatement.execute() >> true
        and:
            1 * connectionListener.onStatementExecute(_) >> {
                throw new RuntimeException("Foo Bar")
            }
            1 * connectionListener.onEvent(_)
        and:
            0 * _
    }

    def "when transaction is committed and onTransactionCommit throws exception is not propagated"() {
        given:
            setupSpyql()

        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> originalConnection
        and:
            1 * listener.onGetConnection({ GetConnectionEvent success -> success.executionTimeNs > 0 }) >> connectionListener

        when: "the first statement is prepared"
            def statement = connection.prepareStatement("SELECT 1")
        then: "original prepareStatement is called"
            1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement

        when: "the statement is executed"
            statement.execute()
        then: "checks if transaction is required"
            1 * originalConnection.getAutoCommit() >> false
        and: "transaction is started"
            1 * connectionListener.onTransactionBegin(_)
        and: "original execute is called"
            1 * originalStatement.execute() >> true
        and:
            1 * connectionListener.onStatementExecute(_)

        when:
            connection.commit()
        then:
            1 * originalConnection.commit()
        and:
            1 * connectionListener.onTransactionCommit(_) >> {
                throw new RuntimeException("Foo Bar")
            }
        and:
            0 * _
    }

    def "when transaction is rolled back and onTransactionRollback throws exception is not propagated"() {
        given:
            setupSpyql()

        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> originalConnection
        and:
            1 * listener.onGetConnection({ GetConnectionEvent success -> success.executionTimeNs > 0 }) >> connectionListener

        when: "the first statement is prepared"
            def statement = connection.prepareStatement("SELECT 1")
        then: "original prepareStatement is called"
            1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement

        when: "the statement is executed"
            statement.execute()
        then: "checks if transaction is required"
            1 * originalConnection.getAutoCommit() >> false
        and: "transaction is started"
            1 * connectionListener.onTransactionBegin(_)
        and: "original execute is called"
            1 * originalStatement.execute() >> true
        and:
            1 * connectionListener.onStatementExecute(_)

        when:
            connection.rollback()
        then:
            1 * originalConnection.rollback()
        and:
            1 * connectionListener.onTransactionRollback(_) >> {
                throw new RuntimeException("Foo Bar")
            }
        and:
            0 * _
    }

    // PROXY tests:

    def "proxy without listener doesn't wrap connections"() {
        given:
            setupSpyql(false)

        when:
            def connection = spyqlDataSource.getConnection()

        then:
            1 * originalDataSource.getConnection() >> Mock(Connection)
            !(connection instanceof SpyqlConnection)

        when:
            connection = spyqlDataSource.getConnection("username", "password")

        then:
            1 * originalDataSource.getConnection("username", "password") >> Mock(Connection)
            !(connection instanceof SpyqlConnection)
    }

    def "if data source listener returns null connection is not wrapped"() {
        given:
            setupSpyql()

        when:
            def connection = spyqlDataSource.getConnection()

        then:
            1 * originalDataSource.getConnection() >> null
        and:
            1 * listener.onGetConnectionFailure({ GetConnectionFailureEvent result -> result.executionTimeNs > 0 }) >> null
        and:
            !(connection instanceof SpyqlConnection)

        when:
            connection = spyqlDataSource.getConnection("username", "password")

        then:
            1 * originalDataSource.getConnection("username", "password") >> null
        and:
            1 * listener.onGetConnectionFailure({ GetConnectionFailureEvent result -> result.executionTimeNs > 0 }) >> null
        and:
            !(connection instanceof SpyqlConnection)
    }

    def "createStatement is proxied"() {
        given:
            setupSpyql()

        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> originalConnection
        and:
            1 * listener.onGetConnection({ GetConnectionEvent success -> success.executionTimeNs > 0 }) >> connectionListener
        and:
            connection instanceof SpyqlConnection

        when:
            connection.createStatement()
        then:
            1 * originalConnection.createStatement() >> Mock(Statement)
            connection instanceof SpyqlConnection
    }

    def "prepareStatement is proxied"() {
        given:
            setupSpyql()

        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> originalConnection
        and:
            1 * listener.onGetConnection({ GetConnectionEvent success -> success.executionTimeNs > 0 }) >> connectionListener
        and:
            connection instanceof SpyqlConnection

        when:
            def statement = connection.prepareStatement("SELECT 1")
        then:
            1 * originalConnection.prepareStatement("SELECT 1") >> Mock(PreparedStatement)
            statement instanceof SpyqlStatement
    }

    def "prepareCall is proxied"() {
        given:
            setupSpyql()

        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> originalConnection
        and:
            1 * listener.onGetConnection({ GetConnectionEvent success -> success.executionTimeNs > 0 }) >> connectionListener
        and:
            connection instanceof SpyqlConnection

        when:
            def statement = connection.prepareCall("CALL foo()")
        then:
            1 * originalConnection.prepareCall("CALL foo()") >> Mock(CallableStatement)
            statement instanceof SpyqlStatement
    }

    def "connection level failures are correctly handled"() {
        given:
            setupSpyql()
        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> originalConnection
        and:
            1 * listener.onGetConnection({ _ }) >> connectionListener
        when:
            def statement = connection.prepareStatement("SELECT 1")
        then:
            1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement
        when: 'Query gets executed and fails'
            statement.executeQuery()
        then:
            1 * originalStatement.executeQuery() >> { throw new RuntimeException("You shall not pass!") }
        and:
            1 * connectionListener.onStatementExecuteFailure({ StatementExecuteFailureEvent event ->
                event.transactionId == null && event.executionTimeNs > 0 && event.throwable.message == "You shall not pass!"
            })
            0 * connectionListener.onTransactionBegin(_)
        and:
            thrown(RuntimeException)
        when: 'Query gets executed and transaction gets opened'
            statement.executeQuery()
        then:
            1 * originalStatement.executeQuery()
        when: 'Commit fails'
            connection.commit()
        then:
            1 * originalConnection.commit() >> { throw new RuntimeException("You shall not!") }
        and:
            1 * connectionListener.onTransactionCommitFailure({ TransactionCommitFailureEvent event ->
                event.transactionId && event.throwable.message == "You shall not!" && event.executionTimeNs > 0 && event.connectionId
            })
        and:
            thrown(RuntimeException)
        when: 'Rollback fails'
            connection.rollback()
        then:
            1 * originalConnection.rollback() >> { throw new RuntimeException("Massive failure") }
        and:
            1 * connectionListener.onTransactionRollbackFailure({ TransactionRollbackFailureEvent event ->
                event.transactionId && event.throwable.message == "Massive failure" && event.executionTimeNs > 0 && event.connectionId
            })
        and:
            thrown(RuntimeException)
        when: 'Implicit transaction on auto commit fails'
            connection.setAutoCommit(true)
        then:
            1 * originalConnection.getAutoCommit() >> false
            1 * originalConnection.setAutoCommit(true) >> { throw new RuntimeException("Power outage") }
        and:
            thrown(RuntimeException)
        and:
            1 * connectionListener.onTransactionCommitFailure({ TransactionCommitFailureEvent event ->
                event.transactionId && event.connectionId && event.executionTimeNs > 0 && event.throwable.message == "Power outage"
            })
        when: 'Connection close fails'
            connection.close()
        then:
            1 * originalConnection.close() >> { throw new RuntimeException("No electricity") }
        and:
            1 * connectionListener.onConnectionCloseFailure({ ConnectionCloseFailureEvent event ->
                event.transactionId && event.throwable.message == "No electricity" && event.executionTimeNs > 0 && event.connectionId
            })
        and:
            thrown(RuntimeException)
    }

    def "auto commit switch can trigger a commit of a transaction"() {
        given:
            setupSpyql()
        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> originalConnection
        and:
            1 * listener.onGetConnection({ _ }) >> connectionListener
        when: 'auto commit is not changed'
            connection.setAutoCommit(true)
        then:
            1 * originalConnection.getAutoCommit() >> true
        and:
            0 * connectionListener.onTransactionCommit(_)
        when: 'auto commit is turned on, but there is no ongoing transaction'
            connection.setAutoCommit(true)
        then:
            1 * originalConnection.getAutoCommit() >> false
        and:
            0 * connectionListener.onTransactionCommit(_)
        when: 'auto commit is turned off'
            connection.setAutoCommit(false)
        then:
            0 * originalConnection.getAutoCommit()
        and:
            0 * connectionListener.onTransactionCommit(_)
        when: 'transaction is started'
            connection.createStatement().executeQuery("SELECT 1")
        then:
            1 * originalConnection.getAutoCommit() >> false
            1 * originalConnection.createStatement() >> originalStatement
        when: 'auto commit is turned on while there is ongoing transaction'
            connection.setAutoCommit(true)
        then:
            1 * originalConnection.getAutoCommit() >> false
        and:
            1 * connectionListener.onTransactionCommit(_)
    }

    def "faulty data source listeners will not break application"() {
        given:
            setupSpyql()
        when:
            def connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> originalConnection
        and:
            1 * listener.onGetConnection(_) >> { throw new RuntimeException("Fail #1") }
        and:
            connection
            !(connection instanceof SpyqlConnection)
        when:
            connection = spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> null
        and:
            1 * listener.onGetConnectionFailure(_) >> { throw new RuntimeException("Fail #2") }
        when:
            spyqlDataSource.getConnection()
        then:
            1 * originalDataSource.getConnection() >> { throw new SQLException("Original Fail") }
        and:
            1 * listener.onGetConnectionFailure(_) >> { throw new RuntimeException("Fail #4") }
        and:
            def e = thrown(SQLException)
            e.message == "Original Fail"
    }

    def "error spam is averted on datasource events"() {
        given:
            setupSpyql()
        when: 'error spam is created'
            110.times { spyqlDataSource.getConnection() }
        then:
            110 * originalDataSource.getConnection() >> originalConnection
        and:
            110 * listener.onGetConnection(_) >> { throw new RuntimeException("General fail") }
        and: 'errors are throttled'
            100 * spyqlDataSource.log.error(_, _)
        when: 'error spam is created'
            110.times { spyqlDataSource.getConnection() }
        then:
            110 * originalDataSource.getConnection() >> null
        and:
            110 * listener.onGetConnectionFailure(_) >> { throw new RuntimeException("General fail") }
        and: 'errors are throttled'
            0 * spyqlDataSource.log.error(_, _)
        when: 'error spam is created'
            110.times { spyqlDataSource.getConnection() }
        then:
            1 * originalDataSource.getConnection() >> { throw new RuntimeException("General fail") }
        and:
            1 * listener.onGetConnectionFailure(_) >> { throw new RuntimeException("General fail") }
        and: 'errors are throttled'
            0 * spyqlDataSource.log.error(_, _)
        then:
            thrown(RuntimeException)
    }
}
