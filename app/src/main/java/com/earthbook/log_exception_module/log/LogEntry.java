package com.earthbook.log_exception_module.log;

/**
 * 包含所有日誌所需信息的日誌條目
 */
public class LogEntry {
    final int priority;
    final String tag;
    final String message;
    final Throwable throwable;

    public LogEntry(int priority, String tag, String message, Throwable throwable) {
        this.priority = priority;
        this.tag = tag;
        this.message = message;
        this.throwable = throwable;
    }
}
