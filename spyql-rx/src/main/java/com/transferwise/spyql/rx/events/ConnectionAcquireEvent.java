package com.transferwise.spyql.rx.events;

import com.transferwise.spyql.GetConnectionResult;

public class ConnectionAcquireEvent implements ConnectionEvent {
	private long connectionId;
	private GetConnectionResult result;

	public ConnectionAcquireEvent(long connectionId, GetConnectionResult result) {
		this.connectionId = connectionId;
		this.result = result;
	}

	@Override
	public long getConnectionId() {
		return connectionId;
	}

	public GetConnectionResult getResult() {
		return result;
	}

	@Override
	public String toString() {
		return "ConnectionAcquireEvent{" +
				"connectionId=" + connectionId +
				", result=" + result +
				'}';
	}
}
