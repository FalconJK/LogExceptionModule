package com.earthbook.log_exception_module.logdb;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.earthbook.log_exception_module.logdb.activity.SessionListActivity;
import com.earthbook.log_exception_module.logdb.core.Forest;
import com.earthbook.log_exception_module.logdb.core.LoggerInterface;
import com.earthbook.log_exception_module.logdb.core.TimberProcessor;
import com.earthbook.log_exception_module.logdb.core.Tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 懶人的日誌記錄。
 */
public final class Timber{

    private Timber() {
        throw new AssertionError("No instances.");
    }

    /**
     * 清理所有訂閱
     */
    public static void clearDisposables() {
        TimberProcessor.getInstance().clearDisposables();
    }

    /**
     * 植入一個新的日誌樹
     */
    public static void plant(Tree tree) {
        Forest.plant(tree);
    }

    /**
     * 植入多個日誌樹
     */
    public static void plant(Tree... trees) {
        Forest.plant(trees);
    }

    /**
     * 移除一個已植入的日誌樹
     */
    public static void uproot(Tree tree) {
        Forest.uproot(tree);
    }

    /**
     * 移除所有已植入的日誌樹
     */
    public static void uprootAll() {
        Forest.uprootAll();
    }

    /**
     * 設置一次性標籤用於下一次日誌調用
     */
    public static LoggerInterface tag(String tag) {
        return Forest.tag(tag);
    }

    /**
     * 記錄 VERBOSE 級別的消息
     */
    public static void v(@Nullable String message, Object... args) {
        Forest.v(message, args);
    }

    /**
     * 記錄 VERBOSE 級別的異常和消息
     */
    public static void v(@Nullable Throwable t, @Nullable String message, Object... args) {
        Forest.v(t, message, args);
    }

    /**
     * 記錄 VERBOSE 級別的異常
     */
    public static void v(@Nullable Throwable t) {
        Forest.v(t);
    }

    /**
     * 記錄 DEBUG 級別的消息
     */
    public static void d(@Nullable String message, Object... args) {
        Forest.d(message, args);
    }

    /**
     * 記錄 DEBUG 級別的異常和消息
     */
    public static void d(@Nullable Throwable t, @Nullable String message, Object... args) {
        Forest.d(t, message, args);
    }

    /**
     * 記錄 DEBUG 級別的異常
     */
    public static void d(@Nullable Throwable t) {
        Forest.d(t);
    }

    /**
     * 記錄 INFO 級別的消息
     */
    public static void i(@Nullable String message, Object... args) {
        Forest.i(message, args);
    }

    /**
     * 記錄 INFO 級別的異常和消息
     */
    public static void i(@Nullable Throwable t, @Nullable String message, Object... args) {
        Forest.i(t, message, args);
    }

    /**
     * 記錄 INFO 級別的異常
     */
    public static void i(@Nullable Throwable t) {
        Forest.i(t);
    }

    /**
     * 記錄 WARNING 級別的消息
     */
    public static void w(@Nullable String message, Object... args) {
        Forest.w(message, args);
    }

    /**
     * 記錄 WARNING 級別的異常和消息
     */
    public static void w(@Nullable Throwable t, @Nullable String message, Object... args) {
        Forest.w(t, message, args);
    }

    /**
     * 記錄 WARNING 級別的異常
     */
    public static void w(@Nullable Throwable t) {
        Forest.w(t);
    }

    /**
     * 記錄 ERROR 級別的消息
     */
    public static void e(@Nullable String message, Object... args) {
        Forest.e(message, args);
    }

    /**
     * 記錄 ERROR 級別的異常和消息
     */
    public static void e(@Nullable Throwable t, @Nullable String message, Object... args) {
        Forest.e(t, message, args);
    }

    /**
     * 記錄 ERROR 級別的異常
     */
    public static void e(@Nullable Throwable t) {
        Forest.e(t);
    }

    /**
     * 記錄 ASSERT 級別的消息
     */
    public static void wtf(@Nullable String message, Object... args) {
        Forest.wtf(message, args);
    }

    /**
     * 記錄 ASSERT 級別的異常和消息
     */
    public static void wtf(@Nullable Throwable t, @Nullable String message, Object... args) {
        Forest.wtf(t, message, args);
    }

    /**
     * 記錄 ASSERT 級別的異常
     */
    public static void wtf(@Nullable Throwable t) {
        Forest.wtf(t);
    }

    /**
     * 記錄指定優先級的消息
     */
    public static void log(int priority, @Nullable String message, Object... args) {
        Forest.log(priority, message, args);
    }

    /**
     * 記錄指定優先級的異常和消息
     */
    public static void log(int priority, @Nullable Throwable t, @Nullable String message, Object... args) {
        Forest.log(priority, t, message, args);
    }

    public static void startSessionActivity(Context context) {
        Intent intent = new Intent(context, SessionListActivity.class);
        context.startActivity(intent);
    }

    public static class DbTree extends _DbTree {
        public DbTree(Application application) {
            super(application);
        }

        @Override
        protected void log(int priority, @Nullable String tag, String link, @NotNull String message, @Nullable Throwable t, StackTraceElement[] stackTraces) {
            message = String.format("(%s) %s", link, message);
            super.log(priority, tag, link, message, t, stackTraces);
        }
    }

    public static class DebugTree extends _DebugTree {
        public DebugTree() {
            super();
        }

        @Override
        protected void log(int priority, @Nullable String tag, String link, @NotNull String message, @Nullable Throwable t, StackTraceElement[] stackTraces) {
            message = String.format("(%s) %s", link, message);
            super.log(priority, tag, link, message, t, stackTraces);
        }
    }

    /**
     * 記錄指定優先級的異常
     */
    public static void log(int priority, @Nullable Throwable t) {
        Forest.log(priority, t);
    }
}
