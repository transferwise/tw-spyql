package com.transferwise.spyql;

import org.springframework.jdbc.datasource.ConnectionProxy;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;

public class SpyqlDataSourceProxy extends DelegatingDataSource {

	private SpyqlListener spyqlListener;

	public SpyqlDataSourceProxy(DataSource targetDataSource, SpyqlListener spyqlListener) {
		super(targetDataSource);
		this.spyqlListener = spyqlListener;
	}

	public SpyqlDataSourceProxy(SpyqlListener spyqlListener) {
		super();
		this.spyqlListener = spyqlListener;
	}

	public SpyqlDataSourceProxy(DataSource targetDataSource) {
		super(targetDataSource);
	}

	public SpyqlDataSourceProxy() {
		super();
	}

	public void setListener(SpyqlListener listener) {
		spyqlListener = listener;
	}

	// TODO: Think about the case when listener removed from a different thread when getConnection is called at the same time.
	public void removeListener() {
		spyqlListener = null;
	}

	@Override
	public Connection getConnection() throws SQLException {
		Connection target = super.getConnection();
		return spyqlListener == null ? target : createConnectionProxy(target);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		Connection target = super.getConnection(username, password);
		return spyqlListener == null ? target : createConnectionProxy(target);
	}

	private Connection createConnectionProxy(Connection target) {
		return createProxy(new ConnectionInvocationHandler(target), ConnectionProxy.class);
	}

	private <T> T createProxy(InvocationHandler invocationHandler, Class<T> clazz) {
		return clazz.cast(Proxy.newProxyInstance(
				clazz.getClassLoader(),
				new Class<?>[]{clazz},
				invocationHandler));
	}

	private class ConnectionInvocationHandler implements InvocationHandler {

		private Connection target;
		private boolean withinTransaction = false;
		private SpyqlTransactionListener transactionListener;
		private long transactionStartTime;

		ConnectionInvocationHandler(Connection target) {
			if (target == null) {
				throw new IllegalArgumentException("target connection cannot be null");
			}
			this.target = target;
		}

		@Override
		@SuppressWarnings("deprecation")
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			try {
				switch (method.getName()) {
					case "createStatement": {
						Statement statement = (Statement) method.invoke(target, args);
						StatementInvocationHandler<Statement> invocationHandler =
								new StatementInvocationHandler<>(statement, this, null);
						return createProxy(invocationHandler, Statement.class);
					}
					case "prepareStatement": {
						PreparedStatement statement = (PreparedStatement) method.invoke(target, args);
						StatementInvocationHandler<PreparedStatement> invocationHandler =
								new StatementInvocationHandler<>(statement, this, (String) args[0]);
						return createProxy(invocationHandler, PreparedStatement.class);
					}
					case "prepareCall": {
						CallableStatement statement = (CallableStatement) method.invoke(target, args);
						StatementInvocationHandler<CallableStatement> invocationHandler =
								new StatementInvocationHandler<>(statement, this, (String) args[0]);
						return createProxy(invocationHandler, CallableStatement.class);
					}
					case "commit":
						method.invoke(target, args);
						if (transactionListener != null) {
							long transactionExecutionTimeNs = System.nanoTime() - transactionStartTime;
							try {
								transactionListener.onTransactionCommit(transactionExecutionTimeNs);
								transactionListener.onTransactionCommit();
								transactionListener.onTransactionComplete(transactionExecutionTimeNs);
							} catch (Exception ignore) {}
						}
						return null;
					case "rollback":
						method.invoke(target, args);
						if (transactionListener != null) {
							long transactionExecutionTimeNs = System.nanoTime() - transactionStartTime;
							try {
								transactionListener.onTransactionRollback(transactionExecutionTimeNs);
								transactionListener.onTransactionRollback();
								transactionListener.onTransactionComplete(transactionExecutionTimeNs);
							} catch (Exception ignore) {}
						}
						return null;
				}

				return method.invoke(target, args);
			} catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		boolean isInTransaction() {
			return withinTransaction;
		}

		void onTransactionBegin(SpyqlTransactionDefinition transactionSpy) {
			try {
				transactionListener = spyqlListener.onTransactionBegin(transactionSpy);
			} catch (Exception ignore) {}
			withinTransaction = true;
			transactionStartTime = System.nanoTime();
		}

		void onStatementExecuted(String sql, Long executionTimeNs) {
			if (transactionListener == null) {
				return;
			}
			try {
				transactionListener.onStatementExecute(sql, executionTimeNs);
			} catch (Exception ignore) {}
		}

		private Connection getTargetConnection(Method operation) {
			return target;
		}
	}

	private class StatementInvocationHandler<T extends Statement> implements InvocationHandler {

		private T target;
		private ConnectionInvocationHandler connectionInvocationHandler;
		private String sql;

		StatementInvocationHandler(T target, ConnectionInvocationHandler connectionInvocationHandler, String sql) {
			if (target == null) {
				throw new IllegalArgumentException("target statement cannot be null");
			}
			this.target = target;
			this.connectionInvocationHandler = connectionInvocationHandler;
			this.sql = sql;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			try {
				if (method.getName().startsWith("execute")) {
					if (!connectionInvocationHandler.isInTransaction() && !target.getConnection().getAutoCommit()) {
						connectionInvocationHandler.onTransactionBegin(createTransactionSpy());
					}

					long startTime = System.nanoTime();
					Object result = method.invoke(target, args);
					long executionTimeNs = System.nanoTime() - startTime;

					String sql = getSql(method, args);
					if (connectionInvocationHandler.isInTransaction()) {
						connectionInvocationHandler.onStatementExecuted(sql, executionTimeNs);
					} else {
						try {
							spyqlListener.onStatementExecute(sql, executionTimeNs);
						} catch (Exception ignored) {}
					}
					return result;
				}

				return method.invoke(target, args);
			} catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		private String getSql(Method method, Object[] args) {
			return isExecuteMethodWithSqlArgument(method, args) ? (String) args[0] : this.sql;
		}

		private SpyqlTransactionDefinition createTransactionSpy() {
			return new SpyqlTransactionDefinition(
					TransactionSynchronizationManager.getCurrentTransactionName(),
					TransactionSynchronizationManager.isCurrentTransactionReadOnly(),
					TransactionSynchronizationManager.getCurrentTransactionIsolationLevel()
			);
		}

		private boolean isExecuteMethodWithSqlArgument(Method method, Object[] args) {
			return method.getName().startsWith("execute") &&
					method.getParameterCount() > 0 &&
					method.getParameterTypes()[0] == String.class &&
					args[0] != null;
		}
	}

}
