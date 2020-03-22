package com.transferwise.common.spyql.event;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class GetConnectionFailureEvent implements DataSourceEvent {

  private long executionTimeNs;
  private boolean nullReturned;
  private Throwable throwable;
  private long connectionId;
}
