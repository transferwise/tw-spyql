package com.transferwise.common.spyql.spring;

import com.transferwise.common.spyql.SpyqlTransactionDefinition;
import com.transferwise.common.spyql.TransactionDefinitionProvider;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class SpringTransactionDefinition implements TransactionDefinitionProvider {
    @Override
    public SpyqlTransactionDefinition get() {
        return new SpyqlTransactionDefinition(
            TransactionSynchronizationManager.getCurrentTransactionName(),
            TransactionSynchronizationManager.isCurrentTransactionReadOnly(),
            TransactionSynchronizationManager.getCurrentTransactionIsolationLevel()
        );
    }
}
