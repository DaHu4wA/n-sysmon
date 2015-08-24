package com.nsysmon.util.timer;

/**
 * @author arno
 */
public class ASystemNanoTimer implements ATimer {
    @Override public long getCurrentNanos() {
        return System.nanoTime();
    }
}
