package com.transferwise.spyql.rx.events;

public class ConnectionAcquireEvent implements ConnectionEvent {
	private long connectionId;
	private long acquireTimeNs;

	public ConnectionAcquireEvent(long connectionId, long acquireTimeNs) {
		this.connectionId = connectionId;
		this.acquireTimeNs = acquireTimeNs;
	}

	@Override
	public long getConnectionId() {
		return connectionId;
	}

	public long getAcquireTimeNs() {
		return acquireTimeNs;
	}

	@Override
	public String toString() {
		return "ConnectionAcquireEvent{" +
				"connectionId=" + connectionId +
				", acquireTimeNs=" + acquireTimeNs +
				'}';
	}
}
