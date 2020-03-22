package com.transferwise.common.spyql.event;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class StatementExecuteEvent implements ConnectionEvent {

  private long executionTimeNs;
  private long connectionId;
  private Long transactionId;
  private String sql;

  public boolean isInTransaction() {
    return transactionId != null;
  }
}
