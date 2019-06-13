package com.transferwise.common.spyql.event;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ConnectionCloseEvent implements ConnectionEvent {
    private long executionTimeNs;
    private long connectionId;
    private Long transactionId;
}
