package com.transferwise.spyql.utils

import spock.lang.Specification

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class SimpleThrottlerSpec extends Specification {
    def "throttling works as expected"() {
        given:
            SimpleThrottler simpleThrottler = new SimpleThrottler(Duration.ofMinutes(1), 10l)
            Instant now = Instant.now()
            simpleThrottler.clock = Clock.fixed(now, ZoneId.systemDefault())
        when:
            10.times {
                def result = simpleThrottler.doThrottleAnEvent()
                assert !result
            }
            1.times {
                def result = simpleThrottler.doThrottleAnEvent()
                assert result
            }
            simpleThrottler.clock = Clock.fixed(now.plusSeconds(59), ZoneId.systemDefault())
            1.times {
                def result = simpleThrottler.doThrottleAnEvent()
                assert result
            }
            simpleThrottler.clock = Clock.fixed(now.plusSeconds(60), ZoneId.systemDefault())
            1.times {
                def result = simpleThrottler.doThrottleAnEvent()
                assert !result
            }
        then:
            1 == 1
    }
}
