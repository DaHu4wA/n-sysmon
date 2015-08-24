package com.nsysmon.impl;

import com.nsysmon.config.log.NSysMonLogger;
import com.nsysmon.measure.environment.AEnvironmentMeasurer;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * This class limits the impact on N-SysMon itself if some measurer runs into problems or uses large amounts of resources
 *
 * @author arno
 */
class RobustEnvironmentMeasurerWrapper {
    private final AEnvironmentMeasurer inner;
    private static final NSysMonLogger log = NSysMonLogger.get(RobustEnvironmentMeasurerWrapper.class);

    private final long timeoutNanos;
    private final int maxNumTimeouts;

    private final AtomicInteger numTimeouts = new AtomicInteger(0);

    private interface Strategy {
        void contributeMeasurements(AEnvironmentMeasurer.EnvironmentCollector data);
    }

    private final Strategy ENABLED = new Strategy() {
        @Override public void contributeMeasurements(AEnvironmentMeasurer.EnvironmentCollector data) {
            try {
                final long start = System.nanoTime();
                inner.contributeMeasurements(data);
                handleDuration(System.nanoTime() - start);
            } catch (Exception e) {
                log.warn("Disabling scalar measurer " + inner.getClass().getName() + " because an exception occurred", e);
                strategy = DISABLED;
            }
        }

        private void handleDuration(long durationNanos) {
            if(durationNanos > timeoutNanos) {
                log.warn("Environment measurer " + inner.getClass().getName() + " timed out (took " + durationNanos + "ns)");
                numTimeouts.incrementAndGet();
                strategy = TIMED_OUT;
            }
        }
    };

    private final Strategy TIMED_OUT = new Strategy() {
        @Override public void contributeMeasurements(AEnvironmentMeasurer.EnvironmentCollector data) {
            try {
                final long start = System.nanoTime();
                inner.contributeMeasurements(data);
                handleDuration(System.nanoTime() - start);
            } catch (Exception e) {
                log.warn("Disabling scalar measurer " + inner.getClass().getName() + " because an exception occurred", e);
                strategy = DISABLED;
            }
        }

        private void handleDuration(long durationNanos) {
            if(durationNanos > timeoutNanos) {
                if(numTimeouts.incrementAndGet() >= maxNumTimeouts) {
                    log.warn("Environment measurer " + inner.getClass().getName() + " timed out " + maxNumTimeouts + " times in row - permanently disabling");
                    strategy = DISABLED;
                }
                else {
                    strategy = ENABLED;
                }
            }
        }
    };

    private final Strategy DISABLED = new Strategy() {
        @Override public void contributeMeasurements(AEnvironmentMeasurer.EnvironmentCollector data) {
        }
    };

    private volatile Strategy strategy = ENABLED;

    RobustEnvironmentMeasurerWrapper(AEnvironmentMeasurer inner, long timeoutNanos, int maxNumTimeouts) {
        this.inner = inner;
        this.timeoutNanos = timeoutNanos;
        this.maxNumTimeouts = maxNumTimeouts;
    }

    void contributeMeasurements(AEnvironmentMeasurer.EnvironmentCollector data) {
        strategy.contributeMeasurements(data);
    }

    public void shutdown() {
        try {
            inner.shutdown();
        } catch (Exception exc) {
            log.error("failed to shut down environment measurer " + inner.getClass().getName() + ".", exc);
        }
    }
}
