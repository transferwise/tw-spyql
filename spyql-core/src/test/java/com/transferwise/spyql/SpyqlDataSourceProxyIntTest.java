package com.transferwise.spyql;

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

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
public class SpyqlDataSourceProxyIntTest {

	@Autowired
	DataSource dataSource;

	@Test
	public void createDataSourceProxy() throws SQLException {
		SpyqlDataSourceListener listener = new TestListener();
		DataSource proxy = new SpyqlDataSourceProxy(dataSource, listener);
		Connection connection = proxy.getConnection();
		assertThat(connection, is(notNullValue()));
		PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1");
		ResultSet result = preparedStatement.executeQuery();
		result.first();
		assertThat(result.getInt(1), is(equalTo(1)));
	}

	static class TestListener implements SpyqlDataSourceListener {

		@Override
		public SpyqlConnectionListener onGetConnection(GetConnectionResult result) {
			return new SpyqlConnectionListener() {
				@Override
				public SpyqlTransactionListener onTransactionBegin(SpyqlTransactionDefinition transactionDefinition) {
					return null;
				}

				@Override
				public void onStatementExecute(String sql, Long executionTimeNs) {

				}

				@Override
				public void onClose() {

				}
			};
		}
	}
}
