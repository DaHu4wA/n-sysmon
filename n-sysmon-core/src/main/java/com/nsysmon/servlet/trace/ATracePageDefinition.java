package com.nsysmon.servlet.trace;

import com.nsysmon.NSysMonApi;
import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.impl.NSysMonConfigurer;
import com.nsysmon.measure.scalar.AJmxGcMeasurer;
import com.nsysmon.servlet.performance.AAbstractNsysmonPerformancePageDef;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author arno
 */
public class ATracePageDefinition extends AAbstractNsysmonPerformancePageDef {
    private final ATraceFilter filter;
    private final ATraceCollectingDataSink collector;

    private static final List<ColDef> colDefs = Arrays.asList(
            new ColDef("%", true, 1, ColWidth.Medium),
            new ColDef("total µs", false, 0, ColWidth.Long),
            new ColDef("self µs", false, 0, ColWidth.Long),
            new ColDef("start @", false, 0, ColWidth.Long)
    );


    public ATracePageDefinition(ATraceFilter traceFilter, int bufferSize) {
        this.filter = traceFilter;
        this.collector = new ATraceCollectingDataSink(traceFilter, bufferSize);
    }

    @Override public void init(NSysMonApi sysMon) {
        super.init(sysMon);
        NSysMonConfigurer.addDataSink(sysMon, collector);
    }

    @Override public String getId() {
        return filter.getId();
    }

    @Override public String getShortLabel() {
        return filter.getShortLabel();
    }

    @Override public String getFullLabel() {
        return filter.getFullLabel();
    }

    @Override protected void doStartMeasurements() {
        collector.isStarted = true;
    }

    @Override protected void doStopMeasurements() {
        collector.isStarted = false;
    }

    @Override protected void doClearMeasurements() {
        collector.clear();
    }

    @Override protected boolean isStarted() {
        return collector.isStarted;
    }

    @Override protected List<ColDef> getColDefs() {
        return colDefs;
    }

    @Override protected List<TreeNode> getData() {
        final List<TreeNode> result = new ArrayList<>();

        for(AHierarchicalDataRoot root: collector.getData()) {
            result.add(asTreeNode(root.getRootNode(), root.getUuid().toString(), System.currentTimeMillis(), root.getRootNode().getDurationNanos(), 0, root.isKilled()));
        }

        Collections.sort(result, (o1, o2) -> (int) (o2.colDataRaw[3] - o1.colDataRaw[3]));

        return result;
    }

    private TreeNode asTreeNode(AHierarchicalData node, String id, long now, long parentNanos, int level, boolean wasKilled) {
        final List<TreeNode> children = new ArrayList<>();
        long selfNanos = node.getDurationNanos();

        final long childNow = level > 0 ? now : node.getStartTimeMillis();

        int i=0;
        for(AHierarchicalData child: node.getChildren()) {
            if(child.isSerial()) {
                selfNanos -= child.getDurationNanos();
            }
            children.add(asTreeNode(child, String.valueOf(i), childNow, node.getDurationNanos(), level+1, wasKilled));
            i++;
        }

        if(selfNanos < 0) selfNanos = 0;
        if(selfNanos > node.getDurationNanos()) selfNanos = node.getDurationNanos();

        if(selfNanos != 0 && children.size() > 0) {
            children.add(0, new TreeNode("<self>", true, new long[]{selfNanos * 1000 / node.getDurationNanos(), selfNanos / 1000, selfNanos / 1000, node.getStartTimeMillis() - childNow}, Collections.<TreeNode>emptyList()));
        }


        final long[] colDataRaw = new long[] {
                node.getDurationNanos() * 100 * 10 / (parentNanos == 0 ? 1 : parentNanos), // 100 for '%', 10 for 1 frac digit
                node.getDurationNanos() / 1000,
                selfNanos / 1000,
                node.getStartTimeMillis() - now
        };

        return new TreeNode(id, node.getIdentifier(), tooltipFor(node), node.isSerial(), colDataRaw, children, wasKilled);
    }

    private List<List<String>> tooltipFor(AHierarchicalData node) {
        if(node.getParameters().isEmpty()) {
            return null;
        }

        if(isGarbageCollectionNode(node)) {
            return gcTooltipFor(node);
        }

        final List<List<String>> result = new ArrayList<>();

        for(String key: new TreeSet<>(node.getParameters().keySet())) {
            result.add(Arrays.asList(key, node.getParameters().get(key)));
        }

        return result;
    }

    private List<List<String>> gcTooltipFor(AHierarchicalData node) {
        final List<List<String>> result = new ArrayList<>();

        final SortedSet<String> memKinds = new TreeSet<>();

        for(String key: new TreeSet<>(node.getParameters().keySet())) {
            if(key.startsWith(AJmxGcMeasurer.KEY_PREFIX_MEM)) {
                memKinds.add(key.split(":")[1]);
                continue;
            }

            result.add(Arrays.asList(key, node.getParameters().get(key)));
        }

        final NumberFormat nf = new DecimalFormat("0.0");
        final NumberFormat nfPos = new DecimalFormat("+0.0;-0.0");
        for(String memKind: memKinds) {
            final String usedAfter      = nf.   format(Long.valueOf(node.getParameters().get(AJmxGcMeasurer.getUsedAfterKey(memKind))) / 1024.0 / 1024.0);
            final String committedAfter = nf.   format(Long.valueOf(node.getParameters().get(AJmxGcMeasurer.getCommittedAfterKey(memKind))) / 1024.0 / 1024.0);
            final String usedDelta      = nfPos.format(Long.valueOf(node.getParameters().get(AJmxGcMeasurer.getUsedDeltaKey(memKind))) / 1024.0 / 1024.0);
            final String committedDelta = nfPos.format(Long.valueOf(node.getParameters().get(AJmxGcMeasurer.getCommittedDeltaKey(memKind))) / 1024.0 / 1024.0);
            final String memValue = usedAfter + "MB (" + usedDelta + ") / " + committedAfter + "MB (" + committedDelta + ")";
            result.add(Arrays.asList(memKind, memValue));
        }


        return result;
    }

    private boolean isGarbageCollectionNode(AHierarchicalData node) {
        return node.getParameters().containsKey(AJmxGcMeasurer.KEY_ID);
    }
}
