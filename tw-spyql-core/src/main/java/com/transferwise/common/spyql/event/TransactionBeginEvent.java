package com.transferwise.common.spyql.event;

import com.transferwise.common.spyql.SpyqlTransactionDefinition;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TransactionBeginEvent implements ConnectionEvent {
    private SpyqlTransactionDefinition transactionDefinition;
    private long connectionId;
    private long transactionId;
}
