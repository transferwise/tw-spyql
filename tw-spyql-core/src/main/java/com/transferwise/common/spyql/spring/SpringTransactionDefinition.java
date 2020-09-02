package com.transferwise.common.spyql.spring;

import com.transferwise.common.context.TwContext;
import com.transferwise.common.spyql.SpyqlTransactionDefinition;
import com.transferwise.common.spyql.TransactionDefinitionProvider;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class SpringTransactionDefinition implements TransactionDefinitionProvider {

  @Override
  public SpyqlTransactionDefinition get() {
    TwContext twContext = TwContext.current();

    return new SpyqlTransactionDefinition(
        TransactionSynchronizationManager.getCurrentTransactionName(),
        TransactionSynchronizationManager.isCurrentTransactionReadOnly(),
        TransactionSynchronizationManager.getCurrentTransactionIsolationLevel()
    )
        .setEntryPointName(twContext.getName())
        .setEntryPointGroup(twContext.getGroup())
        .setEntryPointOwner(twContext.getOwner());
  }
}
