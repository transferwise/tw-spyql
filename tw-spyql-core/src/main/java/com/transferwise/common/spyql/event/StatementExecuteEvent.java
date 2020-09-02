package com.transferwise.common.spyql.event;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class StatementExecuteEvent implements ConnectionEvent {

  private long executionTimeNs;
  private long connectionId;
  private long affectedRowsCount;
  private SpyqlTransaction transaction;
  private String sql;

  public boolean isInTransaction() {
    return transaction != null;
  }
}
