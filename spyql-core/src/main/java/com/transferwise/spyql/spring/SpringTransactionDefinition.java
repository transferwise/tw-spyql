package com.transferwise.spyql.spring;

import com.transferwise.spyql.SpyqlTransactionDefinition;
import com.transferwise.spyql.TransactionDefinitionProvider;
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
