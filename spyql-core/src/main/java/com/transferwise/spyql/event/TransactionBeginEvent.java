package com.transferwise.spyql.event;

import com.transferwise.spyql.SpyqlTransactionDefinition;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TransactionBeginEvent implements ConnectionEvent {
    private SpyqlTransactionDefinition transactionDefinition;
    private long connectionId;
    private long transactionId;
}
