package com.transferwise.spyql;

import javax.sql.DataSource;
import java.sql.SQLException;

public class SpyqlHelper {
	public static void setDataSourceListener(DataSource dataSource, SpyqlDataSourceListener listener) throws SQLException, SpyqlException {
		if (dataSource.isWrapperFor(SpyqlDataSourceProxy.class)) {
			dataSource
					.unwrap(SpyqlDataSourceProxy.class)
					.setListener(listener);
		} else {
			throw new SpyqlException("Unable to attach listener to the dataSource because it doesn't have SpyqlDataSourceProxy in the proxy chain");
		}
	}

	public static void setDataSourceListener(DataSource dataSource, SpyqlConnectionListener listener) throws SQLException, SpyqlException {
		setDataSourceListener(dataSource, new SpyqlDataSourceListener() {
			@Override
			public SpyqlConnectionListener onGetConnection(GetConnectionResult result) {
				return listener;
			}
		});
	}
}
