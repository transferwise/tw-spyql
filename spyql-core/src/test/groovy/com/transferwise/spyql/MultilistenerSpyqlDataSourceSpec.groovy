package com.transferwise.spyql

import com.transferwise.spyql.listener.SpyqlConnectionListener
import com.transferwise.spyql.listener.SpyqlDataSourceListener
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection
import java.sql.Statement

class MultilistenerSpyqlDataSourceSpec extends Specification {
    def "all listeners will receive events"() {
        given:
            SpyqlDataSourceListener listener1 = Mock(SpyqlDataSourceListener)
            SpyqlConnectionListener connectionListener1 = Mock(SpyqlConnectionListener)
            SpyqlDataSourceListener listener2 = Mock(SpyqlDataSourceListener)
            SpyqlConnectionListener connectionListener2 = Mock(SpyqlConnectionListener)

            DataSource dataSource = Mock(DataSource)
            Connection connection = Mock(Connection)
            Statement statement = Mock(Statement)

            SpyqlDataSource spyqlDataSource = new SpyqlDataSource(dataSource)
            spyqlDataSource.addListener(listener1)
            spyqlDataSource.addListener(listener2)

        when:
            def spyqlConnection = spyqlDataSource.getConnection()
        then:
            1 * dataSource.getConnection() >> connection
            1 * listener1.onGetConnection(_) >> connectionListener1
            1 * listener2.onGetConnection(_) >> connectionListener2
        when:
            spyqlConnection.createStatement().executeUpdate("UPDATE SOMETHING")
        then:
            1 * connection.createStatement() >> statement
            1 * connectionListener1.onStatementExecute(_)
            1 * connectionListener2.onStatementExecute(_)
    }
}
