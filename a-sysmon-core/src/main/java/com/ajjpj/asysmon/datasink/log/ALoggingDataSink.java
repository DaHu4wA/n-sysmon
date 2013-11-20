package com.ajjpj.asysmon.datasink.log;

import com.ajjpj.asysmon.data.ACorrelationId;
import com.ajjpj.asysmon.data.AHierarchicalData;
import com.ajjpj.asysmon.datasink.ADataSink;

import java.util.Collection;


public abstract class ALoggingDataSink implements ADataSink {
    @Override public void onStartedHierarchicalMeasurement() {
    }

    @Override public void onFinishedHierarchicalMeasurement(AHierarchicalData data, Collection<ACorrelationId> startedFlows, Collection<ACorrelationId> joinedFlows) {
        if(isLoggingEnabled()) {
            logDataRec(data, 0);
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
