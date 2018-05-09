package com.transferwise.spyql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	private static final Logger log = LoggerFactory.getLogger(SpyqlDataSourceProxy.class);

	private SpyqlDataSourceListener spyqlListener;

	public SpyqlDataSourceProxy(DataSource targetDataSource, SpyqlDataSourceListener spyqlListener) {
		super(targetDataSource);
		this.spyqlListener = spyqlListener;
	}

	public SpyqlDataSourceProxy(SpyqlDataSourceListener spyqlListener) {
		super();
		this.spyqlListener = spyqlListener;
	}

	public SpyqlDataSourceProxy(DataSource targetDataSource) {
		super(targetDataSource);
	}

	public SpyqlDataSourceProxy() {
		super();
	}

	public void setListener(SpyqlDataSourceListener listener) {
		spyqlListener = listener;
	}

	// TODO: Think about the case when listener removed from a different thread when getConnection is called at the same time.
	public void removeListener() {
		spyqlListener = null;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return wrappedGetConnection(super::getConnection);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return wrappedGetConnection(() -> super.getConnection(username, password));
	}

	private Connection wrappedGetConnection(ConnectionFactory connectionFactory) throws SQLException {
		if (spyqlListener == null) {
			return connectionFactory.getConnection();
		}
		long startTime = System.nanoTime();
		Connection target;
		try {
			target = connectionFactory.getConnection();
		} catch (Throwable e) {
			long executionTimeNs = System.nanoTime() - startTime;
			spyqlListener.onGetConnection(new GetConnectionException(e.getClass().getName(), e.getMessage(), executionTimeNs));
			throw e;
		}
		long executionTimeNs = System.nanoTime() - startTime;
		if (target == null) {
			spyqlListener.onGetConnection(new GetConnectionNull(executionTimeNs));
			return null;
		}
		SpyqlConnectionListener spyqlConnectionListener = spyqlListener.onGetConnection(new GetConnectionSuccess(executionTimeNs));
		if (spyqlConnectionListener != null) {
			return createConnectionProxy(target, spyqlConnectionListener);
		}
		return target;
	}

	private Connection createConnectionProxy(Connection target, SpyqlConnectionListener spyqlConnectionListener) {
		return createProxy(new ConnectionInvocationHandler(target, spyqlConnectionListener), ConnectionProxy.class);
	}

	private <T> T createProxy(InvocationHandler invocationHandler, Class<T> clazz) {
		return clazz.cast(Proxy.newProxyInstance(
				clazz.getClassLoader(),
				new Class<?>[]{clazz},
				invocationHandler));
	}

	private class ConnectionInvocationHandler implements InvocationHandler {

		private SpyqlConnectionListener spyqlConnectionListener;
		private Connection target;
		private boolean withinTransaction = false;
		private SpyqlTransactionListener transactionListener;
		private long transactionStartTime;

		ConnectionInvocationHandler(Connection target, SpyqlConnectionListener spyqlConnectionListener) {
			this.target = target;
			this.spyqlConnectionListener = spyqlConnectionListener;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			try {
				switch (method.getName()) {
					case "createStatement": {
						Statement statement = (Statement) method.invoke(target, args);
						if (statement == null) {
							return null;
						}
						StatementInvocationHandler<Statement> invocationHandler =
								new StatementInvocationHandler<>(statement, this, null);
						return createProxy(invocationHandler, Statement.class);
					}
					case "prepareStatement": {
						PreparedStatement statement = (PreparedStatement) method.invoke(target, args);
						if (statement == null) {
							return null;
						}
						StatementInvocationHandler<PreparedStatement> invocationHandler =
								new StatementInvocationHandler<>(statement, this, (String) args[0]);
						return createProxy(invocationHandler, PreparedStatement.class);
					}
					case "prepareCall": {
						CallableStatement statement = (CallableStatement) method.invoke(target, args);
						if (statement == null) {
							return null;
						}
						StatementInvocationHandler<CallableStatement> invocationHandler =
								new StatementInvocationHandler<>(statement, this, (String) args[0]);
						return createProxy(invocationHandler, CallableStatement.class);
					}
					case "commit":
						method.invoke(target, args);
						withinTransaction = false;
						if (transactionListener != null) {
							long transactionExecutionTimeNs = System.nanoTime() - transactionStartTime;
							try {
								transactionListener.onTransactionCommit(transactionExecutionTimeNs);
							} catch (Exception e) {
								log.error("Exception was thrown in listener:", e);
							}
						}
						return null;
					case "rollback":
						method.invoke(target, args);
						withinTransaction = false;
						if (transactionListener != null) {
							long transactionExecutionTimeNs = System.nanoTime() - transactionStartTime;
							try {
								transactionListener.onTransactionRollback(transactionExecutionTimeNs);
							} catch (Exception e) {
								log.error("Exception was thrown in listener:", e);
							}
						}
						return null;
					case "close":
						method.invoke(target, args);
						spyqlConnectionListener.onClose();
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

		void onTransactionBegin(SpyqlTransactionDefinition transactionDefinition) {
			try {
				transactionListener = spyqlConnectionListener.onTransactionBegin(transactionDefinition);
			} catch (Exception e) {
				log.error("Exception was thrown in listener:", e);
			}
			withinTransaction = true;
			transactionStartTime = System.nanoTime();
		}

		void onStatementExecuted(String sql, Long executionTimeNs) {
			if (isInTransaction()) {
				if (transactionListener == null) {
					return;
				}
				try {
					transactionListener.onStatementExecute(sql, executionTimeNs);
				} catch (Exception e) {
					log.error("Exception was thrown in listener:", e);
				}
			} else {
				try {
					spyqlConnectionListener.onStatementExecute(sql, executionTimeNs);
				} catch (Exception e) {
					log.error("Exception was thrown in listener:", e);
				}
			}
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
			this.target = target;
			this.connectionInvocationHandler = connectionInvocationHandler;
			this.sql = sql;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			try {
				if (method.getName().startsWith("execute")) {
					if (!connectionInvocationHandler.isInTransaction() && !target.getConnection().getAutoCommit()) {
						connectionInvocationHandler.onTransactionBegin(createTransactionDefinition());
					}

					long startTime = System.nanoTime();
					Object result = method.invoke(target, args);
					long executionTimeNs = System.nanoTime() - startTime;

					String sql = getSql(method, args);
					connectionInvocationHandler.onStatementExecuted(sql, executionTimeNs);
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

		private SpyqlTransactionDefinition createTransactionDefinition() {
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

	interface ConnectionFactory {
		Connection getConnection() throws SQLException;
	}
}
