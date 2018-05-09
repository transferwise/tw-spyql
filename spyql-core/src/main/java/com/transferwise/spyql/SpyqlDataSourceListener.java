package com.transferwise.spyql;

public interface SpyqlDataSourceListener {
	SpyqlConnectionListener onGetConnection(GetConnectionResult result);
}
