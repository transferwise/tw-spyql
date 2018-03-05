package com.transferwise.spyql;

import javax.sql.DataSource;
import java.sql.SQLException;

public class SpyqlHelper {
	public static void setDataSourceListener(DataSource dataSource, SpyqlListener listener) throws SQLException, SpyqlException {
		if (dataSource.isWrapperFor(SpyqlDataSourceProxy.class)) {
			dataSource
					.unwrap(SpyqlDataSourceProxy.class)
					.setListener(listener);
		} else {
			throw new SpyqlException("Unable to attach listener to the dataSource because it doesn't have SpyqlDataSourceProxy in the proxy chain");
		}
	}
}
