package com.falconjk.mylibrary.logdb.core;

import android.util.Log;

import com.falconjk.mylibrary.logdb.Timber;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 處理日誌調用的外觀。通過 {@link Timber#plant} 安裝實例。
 */
public abstract class Tree {
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

    /**
     * 將日誌消息寫入其目的地。默認情況下，所有特定級別的方法都會調用此方法。
     *
     * @param priority 日誌級別。請參閱 {@link Log} 中的常量。
     * @param tag      顯式或推斷的標籤。可能為 {@code null}。
     * @param message  格式化的日誌消息。
     * @param t        隨附的異常。可能為 {@code null}。
     */
    protected abstract void log(int priority, @Nullable String tag, String link, @NotNull String message, @Nullable Throwable t, StackTraceElement[] stackTraces);
}