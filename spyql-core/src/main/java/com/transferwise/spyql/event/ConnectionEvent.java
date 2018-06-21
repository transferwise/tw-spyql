package com.transferwise.spyql.event;

public interface ConnectionEvent extends SpyqlEvent {
    long getConnectionId();
}
