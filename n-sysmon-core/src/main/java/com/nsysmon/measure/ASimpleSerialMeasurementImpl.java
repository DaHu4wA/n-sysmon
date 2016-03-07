package com.nsysmon.measure;

import com.nsysmon.config.log.NSysMonLogger;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author arno
 */
class ASimpleSerialMeasurementImpl implements ASimpleMeasurement {
    private static final NSysMonLogger log = NSysMonLogger.get(ASimpleSerialMeasurementImpl.class);

    private final AMeasurementHierarchy hierarchy;

    private final long startTimeMillis = System.currentTimeMillis();
    private final long startTimeNanos; // this number has no absolute meaning and is useful only for measuring differences
    private final String identifier;

    private final Map<String, String> parameters = new TreeMap<>();

    private boolean isFinished = false;

    ASimpleSerialMeasurementImpl(AMeasurementHierarchy hierarchy, long startTimeNanos, String identifier) {
        this.hierarchy = hierarchy;
        this.startTimeNanos = startTimeNanos;
        this.identifier = identifier;
    }

    @Override public void addParameter(String identifier, String value) {
        if(parameters.put(identifier, value) != null) {
            log.warn("duplicate parameter " + identifier + " for a measurement");
        }
    }

    public void finish() {
        if(isFinished) {
            log.error (new IllegalStateException("a simple measurement can be finished only once."));
            return;
        }

        isFinished = true;
        hierarchy.finish(this);
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public long getStartTimeNanos() {
        return startTimeNanos;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return "ASimpleMeasurement {" + identifier + " " + parameters + "}";
    }
}
