package com.nsysmon.measure.scalar;

import com.ajjpj.afoundation.io.AFile;
import com.ajjpj.afoundation.proc.CliCommand;
import com.nsysmon.data.AScalarDataPoint;
import com.nsysmon.measure.environment.impl.AFileSystemsEnvironmentMeasurer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author arno
 */
public class AProcDiskstatsMeasurer implements AScalarMeasurer {
    public static final AFile PROC_DISKSTATS = new AFile("/proc/diskstats", Charset.defaultCharset());

    public static final String KEY_PREFIX = "disk:";
    public static final String KEY_MEMENTO = KEY_PREFIX;

    public static final String KEY_SUFFIX_SIZE = ":sizeGB";
    public static final String KEY_SUFFIX_USED = ":usedGB";
    public static final String KEY_SUFFIX_AVAILABLE = ":availableGB";
    public static final String KEY_SUFFIX_READ_SECTORS = ":read-sectors";
    public static final String KEY_SUFFIX_WRITTEN_SECTORS = ":written-sectors";
    public static final String KEY_SUFFIX_READ_MBYTES = ":read-mbytes";
    public static final String KEY_SUFFIX_WRITTEN_MBYTES = ":written-mbytes";
    public static final String KEY_SUFFIX_IOS_IN_PROGRESS = ":ios-in-progress";

    public static final String KEY_MOUNTPOINT = ":mountpoint:";

    private final boolean isWindows;

    public AProcDiskstatsMeasurer(){
        isWindows = System.getProperty("os.name").toLowerCase().contains("win");
    }

    @Override public void prepareMeasurements(Map<String, Object> mementos) throws Exception {
        //this measurement isn't working on windows
        if (isWindows){
            return;
        }
        mementos.put(KEY_MEMENTO, createSnapshot());
    }

    @Override public void contributeMeasurements(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) throws Exception {
        //this measurement isn't working on windows
        if (isWindows){
            return;
        }
        contributeDiskSize(data, timestamp);
        contributeTraffic(data, timestamp, mementos);
        contributeMountPoints(data, timestamp);
    }

    static void contributeMountPoints(Map<String, AScalarDataPoint> data, long timestamp) throws Exception {
        for(Map.Entry<String, String> entry: AFileSystemsEnvironmentMeasurer.getMountPoints().entrySet()) {
            add(data, timestamp, getMountPointKey(entry.getKey(), entry.getValue()), 1, 0);
        }
    }

    static void contributeTraffic(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) throws Exception {
        final Snapshot prev = (Snapshot) mementos.get(KEY_MEMENTO);
        final Snapshot current = createSnapshot();

        final long diffTime = current.timestamp - prev.timestamp;

        for(String dev: current.sectorsRead.keySet()) {
            if(! prev.sectorsRead.containsKey(dev)) {
                continue; // deal with dynamically added or removed devices
            }

            if(current.sectorsRead.get(dev) == 0 && current.sectorsWritten.get(dev) == 0) {
                continue; // filter out 'unused' devices, e.g. RAM disks
            }

            final long sectorsReadRaw = current.sectorsRead.get(dev) - prev.sectorsRead.get(dev);
            final long sectorsWrittenRaw = current.sectorsWritten.get(dev) - prev.sectorsWritten.get(dev);

            final long sectorsRead    = sectorsReadRaw    * 10*1000 / diffTime;
            final long sectorsWritten = sectorsWrittenRaw * 10*1000 / diffTime;

            final int blockSize = physicalBlockSize(dev);
            final long mBytesRead = sectorsReadRaw * blockSize * 100 * 1000 / diffTime / 1024 / 1024;
            final long mBytesWritten = sectorsWrittenRaw * blockSize * 100 * 1000 / diffTime / 1024 / 1024;

            final long iosInProgress = current.iosInProgress.get(dev);

            add(data, timestamp, getReadSectorsKey(dev), sectorsRead, 1);
            add(data, timestamp, getWrittenSectorsKey(dev), sectorsWritten, 1);
            add(data, timestamp, getReadMbytesKey(dev), mBytesRead, 2);
            add(data, timestamp, getWrittenMbytesKey(dev), mBytesWritten, 2);
            add(data, timestamp, getIosInProgressKey(dev), iosInProgress, 0);
        }
    }

