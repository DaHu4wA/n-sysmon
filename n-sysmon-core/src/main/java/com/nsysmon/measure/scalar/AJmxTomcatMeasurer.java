package com.nsysmon.measure.scalar;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.nsysmon.data.AScalarDataPoint;
import org.apache.log4j.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Map;

public class AJmxTomcatMeasurer implements AScalarMeasurer {

    private static final Logger LOG = Logger.getLogger(AJmxTomcatMeasurer.class);


    private static final String KEY_PREFIX = "Tomcat ";
    private static final String KEY_REQUEST_COUNT = KEY_PREFIX + "Request Count";
    private static final String KEY_BYTES_RECEIVED  = KEY_PREFIX + "Received MB";
    private static final String KEY_BYTES_SEND  = KEY_PREFIX + "Send MB";
    private static final String KEY_ERROR_COUNT  = KEY_PREFIX + "Error Count";

    private static final String OBJECT_GLOBAL_REQUEST_PROCESSOR = "Tomcat:type=GlobalRequestProcessor,name=\"http-bio-8080\"";
    private static final String NAME_REQUEST_COUNT = "requestCount";
    private static final String NAME_BYTES_RECEIVED = "bytesReceived";
    private static final String NAME_BYTES_SEND = "bytesSend";
    private static final String NAME_ERROR_COUNT = "errorCount";


    @Override public void prepareMeasurements(Map<String, Object> mementos) throws Exception {
    }

    @Override public void contributeMeasurements(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        try {
            final Object requestCount = server.getAttribute(new ObjectName(OBJECT_GLOBAL_REQUEST_PROCESSOR), NAME_REQUEST_COUNT);
            final Object bytesReceived = server.getAttribute(new ObjectName(OBJECT_GLOBAL_REQUEST_PROCESSOR), NAME_BYTES_RECEIVED);
            final Object bytesSend = server.getAttribute(new ObjectName(OBJECT_GLOBAL_REQUEST_PROCESSOR), NAME_BYTES_SEND);
            final Object errorCount = server.getAttribute(new ObjectName(OBJECT_GLOBAL_REQUEST_PROCESSOR), NAME_ERROR_COUNT);

            data.put(KEY_REQUEST_COUNT, new AScalarDataPoint(timestamp, KEY_REQUEST_COUNT, (Integer) requestCount, 0));
            data.put(KEY_BYTES_RECEIVED, new AScalarDataPoint(timestamp, KEY_BYTES_RECEIVED, formatToMegaBytes((Long) bytesReceived), 3));
            data.put(KEY_BYTES_SEND, new AScalarDataPoint(timestamp, KEY_BYTES_SEND, formatToMegaBytes((Long) bytesSend), 3));
            data.put(KEY_ERROR_COUNT, new AScalarDataPoint(timestamp, KEY_ERROR_COUNT, (Integer) errorCount, 0));
        } catch (Exception e) {
            LOG.error(e);
        }
    }

    private long formatToMegaBytes(final long bytes) {
        final double megaByes = bytes / 1024;
        double megaBytesWithoutBytes = megaByes * 1000;
        long test = Math.round(megaBytesWithoutBytes);
        return test / 1024;
    }

    @Override public AOption<Long> getTimeoutInMilliSeconds() {
        return AOption.none();
    }

    @Override public void shutdown() throws Exception {
    }
}