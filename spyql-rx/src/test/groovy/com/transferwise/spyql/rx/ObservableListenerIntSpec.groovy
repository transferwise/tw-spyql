package com.transferwise.spyql.rx

import com.transferwise.spyql.SpyqlDataSource
import com.transferwise.spyql.SpyqlDataSourceSpec
import com.transferwise.spyql.listener.SpyqlConnectionListener
import com.transferwise.spyql.listener.SpyqlDataSourceListener

import javax.sql.DataSource
import java.sql.Connection
import java.sql.PreparedStatement

class ObservableListenerIntSpec extends SpyqlDataSourceSpec {
    def setupSpyql(boolean attachListener = true) {
        originalDataSource = Mock(DataSource)
        if (attachListener) {
            listener = Mock(SpyqlDataSourceListener)
            connectionListener = Mock(SpyqlConnectionListener)
            def observableListener = new ObservableListener()
            observableListener.attachListener(listener)
            spyqlDataSource = new SpyqlDataSource(originalDataSource, observableListener)
            originalConnection = Mock(Connection)
            originalStatement = Mock(PreparedStatement)
        } else {
            spyqlDataSource = new SpyqlDataSource(originalDataSource)
        }
    }
}
