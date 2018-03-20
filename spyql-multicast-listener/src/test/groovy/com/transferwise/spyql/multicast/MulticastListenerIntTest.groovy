package com.transferwise.spyql.multicast

import com.transferwise.spyql.SpyqlDataSourceProxy
import com.transferwise.spyql.SpyqlListener
import com.transferwise.spyql.SpyqlTransactionListener
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection
import java.sql.PreparedStatement

class MulticastListenerIntTest extends Specification {

	def "when outside transaction commit does nothing"() {
		given:
		def dataSourceMock = Mock(DataSource)
		def originalListener = Mock(SpyqlListener)
		def listener = new MulticastListener()
		listener.attachListener(originalListener)
		def proxy = new SpyqlDataSourceProxy(dataSourceMock, listener)
		def originalConnection = Mock(Connection)

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection

		when:
		connection.commit()
		then:
		1 * originalConnection.commit()
		0 * originalListener.onStatementExecute(_, _)
		0 * originalListener.onTransactionBegin(_)
	}

	def "when outside transaction rollback does nothing"() {
		given:
		def dataSourceMock = Mock(DataSource)
		def originalListener = Mock(SpyqlListener)
		def listener = new MulticastListener()
		listener.attachListener(originalListener)
		def proxy = new SpyqlDataSourceProxy(dataSourceMock, listener)
		def originalConnection = Mock(Connection)

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection

		when:
		connection.rollback()
		then:
		1 * originalConnection.rollback()
		0 * listener.onStatementExecute(_, _)
		0 * listener.onTransactionBegin(_)
	}

	def "when outside transaction SpyqlListener.onStatementExecute is called when statement is executed"() {
		given:
		def dataSourceMock = Mock(DataSource)
		def originalListener = Mock(SpyqlListener)
		def listener = new MulticastListener()
		listener.attachListener(originalListener)
		def proxy = new SpyqlDataSourceProxy(dataSourceMock, listener)
		def originalConnection = Mock(Connection)
		def originalStatement = Mock(PreparedStatement)

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection

		when:
		def statement = connection.prepareStatement("SELECT 1")
		then:
		1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement
		0 * originalListener.onTransactionBegin(_)

		when:
		statement.execute()
		then:
		1 * originalStatement.getConnection() >> originalConnection
		1 * originalConnection.getAutoCommit() >> true
		1 * originalStatement.execute() >> true
		0 * originalListener.onTransactionBegin(_)
		1 * originalListener.onStatementExecute("SELECT 1", {it  > 0})
	}

	def "when outside transaction SpyqlListener.onTransactionBegin is called when transaction is started"() {
		given:
		def dataSourceMock = Mock(DataSource)
		def originalListener = Mock(SpyqlListener)
		def listener = new MulticastListener()
		listener.attachListener(originalListener)
		def proxy = new SpyqlDataSourceProxy(dataSourceMock, listener)
		def originalConnection = Mock(Connection)
		def originalStatement = Mock(PreparedStatement)
		def transactionListener = Mock(SpyqlTransactionListener)

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection

		when:
		def statement = connection.prepareStatement("SELECT 1")
		then:
		1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement
		0 * originalListener.onTransactionBegin(_)

		when:
		statement.execute()
		then:
		1 * originalStatement.getConnection() >> originalConnection
		1 * originalConnection.getAutoCommit() >> false
		1 * originalStatement.execute() >> true
		1 * originalListener.onTransactionBegin(_) >> transactionListener
	}

	def "when inside transaction SpyqlListener.onTransactionBegin is not called again"() {
		given:"a connection with autoCommit = false"
		def dataSourceMock = Mock(DataSource)
		def originalListener = Mock(SpyqlListener)
		def listener = new MulticastListener()
		listener.attachListener(originalListener)
		def proxy = new SpyqlDataSourceProxy(dataSourceMock, listener)
		def originalConnection = Mock(Connection)
		originalConnection.getAutoCommit() >> false
		def originalStatement = Mock(PreparedStatement)
		def originalStatement2 = Mock(PreparedStatement)
		def transactionListener = Mock(SpyqlTransactionListener)

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection

		when:"the first statement is prepared"
		def statement = connection.prepareStatement("SELECT 1")
		then:"transaction is not started"
		1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement
		0 * originalListener.onTransactionBegin(_)

		when:"the first statement is executed"
		statement.execute()
		then:"transaction is started"
		1 * originalStatement.getConnection() >> originalConnection
		1 * originalStatement.execute() >> true
		1 * originalListener.onTransactionBegin(_) >> transactionListener

		when:"setting autocommit to false"
		connection.setAutoCommit(false)
		then:"transaction is not started again"
		0 * originalListener.onTransactionBegin(_)

		when:"the same statement is executed"
		statement.execute()
		then:"transaction is not started again"
		0 * originalStatement.getConnection()
		0 * originalConnection.getAutoCommit()
		0 * originalListener.onTransactionBegin(_)

		when:"a new statement is created"
		def statement2 = connection.prepareStatement("SELECT 2")
		then:"transaction is not started again"
		1 * originalConnection.prepareStatement("SELECT 2") >> originalStatement2
		0 * originalListener.onTransactionBegin(_)

		when:"the new statement is executed"
		statement2.execute()
		then:"transaction is not started again"
		0 * originalStatement2.getConnection()
		0 * originalConnection.getAutoCommit()
		0 * originalListener.onTransactionBegin(_)
	}

