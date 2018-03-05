package com.transferewise.spyql;

import com.transferwise.spyql.SpyqlDataSourceProxy;
import com.transferwise.spyql.SpyqlListener;
import com.transferwise.spyql.SpyqlTransactionDefinition;
import com.transferwise.spyql.SpyqlTransactionListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
public class SpyqlDataSourceProxyFooTest {

	@Autowired
	DataSource dataSource;

	@Test
	public void createDataSourceProxy() throws SQLException {
		SpyqlListener listener = new TestListener();
		DataSource proxy = new SpyqlDataSourceProxy(dataSource, listener);
		Connection connection = proxy.getConnection();
		assertThat(connection, is(notNullValue()));
		PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1");
		ResultSet result = preparedStatement.executeQuery();
		result.first();
		assertThat(result.getInt(1), is(equalTo(1)));
	}

	static class TestListener implements SpyqlListener {

		@Override
		public SpyqlTransactionListener onTransactionBegin(SpyqlTransactionDefinition transactionSpy) {
			return null;
		}

		@Override
		public void onStatementExecute(String sql, Long executionTimeNs) {

		}
	}
}
