package com.transferwise.spyql.listener;

import com.transferwise.spyql.event.GetConnectionEvent;
import com.transferwise.spyql.event.GetConnectionFailureEvent;

public interface SpyqlDataSourceListener {
    SpyqlConnectionListener onGetConnection(GetConnectionEvent event);

    default void onGetConnectionFailure(GetConnectionFailureEvent event) {
    }
}
