package com.transferwise.common.spyql.event;

import com.transferwise.common.spyql.SpyqlTransactionDefinition;
import java.time.Instant;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TransactionBeginEvent implements ConnectionEvent {

  private SpyqlTransaction transaction;
  private long connectionId;
}
