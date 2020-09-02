package com.transferwise.common.spyql.event;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ConnectionCloseFailureEvent implements ConnectionEvent {

  private long executionTimeNs;
  private long connectionId;
  private SpyqlTransaction transaction;
  private Throwable throwable;
}
