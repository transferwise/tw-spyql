package com.transferwise.common.spyql.event;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ResultSetNextRowsEvent implements ConnectionEvent {

  private long connectionId;
  private Long transactionId;
  private long rowsCount;

  public boolean isInTransaction() {
    return transactionId != null;
  }
}
