package com.transferwise.spyql

import spock.lang.Ignore
import spock.lang.Specification

import javax.sql.DataSource
import java.lang.reflect.Proxy
import java.sql.CallableStatement
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement

class SpyqlDataSourceProxyTest extends Specification {
	DataSource dataSourceMock
	SpyqlDataSourceListener listener
	SpyqlConnectionListener connectionListener
	SpyqlDataSourceProxy proxy
	Connection originalConnection
	PreparedStatement originalStatement
	SpyqlTransactionListener transactionListener

	def setupSpyql(boolean attachListener = true) {
		dataSourceMock = Mock(DataSource)
		if (attachListener) {
			listener = Mock(SpyqlDataSourceListener)
			connectionListener = Mock(SpyqlConnectionListener)
			proxy = new SpyqlDataSourceProxy(dataSourceMock, listener)
			originalConnection = Mock(Connection)
			originalStatement = Mock(PreparedStatement)
			transactionListener = Mock(SpyqlTransactionListener)
		} else {
			proxy = new SpyqlDataSourceProxy(dataSourceMock)
		}
	}

	def "getConnection calls original getConnection when no listener attached"() {
		given:
		setupSpyql(false)

		when:
		proxy.getConnection()

		then:
		1 * dataSourceMock.getConnection() >> Mock(Connection)
	}

	def "getConnection with username and password calls original getConnection when no listener attached"() {
		given:
		setupSpyql(false)

		when:
		proxy.getConnection("username", "password")

		then:
		1 * dataSourceMock.getConnection("username", "password") >> Mock(Connection)
	}

