package com.transferwise.common.spyql.event;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class StatementExecuteFailureEvent implements ConnectionEvent {

  private long executionTimeNs;
  private long connectionId;
  private SpyqlTransaction transaction;
  private String sql;
  private Throwable throwable;
}
