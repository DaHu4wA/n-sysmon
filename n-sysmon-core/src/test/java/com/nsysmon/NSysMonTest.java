package com.nsysmon;


import com.nsysmon.config.NSysMonConfigBuilder;
import com.nsysmon.config.appinfo.ADefaultApplicationInfoProvider;
import com.nsysmon.data.ACorrelationId;
import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.datasink.ADataSink;
import com.nsysmon.impl.NSysMonConfigurer;
import com.nsysmon.impl.NSysMonImpl;
import com.nsysmon.measure.ACollectingMeasurement;
import com.nsysmon.measure.ASimpleMeasurement;
import com.nsysmon.testutil.CollectingDataSink;
import com.nsysmon.testutil.CountingDataSink;
import com.nsysmon.testutil.CountingLoggerFactory;
import com.nsysmon.testutil.ExplicitTimer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;


/**
 * @author arno
 */
public class NSysMonTest {
    private NSysMonConfigBuilder configBuilder;

    @Before
    public void before() throws UnknownHostException {
        configBuilder = new NSysMonConfigBuilder(new ADefaultApplicationInfoProvider("dummy", "version"));
    }

    private NSysMonApi createSysMon(ADataSink dataSink) {
        final NSysMonApi result = new NSysMonImpl(configBuilder.build());
        NSysMonConfigurer.addDataSink(result, dataSink);
        return result;
    }

    @Test
    public void testSimpleMeasurement() {
        final CollectingDataSink dataSink = new CollectingDataSink();
        final NSysMonApi sysMon = createSysMon(dataSink);

        sysMon.start("a").finish();

        assertEquals(1, dataSink.data.size());
        assertEquals("a", dataSink.data.get(0).getRootNode().getIdentifier());
    }

    @Test
    public void testNestedMeasurement() {
        final CollectingDataSink dataSink = new CollectingDataSink();
        final NSysMonApi sysMon = createSysMon(dataSink);

        final ASimpleMeasurement m = sysMon.start("a");
        sysMon.start("b").finish();
        sysMon.start("c").finish();
        m.finish();

        sysMon.start("d").finish();

        assertEquals(2, dataSink.data.size());
        assertEquals("a", dataSink.data.get(0).getRootNode().getIdentifier());
        assertEquals("b", dataSink.data.get(0).getRootNode().getChildren().get(0).getIdentifier());
        assertEquals("c", dataSink.data.get(0).getRootNode().getChildren().get(1).getIdentifier());

        assertEquals("d", dataSink.data.get(1).getRootNode().getIdentifier());
    }

    @Test
    public void testCollectingMeasurement() {
        final CollectingDataSink dataSink = new CollectingDataSink();
        final NSysMonApi sysMon = createSysMon(dataSink);

        final ASimpleMeasurement m = sysMon.start("a");
        final ACollectingMeasurement coll = sysMon.startCollectingMeasurement("b");

        coll.startDetail("c1");

        sysMon.start("d").finish();

        coll.finishDetail();

        coll.startDetail("c3");
        coll.addDetailMeasurement("c2", 123);
        coll.finishDetail();

        coll.finish();
        m.finish();

        assertEquals(1, dataSink.data.size());

        final AHierarchicalData root = dataSink.data.get(0).getRootNode();
        assertEquals(2, root.getChildren().size());

        assertEquals("d", root.getChildren().get(0).getIdentifier());

        final AHierarchicalData data = root.getChildren().get(1);
        assertEquals("b", data.getIdentifier());
        assertEquals(3, data.getChildren().size());
        assertEquals("c1", data.getChildren().get(0).getIdentifier());
        assertEquals("c2", data.getChildren().get(1).getIdentifier());
        assertEquals("c3", data.getChildren().get(2).getIdentifier());
    }

    @Test
    public void testOverlappingDetails() {
        final CollectingDataSink dataSink = new CollectingDataSink();
        final NSysMonApi sysMon = createSysMon(dataSink);

        final ASimpleMeasurement m = sysMon.start("a");
        final ACollectingMeasurement coll = sysMon.startCollectingMeasurement("b");

        coll.startDetail("c1");
//        try {
            coll.startDetail("c2");
//            fail("exception expected");
//        }
//        catch(Exception exc) {
//            expected
//        }

        m.finish();
    }