	def "onGetConnection is called when connection is acquired using getConnection"() {
		given:
		setupSpyql()

		when:
		proxy.getConnection()

		then:
		1 * dataSourceMock.getConnection() >> Mock(Connection)
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0})
	}

	def "onGetConnection is called when connection is acquired using getConnection with username and password"() {
		given:
		setupSpyql()

		when:
		proxy.getConnection("username", "password")

		then:
		1 * dataSourceMock.getConnection("username", "password") >> Mock(Connection)
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0})
	}

	def "if target data source returns null when getConnection is called then proxy also returns null"() {
		given:
		setupSpyql()

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> null
		and:
		1 * listener.onGetConnection({GetConnectionNull result ->
			result.getExecutionTimeNs() > 0
		}) >> null
		and:
		connection == null
	}

	def "if target data source throws when getConnection is called then proxy also throws"() {
		given:
		setupSpyql()

		when:
		proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> {
			throw new SQLException("foo")
		}
		and:
		1 * listener.onGetConnection({GetConnectionException result ->
			result.getExecutionTimeNs() > 0
			result.exceptionName == SQLException.name
			result.exceptionMessage == "foo"
		}) >> null
		and:
		def ex = thrown(SQLException)
		ex.message == "foo"
	}

	def "onClose is called when connection is closed"() {
		given:
		setupSpyql()

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> connectionListener

		when:
		connection.close()
		then:
		1 * connectionListener.onClose()
	}

	def "when outside transaction commit does nothing"() {
		given:
		setupSpyql()

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> connectionListener

		when:
		connection.commit()
		then:
		1 * originalConnection.commit()
		0 * connectionListener.onStatementExecute(_, _)
		0 * connectionListener.onTransactionBegin(_)
	}

	def "when outside transaction rollback does nothing"() {
		given:
		setupSpyql()

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> connectionListener

		when:
		connection.rollback()
		then:
		1 * originalConnection.rollback()
		0 * connectionListener.onStatementExecute(_, _)
		0 * connectionListener.onTransactionBegin(_)
	}

	def "when outside transaction SpyqlListener.onStatementExecute is called when statement is executed"() {
		given:
		setupSpyql()

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> connectionListener

		when:
		def statement = connection.prepareStatement("SELECT 1")
		then:
		1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement
		0 * connectionListener.onTransactionBegin(_)

		when:
		statement.execute()
		then:
		1 * originalStatement.getConnection() >> originalConnection
		1 * originalConnection.getAutoCommit() >> true
		1 * originalStatement.execute() >> true
		0 * connectionListener.onTransactionBegin(_)
		1 * connectionListener.onStatementExecute("SELECT 1", {it  > 0})
	}

	def "when outside transaction SpyqlconnectionListener.onTransactionBegin is called when transaction is started"() {
		given:
		setupSpyql()

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> connectionListener

		when:
		def statement = connection.prepareStatement("SELECT 1")
		then:
		1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement
		0 * connectionListener.onTransactionBegin(_)

		when:
		statement.execute()
		then:
		1 * originalStatement.getConnection() >> originalConnection
		1 * originalConnection.getAutoCommit() >> false
		1 * originalStatement.execute() >> true
		1 * connectionListener.onTransactionBegin(_) >> transactionListener
	}

	def "when inside transaction SpyqlconnectionListener.onTransactionBegin is not called again"() {
		given:"a connection with autoCommit = false"
		setupSpyql()
		originalConnection.getAutoCommit() >> false
		def originalStatement2 = Mock(PreparedStatement)

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> connectionListener

		when:"the first statement is prepared"
		def statement = connection.prepareStatement("SELECT 1")
		then:"transaction is not started"
		1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement
		0 * connectionListener.onTransactionBegin(_)

		when:"the first statement is executed"
		statement.execute()
		then:"transaction is started"
		1 * originalStatement.getConnection() >> originalConnection
		1 * originalStatement.execute() >> true
		1 * connectionListener.onTransactionBegin(_) >> transactionListener

		when:"setting autocommit to false"
		connection.setAutoCommit(false)
		then:"transaction is not started again"
		0 * connectionListener.onTransactionBegin(_)

		when:"the same statement is executed"
		statement.execute()
		then:"transaction is not started again"
		0 * originalStatement.getConnection()
		0 * originalConnection.getAutoCommit()
		0 * connectionListener.onTransactionBegin(_)

		when:"a new statement is created"
		def statement2 = connection.prepareStatement("SELECT 2")
		then:"transaction is not started again"
		1 * originalConnection.prepareStatement("SELECT 2") >> originalStatement2
		0 * connectionListener.onTransactionBegin(_)

		when:"the new statement is executed"
		statement2.execute()
		then:"transaction is not started again"
		0 * originalStatement2.getConnection()
		0 * originalConnection.getAutoCommit()
		0 * connectionListener.onTransactionBegin(_)
	}

	def "when SpyqlconnectionListener.onTransactionBegin returns null statement executions are not reported anywhere"() {
		given:"a connection with autoCommit = false"
		setupSpyql()
		originalConnection.getAutoCommit() >> false

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> connectionListener

		when:"the first statement is executed"
		def statement = connection.prepareStatement("SELECT 1")
		statement.execute()
		then:"transaction is started"
		1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement
		1 * originalStatement.getConnection() >> originalConnection
		1 * originalStatement.execute() >> true
		1 * connectionListener.onTransactionBegin(_) >> null
		and:
		0 * connectionListener.onStatementExecute(_, _)
	}

	def "when inside transaction setAutoCommit(true) commits the transaction"() {
		// TODO
	}

	def "when inside transaction SpyqlTransactionListener.onStatementExecute is called when statement is executed"() {
		given:"a connection with autoCommit = false"
		setupSpyql()
		def originalStatement2 = Mock(PreparedStatement)

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> connectionListener

		when:"the first statement is executed"
		def statement = connection.prepareStatement("SELECT 1")
		statement.execute()
		then:"transaction is started"
		1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement
		1 * originalStatement.getConnection() >> originalConnection
		1 * originalConnection.getAutoCommit() >> false
		1 * originalStatement.execute() >> true
		1 * connectionListener.onTransactionBegin(_) >> transactionListener
		and:"SpyqlTransactionListener.onStatementExecute is called"
		1 * transactionListener.onStatementExecute("SELECT 1", {it  > 0})
		and:"SpyqlListener.onStatementExecute is not called"
		0 * connectionListener.onStatementExecute(_, _)

		when:"the same statement is executed"
		statement.execute()
		then:"SpyqlTransactionListener.onStatementExecute is called"
		1 * transactionListener.onStatementExecute("SELECT 1", {it  > 0})
		and:"SpyqlListener.onStatementExecute is not called"
		0 * connectionListener.onStatementExecute(_, _)

		when:"a new statement is created"
		def statement2 = connection.prepareStatement("SELECT 2")
		then:"statement is prepared"
		1 * originalConnection.prepareStatement("SELECT 2") >> originalStatement2

		when:"the new statement is executed"
		statement2.execute()
		then:"original statement is executed"
		1 * originalStatement2.execute() >> true
		and:"SpyqlTransactionListener.onStatementExecute is called"
		1 * transactionListener.onStatementExecute("SELECT 2", {it  > 0})
		and:"SpyqlListener.onStatementExecute is not called"
		0 * connectionListener.onStatementExecute(_, _)
	}

	def "when inside transaction commit will call onTransactionCommit and onTransactionComplete"() {
		given:"a connection with autoCommit = false"
		setupSpyql()

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> connectionListener

		when:"the first statement is prepared"
		def statement = connection.prepareStatement("SELECT 1")
		then:"original prepareStatement is called"
		1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement

		when:"the statement is executed"
		statement.execute()
		then:"checks if transaction is required"
		1 * originalStatement.getConnection() >> originalConnection
		1 * originalConnection.getAutoCommit() >> false
		and:"transaction is started"
		1 * connectionListener.onTransactionBegin(_) >> transactionListener
		and:"original execute is called"
		1 * originalStatement.execute() >> true

		when:
		connection.commit()
		then:
		1 * originalConnection.commit()
		and:
		1 * transactionListener.onTransactionCommit({it > 0})
		and:
		0 * _
	}

	def "after commit transaction is closed"() {
		given:"a connection with autoCommit = false"
		setupSpyql()

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> connectionListener

		when:"the first statement is prepared"
		def statement = connection.prepareStatement("SELECT 1")
		then:"original prepareStatement is called"
		1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement

		when:"the statement is executed"
		statement.execute()
		then:"checks if transaction is required"
		1 * originalStatement.getConnection() >> originalConnection
		1 * originalConnection.getAutoCommit() >> false
		and:"transaction is started"
		1 * connectionListener.onTransactionBegin(_) >> transactionListener
		and:"original execute is called"
		1 * originalStatement.execute() >> true

		when:
		connection.commit()
		then:
		1 * originalConnection.commit()
		and:
		1 * transactionListener.onTransactionCommit({it > 0})

		when:"the statement executed again"
		statement.execute()
		then:"checks if transaction is required"
		1 * originalStatement.getConnection() >> originalConnection
		1 * originalConnection.getAutoCommit() >> true
		1 * originalStatement.execute() >> true
		and:"calls onStatementExecute outside of transaction"
		1 * connectionListener.onStatementExecute("SELECT 1", {it > 0})
		and:
		0 * _
	}

	def "when inside transaction rollback will call onTransactionRollback and onTransactionComplete"() {
		given:"a connection with autoCommit = false"
		setupSpyql()

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> connectionListener

		when:"the first statement is prepared"
		def statement = connection.prepareStatement("SELECT 1")
		then:"original prepareStatement is called"
		1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement

		when:"the statement is executed"
		statement.execute()
		then:"checks if transaction is required"
		1 * originalStatement.getConnection() >> originalConnection
		1 * originalConnection.getAutoCommit() >> false
		and:"transaction is started"
		1 * connectionListener.onTransactionBegin(_) >> transactionListener
		and:"original execute is called"
		1 * originalStatement.execute() >> true

		when:
		connection.rollback()
		then:
		1 * originalConnection.rollback()
		and:
		1 * transactionListener.onTransactionRollback({it > 0})
		and:
		0 * _
	}

	def "after rollback transaction is closed"() {
		given:"a connection with autoCommit = false"
		setupSpyql()

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> connectionListener

		when:"the first statement is prepared"
		def statement = connection.prepareStatement("SELECT 1")
		then:"original prepareStatement is called"
		1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement

		when:"the statement is executed"
		statement.execute()
		then:"checks if transaction is required"
		1 * originalStatement.getConnection() >> originalConnection
		1 * originalConnection.getAutoCommit() >> false
		and:"transaction is started"
		1 * connectionListener.onTransactionBegin(_) >> transactionListener
		and:"original execute is called"
		1 * originalStatement.execute() >> true

		when:
		connection.rollback()
		then:
		1 * originalConnection.rollback()
		and:
		1 * transactionListener.onTransactionRollback({it > 0})

		when:"the statement executed again"
		statement.execute()
		then:"checks if transaction is required"
		1 * originalStatement.getConnection() >> originalConnection
		1 * originalConnection.getAutoCommit() >> true
		1 * originalStatement.execute() >> true
		and:"calls onStatementExecute outside of transaction"
		1 * connectionListener.onStatementExecute("SELECT 1", {it > 0})
		and:
		0 * _
	}

	def "when SpyqlListener.onStatementExecute throws exception is not propagated"() {
		given:
		setupSpyql()

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> connectionListener

		when:"the first statement is prepared"
		def statement = connection.prepareStatement("SELECT 1")
		then:"original prepareStatement is called"
		1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement

		when:"the statement is executed"
		statement.execute()
		then:"checks if transaction is required"
		1 * originalStatement.getConnection() >> originalConnection
		1 * originalConnection.getAutoCommit() >> true
		and:"original execute is called"
		1 * originalStatement.execute() >> true
		and:
		1 * connectionListener.onStatementExecute("SELECT 1", {it > 0}) >> {
			throw new Exception("Foo Bar")
		}
		and:
		0 * _
	}

	def "when SpyqlconnectionListener.onTransactionBegin throws exception is not propagated"() {
		given:
		setupSpyql()

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> connectionListener

		when:"the first statement is prepared"
		def statement = connection.prepareStatement("SELECT 1")
		then:"original prepareStatement is called"
		1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement

		when:"the statement is executed"
		statement.execute()
		then:"checks if transaction is required"
		1 * originalStatement.getConnection() >> originalConnection
		1 * originalConnection.getAutoCommit() >> false
		and:"calls onTransactionBegin which throws exception"
		1 * connectionListener.onTransactionBegin(_) >> {
			throw new Exception("Foo Bar")
		}
		then:"exception is ignored and original statement is still executed"
		1 * originalStatement.execute() >> true
		and:"and SpyqlTransactionListener.onStatementExecute is not called as well as nothing else"
		0 * _
	}

	def "when SpyqlTransactionListener.onStatementExecute throws exception is not propagated"() {
		given:
		setupSpyql()

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> connectionListener

		when:"the first statement is prepared"
		def statement = connection.prepareStatement("SELECT 1")
		then:"original prepareStatement is called"
		1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement

		when:"the statement is executed"
		statement.execute()
		then:"checks if transaction is required"
		1 * originalStatement.getConnection() >> originalConnection
		1 * originalConnection.getAutoCommit() >> false
		and:
		1 * connectionListener.onTransactionBegin(_) >> transactionListener
		and:"original execute is called"
		1 * originalStatement.execute() >> true
		and:
		1 * transactionListener.onStatementExecute("SELECT 1", {it > 0}) >> {
			throw new Exception("Foo Bar")
		}
		and:
		0 * _
	}

	def "when transaction is committed and onTransactionCommit throws exception is not propagated"() {
		given:
		setupSpyql()

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> connectionListener

		when:"the first statement is prepared"
		def statement = connection.prepareStatement("SELECT 1")
		then:"original prepareStatement is called"
		1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement

		when:"the statement is executed"
		statement.execute()
		then:"checks if transaction is required"
		1 * originalStatement.getConnection() >> originalConnection
		1 * originalConnection.getAutoCommit() >> false
		and:"transaction is started"
		1 * connectionListener.onTransactionBegin(_) >> transactionListener
		and:"original execute is called"
		1 * originalStatement.execute() >> true
		and:
		1 * transactionListener.onStatementExecute("SELECT 1", {it > 0})

		when:
		connection.commit()
		then:
		1 * originalConnection.commit()
		and:
		1 * transactionListener.onTransactionCommit({it > 0}) >> {
			throw new Exception("Foo Bar")
		}
		and:
		0 * _
	}

	def "when transaction is rolled back and onTransactionRollback throws exception is not propagated"() {
		given:
		setupSpyql()

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> connectionListener

		when:"the first statement is prepared"
		def statement = connection.prepareStatement("SELECT 1")
		then:"original prepareStatement is called"
		1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement

		when:"the statement is executed"
		statement.execute()
		then:"checks if transaction is required"
		1 * originalStatement.getConnection() >> originalConnection
		1 * originalConnection.getAutoCommit() >> false
		and:"transaction is started"
		1 * connectionListener.onTransactionBegin(_) >> transactionListener
		and:"original execute is called"
		1 * originalStatement.execute() >> true
		and:
		1 * transactionListener.onStatementExecute("SELECT 1", {it > 0})

		when:
		connection.rollback()
		then:
		1 * originalConnection.rollback()
		and:
		1 * transactionListener.onTransactionRollback({it > 0}) >> {
			throw new Exception("Foo Bar")
		}
		and:
		0 * _
	}

	@Ignore
	def "when execute throws onStatementFailure is called"() {
		given:
		setupSpyql()

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> connectionListener

		when:"the first statement is prepared"
		def statement = connection.prepareStatement("SELECT 1")
		then:"original prepareStatement is called"
		1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement

		when:"the statement is executed"
		statement.execute()
		then:"checks if transaction is required"
		1 * originalStatement.getConnection() >> originalConnection
		1 * originalConnection.getAutoCommit() >> false
		and:"transaction is started"
		1 * connectionListener.onTransactionBegin(_) >> transactionListener
		and:"original execute is called"
		1 * originalStatement.execute() >> {
			throw new SQLException("Foo Bar")
		}
		and:
		1 * transactionListener.onStatementFailure({e -> e instanceof SQLException && e.message == "Foo Bar"}, {it > 0})
		and:
		def e = thrown(SQLException)
		e.message == "Foo Bar"
	}

	@Ignore
	def "when commit throws onTransactionCommitFailure is called"() {
		expect:
		false
	}

	@Ignore
	def "when rollback throws onTransactionRollbackFailure is called"() {
		expect:
		false
	}

	// PROXY tests:

	def "proxy without listener doesn't wrap connections"() {
		given:
		setupSpyql(false)

		when:
		def connection = proxy.getConnection()

		then:
		1 * dataSourceMock.getConnection() >> Mock(Connection)
		!hasInvocationHandler(connection, SpyqlDataSourceProxy.ConnectionInvocationHandler.class)

		when:
		connection = proxy.getConnection("username", "password")

		then:
		1 * dataSourceMock.getConnection("username", "password") >> Mock(Connection)
		!hasInvocationHandler(connection, SpyqlDataSourceProxy.ConnectionInvocationHandler.class)
	}

	def "if data source listener returns null connection is not wrapped"() {
		given:
		setupSpyql()

		when:
		def connection = proxy.getConnection()

		then:
		1 * dataSourceMock.getConnection() >> Mock(Connection)
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> null
		and:
		!hasInvocationHandler(connection, SpyqlDataSourceProxy.ConnectionInvocationHandler.class)

		when:
		connection = proxy.getConnection("username", "password")

		then:
		1 * dataSourceMock.getConnection("username", "password") >> Mock(Connection)
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> null
		and:
		!hasInvocationHandler(connection, SpyqlDataSourceProxy.ConnectionInvocationHandler.class)
	}

	def "createStatement is proxied"() {
		given:
		setupSpyql()

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> connectionListener
		and:
		hasInvocationHandler(connection, SpyqlDataSourceProxy.ConnectionInvocationHandler.class)

		when:
		def statement = connection.createStatement()
		then:
		1 * originalConnection.createStatement() >> Mock(Statement)
		hasInvocationHandler(statement, SpyqlDataSourceProxy.StatementInvocationHandler.class)
	}

	def "prepareStatement is proxied"() {
		given:
		setupSpyql()

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> connectionListener
		and:
		hasInvocationHandler(connection, SpyqlDataSourceProxy.ConnectionInvocationHandler.class)

		when:
		def statement = connection.prepareStatement("SELECT 1")
		then:
		1 * originalConnection.prepareStatement("SELECT 1") >> Mock(PreparedStatement)
		hasInvocationHandler(statement, SpyqlDataSourceProxy.StatementInvocationHandler.class)
	}

	def "prepareCall is proxied"() {
		given:
		setupSpyql()

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection
		and:
		1 * listener.onGetConnection({GetConnectionSuccess success -> success.executionTimeNs > 0}) >> connectionListener
		and:
		hasInvocationHandler(connection, SpyqlDataSourceProxy.ConnectionInvocationHandler.class)

		when:
		def statement = connection.prepareCall("CALL foo()")
		then:
		1 * originalConnection.prepareCall("CALL foo()") >> Mock(CallableStatement)
		hasInvocationHandler(statement, SpyqlDataSourceProxy.StatementInvocationHandler.class)
	}

	private static boolean hasInvocationHandler(Object connection, Class<?> clazz) {
		clazz.isInstance(Proxy.getInvocationHandler(connection))
	}
}
