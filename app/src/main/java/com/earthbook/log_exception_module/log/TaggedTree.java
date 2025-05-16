package com.earthbook.log_exception_module.log;

import android.util.Log;

import org.jetbrains.annotations.Nullable;

/**
 * 帶標籤的日誌樹，實現所有日誌方法
 */
public class TaggedTree implements LoggerInterface {
    private final String tag;

    TaggedTree(String tag) {
        this.tag = tag;
    }

    @Override
    public void v(@Nullable String message, Object... args) {
        TimberProcessor.getInstance().processLog(new LogEntry(Log.VERBOSE, tag, formatArgs(message, args), null));
    }

    @Override
    public void v(@Nullable Throwable t, @Nullable String message, Object... args) {
        TimberProcessor.getInstance().processLog(new LogEntry(Log.VERBOSE, tag, formatArgs(message, args), t));
    }

    @Override
    public void v(@Nullable Throwable t) {
        TimberProcessor.getInstance().processLog(new LogEntry(Log.VERBOSE, tag, null, t));
    }

    @Override
    public void d(@Nullable String message, Object... args) {
        TimberProcessor.getInstance().processLog(new LogEntry(Log.DEBUG, tag, formatArgs(message, args), null));
    }

    @Override
    public void d(@Nullable Throwable t, @Nullable String message, Object... args) {
        TimberProcessor.getInstance().processLog(new LogEntry(Log.DEBUG, tag, formatArgs(message, args), t));
    }

    @Override
    public void d(@Nullable Throwable t) {
        TimberProcessor.getInstance().processLog(new LogEntry(Log.DEBUG, tag, null, t));
    }

    @Override
    public void i(@Nullable String message, Object... args) {
        TimberProcessor.getInstance().processLog(new LogEntry(Log.INFO, tag, formatArgs(message, args), null));
    }

    @Override
    public void i(@Nullable Throwable t, @Nullable String message, Object... args) {
        TimberProcessor.getInstance().processLog(new LogEntry(Log.INFO, tag, formatArgs(message, args), t));
    }

    @Override
    public void i(@Nullable Throwable t) {
        TimberProcessor.getInstance().processLog(new LogEntry(Log.INFO, tag, null, t));
    }

    @Override
    public void w(@Nullable String message, Object... args) {
        TimberProcessor.getInstance().processLog(new LogEntry(Log.WARN, tag, formatArgs(message, args), null));
    }

    @Override
    public void w(@Nullable Throwable t, @Nullable String message, Object... args) {
        TimberProcessor.getInstance().processLog(new LogEntry(Log.WARN, tag, formatArgs(message, args), t));
    }

    @Override
    public void w(@Nullable Throwable t) {
        TimberProcessor.getInstance().processLog(new LogEntry(Log.WARN, tag, null, t));
    }

    @Override
    public void e(@Nullable String message, Object... args) {
        TimberProcessor.getInstance().processLog(new LogEntry(Log.ERROR, tag, formatArgs(message, args), null));
    }

    @Override
    public void e(@Nullable Throwable t, @Nullable String message, Object... args) {
        TimberProcessor.getInstance().processLog(new LogEntry(Log.ERROR, tag, formatArgs(message, args), t));
    }

    @Override
    public void e(@Nullable Throwable t) {
        TimberProcessor.getInstance().processLog(new LogEntry(Log.ERROR, tag, null, t));
    }

    @Override
    public void wtf(@Nullable String message, Object... args) {
        TimberProcessor.getInstance().processLog(new LogEntry(Log.ASSERT, tag, formatArgs(message, args), null));
    }

    @Override
    public void wtf(@Nullable Throwable t, @Nullable String message, Object... args) {
        TimberProcessor.getInstance().processLog(new LogEntry(Log.ASSERT, tag, formatArgs(message, args), t));
    }

    @Override
    public void wtf(@Nullable Throwable t) {
        TimberProcessor.getInstance().processLog(new LogEntry(Log.ASSERT, tag, null, t));
    }

    @Override
    public void log(int priority, @Nullable String message, Object... args) {
        TimberProcessor.getInstance().processLog(new LogEntry(priority, tag, formatArgs(message, args), null));
    }

    @Override
    public void log(int priority, @Nullable Throwable t, @Nullable String message, Object... args) {
        TimberProcessor.getInstance().processLog(new LogEntry(priority, tag, formatArgs(message, args), t));
    }

    @Override
    public void log(int priority, @Nullable Throwable t) {
        TimberProcessor.getInstance().processLog(new LogEntry(priority, tag, null, t));
    }

    private String formatArgs(@Nullable String message, Object... args) {
        if (message == null || message.isEmpty() || args == null || args.length == 0) {
            return message;
        }
        return String.format(message, args);
    }
}