    @Test
    public void testParallelMeasurement() {
        final CollectingDataSink dataSink = new CollectingDataSink();
        final NSysMonApi sysMon = createSysMon(dataSink);

        // NB: Parallel measurements do not nest. They can have no children, and they can be closed at any point,
        //      regardless of hierarchy (i.e. as long as the root measurement is not finished).

        final ASimpleMeasurement a = sysMon.start("a");
        final ASimpleMeasurement c1 = sysMon.start("c1", false);

        final ASimpleMeasurement b = sysMon.start("b", true);
        final ASimpleMeasurement c2 = sysMon.start("c2", false);
        final ASimpleMeasurement c3 = sysMon.start("c3", false);

        sysMon.start("d", true).finish();

        c3.finish();
        c1.finish();
        b.finish();
        c2.finish();
        a.finish();

        assertEquals(1, dataSink.data.size());
        final AHierarchicalData ma = dataSink.data.get(0).getRootNode();

        // c1 and b were started directly within ma
        assertEquals(2, ma.getChildren().size());

        // c1 was closed first
        assertEquals("c1", ma.getChildren().get(0).getIdentifier());
        assertEquals("b", ma.getChildren().get(1).getIdentifier());

        final AHierarchicalData mb = ma.getChildren().get(1);

        // c2 and c3 were *started* within b, and that's what determines their data's position in the measurement hierarchy
        assertEquals(3, mb.getChildren().size());

        assertEquals("d", mb.getChildren().get(0).getIdentifier());

        // c3 was finished first
        assertEquals("c3", mb.getChildren().get(1).getIdentifier());
        assertEquals("c2", mb.getChildren().get(2).getIdentifier());
    }

    @Test
    public void testFinishRootWithUnfinishedCollectingMeasurement() {
        final CollectingDataSink dataSink = new CollectingDataSink();
        final NSysMonApi sysMon = createSysMon(dataSink);

        final ASimpleMeasurement m = sysMon.start("m");
        final ACollectingMeasurement a = sysMon.startCollectingMeasurement("a");
        m.finish();

        assertEquals(1, dataSink.data.size());
        assertEquals(1, dataSink.data.get(0).getRootNode().getChildren().size());

//        try {
            a.finish();
//            fail("exception expected");
//        }
//        catch(IllegalStateException exc) {
            // expected
//        }

        assertEquals(1, dataSink.data.size());
        assertEquals(1, dataSink.data.get(0).getRootNode().getChildren().size());
    }

    @Test
    public void testSerialTimerRecording() {
        final ExplicitTimer timer = new ExplicitTimer();
        final CollectingDataSink dataSink = new CollectingDataSink();
        configBuilder.setTimer(timer);
        final NSysMonApi sysMon = createSysMon(dataSink);

        final ASimpleMeasurement m = sysMon.start("a");
        timer.curNanos += 100;
        sysMon.start("b").finish();
        final ASimpleMeasurement m2 = sysMon.start("b");
        timer.curNanos += 100;
        m2.finish();
        timer.curNanos += 100;
        m.finish();

        assertEquals(1, dataSink.data.size());
        final AHierarchicalData root = dataSink.data.get(0).getRootNode();
        assertEquals(300L, root.getDurationNanos());

        assertEquals(2, root.getChildren().size());

        assertEquals(0L, root.getChildren().get(0).getDurationNanos());
        assertEquals(100L, root.getChildren().get(1).getDurationNanos());
    }

    @Test
    public void testParallelTimerRecording() {
        final ExplicitTimer timer = new ExplicitTimer();
        final CollectingDataSink dataSink = new CollectingDataSink();
        configBuilder.setTimer(timer);
        final NSysMonApi sysMon = createSysMon(dataSink);

        final ASimpleMeasurement m = sysMon.start("m");
        timer.curNanos += 100;

        final ASimpleMeasurement a1 = sysMon.start("a1", false);
        timer.curNanos += 100;

        final ASimpleMeasurement a2 = sysMon.start("a2", false);
        timer.curNanos += 100;

        a1.finish();
        timer.curNanos += 100;

        a2.finish();
        timer.curNanos += 100;

        m.finish();

        assertEquals(1, dataSink.data.size());
        final AHierarchicalData root = dataSink.data.get(0).getRootNode();
        assertEquals(500L, root.getDurationNanos());

        assertEquals(2, root.getChildren().size());

        assertEquals(200L, root.getChildren().get(0).getDurationNanos());
        assertEquals(200L, root.getChildren().get(1).getDurationNanos());
    }