	def "when inside transaction setAutoCommit(true) commits the transaction"() {
		// TODO
	}

	def "when inside transaction SpyqlTransactionListener.onStatementExecute is called when statement is executed"() {
		given:"a connection with autoCommit = false"
		def dataSourceMock = Mock(DataSource)
		def originalListener = Mock(SpyqlListener)
		def listener = new MulticastListener()
		listener.attachListener(originalListener)
		def proxy = new SpyqlDataSourceProxy(dataSourceMock, listener)
		def originalConnection = Mock(Connection)
		def originalStatement = Mock(PreparedStatement)
		def originalStatement2 = Mock(PreparedStatement)
		def transactionListener = Mock(SpyqlTransactionListener)

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection

		when:"the first statement is executed"
		def statement = connection.prepareStatement("SELECT 1")
		statement.execute()
		then:"transaction is started"
		1 * originalConnection.prepareStatement("SELECT 1") >> originalStatement
		1 * originalStatement.getConnection() >> originalConnection
		1 * originalConnection.getAutoCommit() >> false
		1 * originalStatement.execute() >> true
		1 * originalListener.onTransactionBegin(_) >> transactionListener
		and:"SpyqlTransactionListener.onStatementExecute is called"
		1 * transactionListener.onStatementExecute("SELECT 1", {it  > 0})
		and:"SpyqlListener.onStatementExecute is not called"
		0 * originalListener.onStatementExecute(_, _)

		when:"the same statement is executed"
		statement.execute()
		then:"SpyqlTransactionListener.onStatementExecute is called"
		1 * transactionListener.onStatementExecute("SELECT 1", {it  > 0})
		and:"SpyqlListener.onStatementExecute is not called"
		0 * originalListener.onStatementExecute(_, _)

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
		0 * originalListener.onStatementExecute(_, _)
	}

	def "when inside transaction commit will call onTransactionCommit and onTransactionComplete"() {
		given:"a connection with autoCommit = false"
		def dataSourceMock = Mock(DataSource)
		def originalListener = Mock(SpyqlListener)
		def listener = new MulticastListener()
		listener.attachListener(originalListener)
		def proxy = new SpyqlDataSourceProxy(dataSourceMock, listener)
		def originalConnection = Mock(Connection)
		def originalStatement = Mock(PreparedStatement)
		def transactionListener = Mock(SpyqlTransactionListener)

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection

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
		1 * originalListener.onTransactionBegin(_) >> transactionListener
		and:"original execute is called"
		1 * originalStatement.execute() >> true

		when:
		connection.commit()
		then:
		1 * originalConnection.commit()
		and:
		1 * transactionListener.onTransactionCommit({it > 0})
		and:
		1 * transactionListener.onTransactionCommit()
		and:
		1 * transactionListener.onTransactionComplete({it > 0})
		and:
		0 * _
	}

	def "when inside transaction rollback will call onTransactionRollback and onTransactionComplete"() {
		given:"a connection with autoCommit = false"
		def dataSourceMock = Mock(DataSource)
		def originalListener = Mock(SpyqlListener)
		def listener = new MulticastListener()
		listener.attachListener(originalListener)
		def proxy = new SpyqlDataSourceProxy(dataSourceMock, listener)
		def originalConnection = Mock(Connection)
		def originalStatement = Mock(PreparedStatement)
		def transactionListener = Mock(SpyqlTransactionListener)

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection

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
		1 * originalListener.onTransactionBegin(_) >> transactionListener
		and:"original execute is called"
		1 * originalStatement.execute() >> true

		when:
		connection.rollback()
		then:
		1 * originalConnection.rollback()
		and:
		1 * transactionListener.onTransactionRollback({it > 0})
		and:
		1 * transactionListener.onTransactionRollback()
		and:
		1 * transactionListener.onTransactionComplete({it > 0})
		and:
		0 * _
	}

	def "when SpyqlListener.onStatementExecute throws exception is not propagated"() {
		given:
		def dataSourceMock = Mock(DataSource)
		def originalListener = Mock(SpyqlListener)
		def listener = new MulticastListener()
		listener.attachListener(originalListener)
		def proxy = new SpyqlDataSourceProxy(dataSourceMock, listener)
		def originalConnection = Mock(Connection)
		def originalStatement = Mock(PreparedStatement)

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection

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
		1 * originalListener.onStatementExecute("SELECT 1", {it > 0}) >> {
			throw new Exception("Foo Bar")
		}
		and:
		0 * _
	}

