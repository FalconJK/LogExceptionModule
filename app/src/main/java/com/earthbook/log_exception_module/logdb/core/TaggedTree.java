package com.earthbook.log_exception_module.logdb.core;

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
        StackInfo stackInfo = new StackInfo(tag, TimberUtil.getLogLink());
        TimberProcessor.getInstance().processLog(new LogEntry(Log.VERBOSE, stackInfo, formatArgs(message, args), null, new Throwable().getStackTrace()));
    }

    @Override
    public void v(@Nullable Throwable t, @Nullable String message, Object... args) {
        StackInfo stackInfo = new StackInfo(tag, TimberUtil.getLogLink());
        TimberProcessor.getInstance().processLog(new LogEntry(Log.VERBOSE, stackInfo, formatArgs(message, args), t, new Throwable().getStackTrace()));
    }

    @Override
    public void v(@Nullable Throwable t) {
        StackInfo stackInfo = new StackInfo(tag, TimberUtil.getLogLink());
        TimberProcessor.getInstance().processLog(new LogEntry(Log.VERBOSE, stackInfo, null, t, new Throwable().getStackTrace()));
    }

    @Override
    public void d(@Nullable String message, Object... args) {
        StackInfo stackInfo = new StackInfo(tag, TimberUtil.getLogLink());
        TimberProcessor.getInstance().processLog(new LogEntry(Log.DEBUG, stackInfo, formatArgs(message, args), null, new Throwable().getStackTrace()));
    }

    @Override
    public void d(@Nullable Throwable t, @Nullable String message, Object... args) {
        StackInfo stackInfo = new StackInfo(tag, TimberUtil.getLogLink());
        TimberProcessor.getInstance().processLog(new LogEntry(Log.DEBUG, stackInfo, formatArgs(message, args), t, new Throwable().getStackTrace()));
    }

    @Override
    public void d(@Nullable Throwable t) {
        StackInfo stackInfo = new StackInfo(tag, TimberUtil.getLogLink());
        TimberProcessor.getInstance().processLog(new LogEntry(Log.DEBUG, stackInfo, null, t, new Throwable().getStackTrace()));
    }

    @Override
    public void i(@Nullable String message, Object... args) {
        StackInfo stackInfo = new StackInfo(tag, TimberUtil.getLogLink());
        TimberProcessor.getInstance().processLog(new LogEntry(Log.INFO, stackInfo, formatArgs(message, args), null, new Throwable().getStackTrace()));
    }

    @Override
    public void i(@Nullable Throwable t, @Nullable String message, Object... args) {
        StackInfo stackInfo = new StackInfo(tag, TimberUtil.getLogLink());
        TimberProcessor.getInstance().processLog(new LogEntry(Log.INFO, stackInfo, formatArgs(message, args), t, new Throwable().getStackTrace()));
    }

    @Override
    public void i(@Nullable Throwable t) {
        StackInfo stackInfo = new StackInfo(tag, TimberUtil.getLogLink());
        TimberProcessor.getInstance().processLog(new LogEntry(Log.INFO, stackInfo, null, t, new Throwable().getStackTrace()));
    }

    @Override
    public void w(@Nullable String message, Object... args) {
        StackInfo stackInfo = new StackInfo(tag, TimberUtil.getLogLink());
        TimberProcessor.getInstance().processLog(new LogEntry(Log.WARN, stackInfo, formatArgs(message, args), null, new Throwable().getStackTrace()));
    }

    @Override
    public void w(@Nullable Throwable t, @Nullable String message, Object... args) {
        StackInfo stackInfo = new StackInfo(tag, TimberUtil.getLogLink());
        TimberProcessor.getInstance().processLog(new LogEntry(Log.WARN, stackInfo, formatArgs(message, args), t, new Throwable().getStackTrace()));
    }

    @Override
    public void w(@Nullable Throwable t) {
        StackInfo stackInfo = new StackInfo(tag, TimberUtil.getLogLink());
        TimberProcessor.getInstance().processLog(new LogEntry(Log.WARN, stackInfo, null, t, new Throwable().getStackTrace()));
    }

    @Override
    public void e(@Nullable String message, Object... args) {
        StackInfo stackInfo = new StackInfo(tag, TimberUtil.getLogLink());
        TimberProcessor.getInstance().processLog(new LogEntry(Log.ERROR, stackInfo, formatArgs(message, args), null, new Throwable().getStackTrace()));
    }

    @Override
    public void e(@Nullable Throwable t, @Nullable String message, Object... args) {
        StackInfo stackInfo = new StackInfo(tag, TimberUtil.getLogLink());
        TimberProcessor.getInstance().processLog(new LogEntry(Log.ERROR, stackInfo, formatArgs(message, args), t, new Throwable().getStackTrace()));
    }

    @Override
    public void e(@Nullable Throwable t) {
        StackInfo stackInfo = new StackInfo(tag, TimberUtil.getLogLink());
        TimberProcessor.getInstance().processLog(new LogEntry(Log.ERROR, stackInfo, null, t, new Throwable().getStackTrace()));
    }

    @Override
    public void wtf(@Nullable String message, Object... args) {
        StackInfo stackInfo = new StackInfo(tag, TimberUtil.getLogLink());
        TimberProcessor.getInstance().processLog(new LogEntry(Log.ASSERT, stackInfo, formatArgs(message, args), null, new Throwable().getStackTrace()));
    }

    @Override
    public void wtf(@Nullable Throwable t, @Nullable String message, Object... args) {
        StackInfo stackInfo = new StackInfo(tag, TimberUtil.getLogLink());
        TimberProcessor.getInstance().processLog(new LogEntry(Log.ASSERT, stackInfo, formatArgs(message, args), t, new Throwable().getStackTrace()));
    }

    @Override
    public void wtf(@Nullable Throwable t) {
        StackInfo stackInfo = new StackInfo(tag, TimberUtil.getLogLink());
        TimberProcessor.getInstance().processLog(new LogEntry(Log.ASSERT, stackInfo, null, t, new Throwable().getStackTrace()));
    }

    @Override
    public void log(int priority, @Nullable String message, Object... args) {
        StackInfo stackInfo = new StackInfo(tag, TimberUtil.getLogLink());
        TimberProcessor.getInstance().processLog(new LogEntry(priority, stackInfo, formatArgs(message, args), null, new Throwable().getStackTrace()));
    }

    @Override
    public void log(int priority, @Nullable Throwable t, @Nullable String message, Object... args) {
        StackInfo stackInfo = new StackInfo(tag, TimberUtil.getLogLink());
        TimberProcessor.getInstance().processLog(new LogEntry(priority, stackInfo, formatArgs(message, args), t, new Throwable().getStackTrace()));
    }

    @Override
    public void log(int priority, @Nullable Throwable t) {
        StackInfo stackInfo = new StackInfo(tag, TimberUtil.getLogLink());
        TimberProcessor.getInstance().processLog(new LogEntry(priority, stackInfo, null, t, new Throwable().getStackTrace()));
    }

    private String formatArgs(@Nullable String message, Object... args) {
        if (message == null || message.isEmpty() || args == null || args.length == 0) {
            return message;
        }
        return String.format(message, args);
    }
}