    @Test
    public void testCollectingTimerRecording() {
        final ExplicitTimer timer = new ExplicitTimer();
        final CollectingDataSink dataSink = new CollectingDataSink();
        configBuilder.setTimer(timer);
        final NSysMonApi sysMon = createSysMon(dataSink);

        final ASimpleMeasurement m = sysMon.start("m");
        timer.curNanos += 100;

        final ACollectingMeasurement a1 = sysMon.startCollectingMeasurement("a1");
        final ACollectingMeasurement a2 = sysMon.startCollectingMeasurement("a2");

        a1.startDetail("x");
        timer.curNanos += 100;

        a2.startDetail("x");
        timer.curNanos += 100;

        a1.finishDetail();
        a2.finishDetail();

        a1.startDetail("x");
        timer.curNanos += 100;

        a2.startDetail("x");
        timer.curNanos += 100;

        a1.finishDetail();
        a2.finishDetail();

        a1.startDetail("y");
        timer.curNanos += 100;

        a2.startDetail("y");
        timer.curNanos += 100;

        a1.finishDetail();
        a2.finishDetail();

        a1.addDetailMeasurement("y", 123);

        a1.finish();
        a2.finish();
        m.finish();

        assertEquals(1, dataSink.data.size());
        final AHierarchicalData root = dataSink.data.get(0).getRootNode();

        assertEquals(2, root.getChildren().size());

        final AHierarchicalData m1 = root.getChildren().get(0);
        final AHierarchicalData m2 = root.getChildren().get(1);

        assertEquals(723L, m1.getDurationNanos());
        assertEquals(2,    m1.getChildren().size());
        assertEquals("x",  m1.getChildren().get(0).getIdentifier());
        assertEquals(400L, m1.getChildren().get(0).getDurationNanos());
        assertEquals("y",  m1.getChildren().get(1).getIdentifier());
        assertEquals(323L, m1.getChildren().get(1).getDurationNanos());

        assertEquals(300L, m2.getDurationNanos());
        assertEquals(2,    m2.getChildren().size());
        assertEquals("x",  m2.getChildren().get(0).getIdentifier());
        assertEquals(200L, m2.getChildren().get(0).getDurationNanos());
        assertEquals("y",  m2.getChildren().get(1).getIdentifier());
        assertEquals(100L, m2.getChildren().get(1).getDurationNanos());
    }

    @Test
    public void testTopLevelCollectingMeasurement() {
        final CollectingDataSink dataSink = new CollectingDataSink();
        final NSysMonApi sysMon = createSysMon(dataSink);

        // top-level collecting measurement is permitted, but results are discarded
        sysMon.startCollectingMeasurement("a").finish();
        assertEquals (0, dataSink.data.size());
    }

    @Test
    public void testTopLevelNestedCollectingMeasurement() {
        final CollectingDataSink dataSink = new CollectingDataSink();
        final NSysMonApi sysMon = createSysMon(dataSink);

        final ACollectingMeasurement a = sysMon.startCollectingMeasurement("a");
        final ACollectingMeasurement b = sysMon.startCollectingMeasurement("b");
        assertTrue (dataSink.data.isEmpty ());
        a.finish();
        assertTrue (dataSink.data.isEmpty ());
        b.finish();
        assertTrue (dataSink.data.isEmpty ());
    }

    @Test
    public void testTopLevelCollectingAndSimpleMeasurement() {
        final CollectingDataSink dataSink = new CollectingDataSink();
        final NSysMonApi sysMon = createSysMon(dataSink);

        // top-level collecting measurements are permitted, but their data is immediately discarded

        final ACollectingMeasurement coll = sysMon.startCollectingMeasurement("a");
        assertEquals(0, dataSink.data.size());

        final ASimpleMeasurement m = sysMon.start("m");
        m.finish();

        assertEquals(1, dataSink.data.size());
        assertEquals("m", dataSink.data.get(0).getRootNode().getIdentifier());

        coll.finish();
    }

