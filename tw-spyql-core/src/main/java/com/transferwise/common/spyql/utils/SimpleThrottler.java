package com.transferwise.common.spyql.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Duration;

@Slf4j
public class SimpleThrottler {
    private long bucketTime = -1;
    private long errorsInBucket;

    private long ratePeriodMs;
    private long rate;

    private Clock clock;

    public SimpleThrottler(Duration ratePeriod, long rate) {
        this.ratePeriodMs = ratePeriod.toMillis();
        this.rate = rate;
        this.clock = Clock.systemDefaultZone();
    }

    public synchronized boolean doThrottleAnEvent() {
        if (bucketTime == -1 || clock.millis() - bucketTime >= ratePeriodMs) {
            bucketTime = clock.millis();
            errorsInBucket = 0;
        }
        errorsInBucket++;
        return errorsInBucket > rate;
    }
}
