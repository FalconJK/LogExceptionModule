package com.falconjk.mylibrary.logdb.core;

/**
 * 包含所有日誌所需信息的日誌條目
 */
public class LogEntry {
    final int priority;
    final StackInfo stackInfo;
    final String message;
    final Throwable throwable;
    final StackTraceElement[] stackTraces;

    public LogEntry(int priority, StackInfo tag, String message, Throwable throwable, StackTraceElement[] stackTraces) {
        this.priority = priority;
        this.stackInfo = tag;
        this.message = message;
        this.throwable = throwable;
        this.stackTraces = stackTraces;
    }
}