	def "when SpyqlListener.onTransactionBegin throws exception is not propagated"() {
		given:
		def dataSourceMock = Mock(DataSource)
		def originalListener = Mock(SpyqlListener)
		def listener = new MulticastListener()
		listener.attachListener(originalListener)
		def proxy = new SpyqlDataSourceProxy(dataSourceMock, listener)
		def originalConnection = Mock(Connection)
		def originalStatement = Mock(PreparedStatement)

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection

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
		1 * originalListener.onTransactionBegin(_) >> {
			throw new Exception("Foo Bar")
		}
		then:"exception is ignored and original statement is still executed"
		1 * originalStatement.execute() >> true
		and:"and SpyqlTransactionListener.onStatementExecute is not called as well as nothing else"
		0 * _
	}

	def "when SpyqlTransactionListener.onStatementExecute throws exception is not propagated"() {
		given:
		def dataSourceMock = Mock(DataSource)
		def originalListener = Mock(SpyqlListener)
		def listener = new MulticastListener()
		listener.attachListener(originalListener)
		def proxy = new SpyqlDataSourceProxy(dataSourceMock, listener)
		def originalConnection = Mock(Connection)
		def originalStatement = Mock(PreparedStatement)
		def transactionListener = Mock(SpyqlTransactionListener)

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection

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
		1 * originalListener.onTransactionBegin(_) >> transactionListener
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
		def dataSourceMock = Mock(DataSource)
		def originalListener = Mock(SpyqlListener)
		def listener = new MulticastListener()
		listener.attachListener(originalListener)
		def proxy = new SpyqlDataSourceProxy(dataSourceMock, listener)
		def originalConnection = Mock(Connection)
		def originalStatement = Mock(PreparedStatement)
		def transactionListener = Mock(SpyqlTransactionListener)

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection

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
		1 * originalListener.onTransactionBegin(_) >> transactionListener
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

	def "when transaction is committed and onTransactionComplete throws exception is not propagated"() {
		given:
		def dataSourceMock = Mock(DataSource)
		def originalListener = Mock(SpyqlListener)
		def listener = new MulticastListener()
		listener.attachListener(originalListener)
		def proxy = new SpyqlDataSourceProxy(dataSourceMock, listener)
		def originalConnection = Mock(Connection)
		def originalStatement = Mock(PreparedStatement)
		def transactionListener = Mock(SpyqlTransactionListener)

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection

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
		1 * originalListener.onTransactionBegin(_) >> transactionListener
		and:"original execute is called"
		1 * originalStatement.execute() >> true
		and:
		1 * transactionListener.onStatementExecute("SELECT 1", {it > 0})

		when:
		connection.commit()
		then:
		1 * originalConnection.commit()
		and:
		1 * transactionListener.onTransactionCommit({it > 0})
		and:
		1 * transactionListener.onTransactionCommit()
		and:
		1 * transactionListener.onTransactionComplete({it > 0}) >> {
			throw new Exception("Foo Bar")
		}
		and:
		0 * _
	}

	def "when transaction is rolled back and onTransactionRollback throws exception is not propagated"() {
		given:
		def dataSourceMock = Mock(DataSource)
		def originalListener = Mock(SpyqlListener)
		def listener = new MulticastListener()
		listener.attachListener(originalListener)
		def proxy = new SpyqlDataSourceProxy(dataSourceMock, listener)
		def originalConnection = Mock(Connection)
		def originalStatement = Mock(PreparedStatement)
		def transactionListener = Mock(SpyqlTransactionListener)

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection

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
		1 * originalListener.onTransactionBegin(_) >> transactionListener
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

	def "when transaction is rolled back and onTransactionComplete throws exception is not propagated"() {
		given:
		def dataSourceMock = Mock(DataSource)
		def originalListener = Mock(SpyqlListener)
		def listener = new MulticastListener()
		listener.attachListener(originalListener)
		def proxy = new SpyqlDataSourceProxy(dataSourceMock, listener)
		def originalConnection = Mock(Connection)
		def originalStatement = Mock(PreparedStatement)
		def transactionListener = Mock(SpyqlTransactionListener)

		when:
		def connection = proxy.getConnection()
		then:
		1 * dataSourceMock.getConnection() >> originalConnection

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
		1 * originalListener.onTransactionBegin(_) >> transactionListener
		and:"original execute is called"
		1 * originalStatement.execute() >> true
		and:
		1 * transactionListener.onStatementExecute("SELECT 1", {it > 0})

		when:
		connection.rollback()
		then:
		1 * originalConnection.rollback()
		and:
		1 * transactionListener.onTransactionRollback({it > 0})
		and:
		1 * transactionListener.onTransactionRollback()
		and:
		1 * transactionListener.onTransactionComplete({it > 0}) >> {
			throw new Exception("Foo Bar")
		}
		and:
		0 * _
	}

}
