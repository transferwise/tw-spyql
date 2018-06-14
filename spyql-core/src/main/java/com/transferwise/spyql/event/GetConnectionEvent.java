package com.transferwise.spyql.event;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class GetConnectionEvent implements DataSourceEvent {
    private long executionTimeNs;
    private long connectionId;
}
