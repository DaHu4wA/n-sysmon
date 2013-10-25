package com.ajjpj.asysmon.config;

import com.ajjpj.asysmon.measure.global.AGlobalMeasurer;
import com.ajjpj.asysmon.datasink.ADataSink;
import com.ajjpj.asysmon.timer.ATimer;

import java.util.List;

/**
 * @author arno
 */
public interface ASysMonConfig {
    ATimer getTimer();
    List<? extends ADataSink> getHandlers();
    List<? extends AGlobalMeasurer> getGlobalMeasurers();
}
