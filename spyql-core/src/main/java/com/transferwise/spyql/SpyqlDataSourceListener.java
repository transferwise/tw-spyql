package com.transferwise.spyql;

public interface SpyqlDataSourceListener {
	SpyqlConnectionListener onGetConnection(Long acquireTimeNs);
}
