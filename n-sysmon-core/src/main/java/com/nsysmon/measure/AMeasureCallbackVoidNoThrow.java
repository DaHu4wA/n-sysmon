package com.nsysmon.measure;

/**
 * @author arno
 */
public interface AMeasureCallbackVoidNoThrow extends AMeasureCallbackVoid <RuntimeException> {
    @Override void call(AWithParameters m);
}
