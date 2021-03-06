package com.nsysmon.datasink.log;

import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.datasink.ADataSink;


public abstract class ALoggingDataSink implements ADataSink {
    @Override public void onStartedHierarchicalMeasurement(String identifier) {
    }

    @Override public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot data) {
        if(isLoggingEnabled()) {
            logDataRec(data.getRootNode(), 0);
        }
    }

    @Override public void shutdown() {
    }

    protected boolean isLoggingEnabled() {
        return true;
    }

    private String indent(int level) {
        return "                                                                                                                                                                                                                                                                                    ".substring(0, 2*level);
    }

    private void logDataRec(AHierarchicalData data, int level) {
        String s = indent(level) + (data.isSerial() ? "+" : "-") + data.getIdentifier() + "@" + data.getStartTimeMillis() + ": " + data.getDurationNanos() + " " + data.getParameters();
        log(s);

        for(AHierarchicalData child: data.getChildren()) {
            logDataRec(child, level + 1);
        }
    }

    protected abstract void log(String s);
}
