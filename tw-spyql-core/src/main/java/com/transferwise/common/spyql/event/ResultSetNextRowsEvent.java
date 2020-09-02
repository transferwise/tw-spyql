package com.transferwise.common.spyql.event;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ResultSetNextRowsEvent implements ConnectionEvent {

  private long connectionId;
  private SpyqlTransaction transaction;
  private long rowsCount;

  public boolean isInTransaction() {
    return transaction != null;
  }
}
