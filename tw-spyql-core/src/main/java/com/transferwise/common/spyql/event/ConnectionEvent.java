package com.transferwise.common.spyql.event;

public interface ConnectionEvent extends SpyqlEvent {

  long getConnectionId();
}