    @Test
    public void testImplicitCloseChildAndDescendants() {
        final CollectingDataSink dataSink = new CollectingDataSink();
        final NSysMonApi sysMon = createSysMon(dataSink);

        final ASimpleMeasurement m = sysMon.start("m");
        sysMon.start("a");
        sysMon.start("b");

        // a and b were not finished explicitly
        m.finish();

        assertEquals(1, dataSink.data.size());
        assertEquals(1, dataSink.data.get(0).getRootNode().getChildren().size());
        assertEquals(1, dataSink.data.get(0).getRootNode().getChildren().get(0).getChildren().size());
    }

    @Test
    public void testMemoryLeak() {
        final CountingDataSink dataSink = new CountingDataSink();
        final NSysMonApi sysMon = createSysMon(dataSink);

        for(int i=0; i<10_000; i++) {
            sysMon.start("a");
        }

        assertEquals(100, dataSink.started);
        assertEquals(99, dataSink.finished);
        assertEquals(103, CountingLoggerFactory.logger.numError);
    }

    @Test
    public void testFlow() {
        final CollectingDataSink dataSink = new CollectingDataSink();
        final NSysMonApi sysMon = createSysMon(dataSink);

//        try {
            sysMon.startFlow(new ACorrelationId("a", "a"));
//            fail("exception expected");
//        } catch (IllegalStateException e) {
//        }

//        try {
            sysMon.joinFlow(new ACorrelationId("a", "a"));
//            fail("exception expected");
//        } catch (IllegalStateException e) {
//        }

        final ASimpleMeasurement m = sysMon.start("m");

        sysMon.startFlow(new ACorrelationId("a", "a"));
        sysMon.startFlow(new ACorrelationId("b", "b"));
        sysMon.startFlow(new ACorrelationId("a", "a"));

        sysMon.joinFlow(new ACorrelationId("c", "c"));
        sysMon.joinFlow(new ACorrelationId("d", "d"));
        sysMon.joinFlow(new ACorrelationId("c", "c"));

        m.finish();

        assertEquals(1, dataSink.data.size());

        final AHierarchicalDataRoot root = dataSink.data.get(0);
        assertEquals(2, root.getStartedFlows().size());
        assertEquals(2, root.getJoinedFlows().size());

        assertTrue(root.getStartedFlows().contains(new ACorrelationId("a", "a")));
        assertTrue(root.getStartedFlows().contains(new ACorrelationId("b", "b")));

        assertTrue(root.getJoinedFlows().contains(new ACorrelationId("c", "c")));
        assertTrue(root.getJoinedFlows().contains(new ACorrelationId("d", "d")));
    }

    @Test
    @Ignore
    public void testLoadCleanup() throws InterruptedException {
        final long start = System.currentTimeMillis();
        final int NUM_THREADS = 10;
        final CountDownLatch latch = new CountDownLatch(2* NUM_THREADS);
        for(int t = 0; t<NUM_THREADS; t++) {
            new Thread() {
                @Override public void run() {
                    for(int i=0; i<100_000; i++) {
                        createHierarchy(5, 5);
                    }
                    System.out.println("s: " + (System.currentTimeMillis() - start) + "ms");
                    latch.countDown();
                }
            }.start();

            new Thread() {
                @Override public void run() {
                    for(int i=0; i<100_000_000; i++) {
                        NSysMon.get().startCollectingMeasurement("xyz");
                    }
                    System.out.println("c: "+ (System.currentTimeMillis() - start) + "ms");
                    latch.countDown();
                }
            }.start();
        }
        latch.await();
    }

    private void createHierarchy(int width, int depth) {
        for(int i=0; i<width; i++) {
            NSysMon.get().start("a-" + i + "-" + depth);
            if(depth > 0) {
                createHierarchy(width, depth-1);
            }
        }
    }
}

//TODO test top-level parallel measurement: should be rejected
