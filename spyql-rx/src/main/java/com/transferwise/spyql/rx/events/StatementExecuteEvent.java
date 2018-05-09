package com.transferwise.spyql.rx.events;

public class StatementExecuteEvent implements ConnectionEvent {
	private long connectionId;
	private String sql;
	private long executionTimeNs;

	public StatementExecuteEvent(long connectionId, String sql, long executionTimeNs) {
		this.connectionId = connectionId;
		this.sql = sql;
		this.executionTimeNs = executionTimeNs;
	}

	public long getConnectionId() {
		return connectionId;
	}

	public String getSql() {
		return sql;
	}

	public long getExecutionTimeNs() {
		return executionTimeNs;
	}

	@Override
	public String toString() {
		return "StatementExecuteEvent{" +
				"connectionId=" + connectionId +
				", sql='" + sql + '\'' +
				", executionTimeNs=" + executionTimeNs +
				'}';
	}
}
