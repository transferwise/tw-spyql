package com.transferwise.common.spyql;

public class SpyqlTransactionDefinition {
    private final String name;
    private final Boolean readOnly;
    private final Integer isolationLevel;

    public SpyqlTransactionDefinition(String name, Boolean readOnly, Integer isolationLevel) {
        this.name = name;
        this.readOnly = readOnly;
        this.isolationLevel = isolationLevel;
    }

    public String getName() {
        return name;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public Integer getIsolationLevel() {
        return isolationLevel;
    }
}
