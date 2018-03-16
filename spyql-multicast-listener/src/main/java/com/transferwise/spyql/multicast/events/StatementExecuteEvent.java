package com.transferwise.spyql.multicast.events;

public class StatementExecuteEvent implements Event {
	private String sql;
	private Long executionTimeNs;

	public StatementExecuteEvent(String sql, Long executionTimeNs) {
		this.sql = sql;
		this.executionTimeNs = executionTimeNs;
	}

	public String getSql() {
		return sql;
	}

	public Long getExecutionTimeNs() {
		return executionTimeNs;
	}

	@Override
	public String toString() {
		return "StatementExecuteEvent{" +
				"sql='" + sql + '\'' +
				", executionTimeNs=" + executionTimeNs +
				'}';
	}
}
