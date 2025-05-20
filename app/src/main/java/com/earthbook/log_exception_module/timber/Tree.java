package com.earthbook.log_exception_module.timber;

import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 處理日誌調用的外觀。通過 {@link Timber#plant} 安裝實例。
 */
public abstract class Tree implements LoggerInterface {
    private final ThreadLocal<String> explicitTag = new ThreadLocal<>();

    /**
     * 獲取此樹應該在哪個調度器上執行
     * 默認返回IO調度器
     */
    protected Scheduler getScheduler() {
        return Schedulers.io();
    }

    @Nullable
    String getTag() {
        String tag = explicitTag.get();
        if (tag != null) {
            explicitTag.remove();
        }
        return tag;
    }

    @Override
    public void v(@Nullable String message, Object... args) {
        prepareLog(Log.VERBOSE, null, message, args);
    }

    @Override
    public void v(@Nullable Throwable t, @Nullable String message, Object... args) {
        prepareLog(Log.VERBOSE, t, message, args);
    }

    @Override
    public void v(@Nullable Throwable t) {
        prepareLog(Log.VERBOSE, t, null);
    }

    @Override
    public void d(@Nullable String message, Object... args) {
        prepareLog(Log.DEBUG, null, message, args);
    }

    @Override
    public void d(@Nullable Throwable t, @Nullable String message, Object... args) {
        prepareLog(Log.DEBUG, t, message, args);
    }

    @Override
    public void d(@Nullable Throwable t) {
        prepareLog(Log.DEBUG, t, null);
    }

    @Override
    public void i(@Nullable String message, Object... args) {
        prepareLog(Log.INFO, null, message, args);
    }

    @Override
    public void i(@Nullable Throwable t, @Nullable String message, Object... args) {
        prepareLog(Log.INFO, t, message, args);
    }

    @Override
    public void i(@Nullable Throwable t) {
        prepareLog(Log.INFO, t, null);
    }

    @Override
    public void w(@Nullable String message, Object... args) {
        prepareLog(Log.WARN, null, message, args);
    }

    @Override
    public void w(@Nullable Throwable t, @Nullable String message, Object... args) {
        prepareLog(Log.WARN, t, message, args);
    }

    @Override
    public void w(@Nullable Throwable t) {
        prepareLog(Log.WARN, t, null);
    }

    @Override
    public void e(@Nullable String message, Object... args) {
        prepareLog(Log.ERROR, null, message, args);
    }

    @Override
    public void e(@Nullable Throwable t, @Nullable String message, Object... args) {
        prepareLog(Log.ERROR, t, message, args);
    }

    @Override
    public void e(@Nullable Throwable t) {
        prepareLog(Log.ERROR, t, null);
    }

    @Override
    public void wtf(@Nullable String message, Object... args) {
        prepareLog(Log.ASSERT, null, message, args);
    }

    @Override
    public void wtf(@Nullable Throwable t, @Nullable String message, Object... args) {
        prepareLog(Log.ASSERT, t, message, args);
    }

    @Override
    public void wtf(@Nullable Throwable t) {
        prepareLog(Log.ASSERT, t, null);
    }

    @Override
    public void log(int priority, @Nullable String message, Object... args) {
        prepareLog(priority, null, message, args);
    }

    @Override
    public void log(int priority, @Nullable Throwable t, @Nullable String message, Object... args) {
        prepareLog(priority, t, message, args);
    }

    @Override
    public void log(int priority, @Nullable Throwable t) {
        prepareLog(priority, t, null);
    }

    /**
     * 返回是否應該記錄 {@code priority} 的消息。
     *
     * @deprecated 使用 {@link #isLoggable(String, int)} 代替。
     */
    @Deprecated
    protected boolean isLoggable(int priority) {
        return true;
    }

    /**
     * 返回是否應該記錄 {@code priority} 或 {@code tag} 的消息。
     */
    protected boolean isLoggable(@Nullable String tag, int priority) {
        return isLoggable(priority);
    }

    private void prepareLog(int priority, @Nullable Throwable t, @Nullable String message, Object... args) {
        // 自動獲取 tag
        String tag = getTag();
        if (tag == null) {
            tag = TimberUtil.createStackElementTag();
        }

        if (!isLoggable(tag, priority)) {
            return;
        }

        if (message == null || message.isEmpty()) {
            if (t == null) {
                return; // 如果消息為空且沒有異常，則不記錄。
            }
            message = getStackTraceString(t);
        } else {
            if (args != null && args.length > 0) {
                message = formatMessage(message, args);
            }
            if (t != null) {
                message += "\n" + getStackTraceString(t);
            }
        }

        // 將日誌訊息推送到 processor
        TimberProcessor.getInstance().processLog(new LogEntry(priority, tag, message, t));
    }

    /**
     * 格式化帶有可選參數的日誌消息。
     */
    protected String formatMessage(@NotNull String message, @NotNull Object[] args) {
        return String.format(message, args);
    }

    private String getStackTraceString(Throwable t) {
        // 不要用 Log.getStackTraceString() 替換這個 - 它會隱藏 UnknownHostException，這不是我們想要的。
        StringWriter sw = new StringWriter(256);
        PrintWriter pw = new PrintWriter(sw, false);
        t.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    /**
     * 將日誌消息寫入其目的地。默認情況下，所有特定級別的方法都會調用此方法。
     *
     * @param priority 日誌級別。請參閱 {@link Log} 中的常量。
     * @param tag      顯式或推斷的標籤。可能為 {@code null}。
     * @param message  格式化的日誌消息。
     * @param t        隨附的異常。可能為 {@code null}。
     */
    protected abstract void log(int priority, @Nullable String tag, @NotNull String message, @Nullable Throwable t);
}
