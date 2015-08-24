package com.nsysmon.testutil;

import com.ajjpj.afoundation.function.AFunction0NoThrow;
import com.nsysmon.config.log.NSysMonLogger;


/**
 * @author arno
 */
public class CountingLogger extends NSysMonLogger {
    public int numDebug = 0;
    public int numInfo = 0;
    public int numWarn = 0;
    public int numError = 0;

    public void reset() {
        numDebug = 0;
        numInfo = 0;
        numWarn = 0;
        numError = 0;
    }

    @Override public void debug(AFunction0NoThrow<String> msg) {
        numDebug += 1;
    }

    @Override public void info(String msg) {
        numInfo += 1;
    }

    @Override public void warn(String msg) {
        numWarn += 1;
    }

    @Override public void warn(String msg, Exception exc) {
        warn(msg);
    }

    @Override public void error(String s) {
        numError += 1;
    }

    @Override public void error(Exception exc) {
        numError += 1;
    }

    @Override public void error(String msg, Exception exc) {
        error(msg);
    }
}
