package com.transferwise.common.spyql.event;

import com.transferwise.common.spyql.SpyqlTransactionDefinition;
import java.time.Instant;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SpyqlTransaction {

  private boolean empty;
  private long id;
  private SpyqlTransactionDefinition definition;
  private Instant startTime;
  private Instant endTime;
}
