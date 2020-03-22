package com.transferwise.common.spyql.listener;

import com.transferwise.common.spyql.event.GetConnectionEvent;
import com.transferwise.common.spyql.event.GetConnectionFailureEvent;

public interface SpyqlDataSourceListener {

  SpyqlConnectionListener onGetConnection(GetConnectionEvent event);

  default void onGetConnectionFailure(GetConnectionFailureEvent event) {
  }
}