    static void contributeDiskSize(Map<String, AScalarDataPoint> data, long timestamp) throws Exception {
        final List<String> df = new CliCommand("df", "-P").getOutput();
        for(String line: df) {
            if(! line.startsWith("/dev/")) {
                continue;
            }

            final String[] split = line.split("\\s+");
            if(split.length < 6) {
                continue;
            }

            final String dev = split[0].substring(5);
            final long sizeKb = Long.valueOf(split[1]);
            final long usedKb = Long.valueOf(split[2]);
            final long availableKb = Long.valueOf(split[3]);
//            final String mountPoint = split[5];

            add(data, timestamp, getSizeKey(dev), sizeKb * 100 / (1024*1024), 2);
            add(data, timestamp, getUsedKey(dev), usedKb * 100 / (1024*1024), 2);
            add(data, timestamp, getAvailableKey(dev), availableKb * 100 / (1024*1024), 2);
        }
    }


    static int physicalBlockSize(String dev) throws IOException {
        while (! dev.isEmpty()) {
            final File f = new File("/sys/block/" + dev + "/queue/physical_block_size");
            if(f.exists()) {
                return Integer.valueOf(new AFile(f, Charset.defaultCharset()).lines().get(0));
            }
            dev = dev.substring(0, dev.length()-1);
        }
        return 512;
    }

    private static void add(Map<String, AScalarDataPoint> data, long timestamp, String key, long value, int numFracDigits) {
        data.put(key, new AScalarDataPoint(timestamp, key, value, numFracDigits));
    }

    public static String getMountPointKey(String dev, String mountPoint) {
        return KEY_PREFIX + dev + KEY_MOUNTPOINT + mountPoint;
    }

    public static String getSizeKey(String dev) {
        return KEY_PREFIX + dev + KEY_SUFFIX_SIZE;
    }

    public static String getUsedKey(String dev) {
        return KEY_PREFIX + dev + KEY_SUFFIX_USED;
    }

    public static String getAvailableKey(String dev) {
        return KEY_PREFIX + dev + KEY_SUFFIX_AVAILABLE;
    }

    public static String getReadSectorsKey(String dev) {
        return KEY_PREFIX + dev + KEY_SUFFIX_READ_SECTORS;
    }

    public static String getWrittenSectorsKey(String dev) {
        return KEY_PREFIX + dev + KEY_SUFFIX_WRITTEN_SECTORS;
    }

    public static String getReadMbytesKey(String dev) {
        return KEY_PREFIX + dev + KEY_SUFFIX_READ_MBYTES;
    }

    public static String getWrittenMbytesKey(String dev) {
        return KEY_PREFIX + dev + KEY_SUFFIX_WRITTEN_MBYTES;
    }

    public static String getIosInProgressKey(String dev) {
        return KEY_PREFIX + dev + KEY_SUFFIX_IOS_IN_PROGRESS;
    }

    private static Snapshot createSnapshot() throws IOException {
        return createSnapshot(PROC_DISKSTATS.lines());
    }

    static Snapshot createSnapshot(Iterable<String> source) throws IOException {
        final Snapshot result = new Snapshot();

        for(String line: source) {
            final String[] split = line.trim().split("\\s+");

            final String dev = split[2];
            final long sectorsRead = Long.valueOf(split[2+3]);
            final long sectorsWritten = Long.valueOf(split[2+7]);
            final int iosInProgress = Integer.valueOf(split[2+9]);

            result.sectorsRead.put(dev, sectorsRead);
            result.sectorsWritten.put(dev, sectorsWritten);
            result.iosInProgress.put(dev, iosInProgress);
        }

        return result;
    }

    @Override public void shutdown() throws Exception {
    }

    static class Snapshot {
        final long timestamp = System.currentTimeMillis();
        final Map<String, Long> sectorsRead = new HashMap<String, Long>();
        final Map<String, Long> sectorsWritten = new HashMap<String, Long>();
        final Map<String, Integer> iosInProgress = new HashMap<String, Integer>();
    }
}
