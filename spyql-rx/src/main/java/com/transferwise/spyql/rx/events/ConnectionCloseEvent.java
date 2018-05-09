package com.transferwise.spyql.rx.events;

public class ConnectionCloseEvent implements ConnectionEvent {
	private long connectionId;

	public ConnectionCloseEvent(long connectionId) {
		this.connectionId = connectionId;
	}

	@Override
	public long getConnectionId() {
		return connectionId;
	}

	@Override
	public String toString() {
		return "ConnectionCloseEvent{" +
				"connectionId=" + connectionId +
				'}';
	}
}
