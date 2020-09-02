package com.transferwise.common.spyql.event;

import com.transferwise.common.spyql.SpyqlTransactionDefinition;
import java.time.Instant;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TransactionRollbackFailureEvent implements ConnectionEvent {

  private long executionTimeNs;
  private long connectionId;
  private SpyqlTransaction transaction;
  private Throwable throwable;
}
