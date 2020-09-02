package com.transferwise.common.spyql;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SpyqlTransactionDefinition {

  private final String name;
  private final Boolean readOnly;
  private final Integer isolationLevel;

  private String entryPointName;
  private String entryPointGroup;
  private String entryPointOwner;

  public SpyqlTransactionDefinition(String name, Boolean readOnly, Integer isolationLevel) {
    this.name = name;
    this.readOnly = readOnly;
    this.isolationLevel = isolationLevel;
  }

}
