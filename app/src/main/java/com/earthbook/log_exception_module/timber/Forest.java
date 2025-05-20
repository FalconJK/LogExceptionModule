package com.earthbook.log_exception_module.timber;

import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 管理所有日誌樹的類
 */
public final class Forest {

    private Forest() {
        throw new AssertionError("No instances.");
    }

    /**
     * 所有已植入樹的線程安全列表。
     */
    private static final CopyOnWriteArrayList<Tree> FOREST = new CopyOnWriteArrayList<>();

    /**
     * 記錄 VERBOSE 級別的消息
     */
    public static void v(@Nullable String message, Object... args) {
        String tag = TimberUtil.createStackElementTag();
        TimberProcessor.getInstance().processLog(new LogEntry(Log.VERBOSE, tag, formatArgs(message, args), null));
    }

    /**
     * 記錄 VERBOSE 級別的異常和消息
     */
    public static void v(@Nullable Throwable t, @Nullable String message, Object... args) {
        String tag = TimberUtil.createStackElementTag();
        TimberProcessor.getInstance().processLog(new LogEntry(Log.VERBOSE, tag, formatArgs(message, args), t));
    }

    /**
     * 記錄 VERBOSE 級別的異常
     */
    public static void v(@Nullable Throwable t) {
        String tag = TimberUtil.createStackElementTag();
        TimberProcessor.getInstance().processLog(new LogEntry(Log.VERBOSE, tag, null, t));
    }

    /**
     * 記錄 DEBUG 級別的消息
     */
    public static void d(@Nullable String message, Object... args) {
        String tag = TimberUtil.createStackElementTag();
        TimberProcessor.getInstance().processLog(new LogEntry(Log.DEBUG, tag, formatArgs(message, args), null));
    }

    /**
     * 記錄 DEBUG 級別的異常和消息
     */
    public static void d(@Nullable Throwable t, @Nullable String message, Object... args) {
        String tag = TimberUtil.createStackElementTag();
        TimberProcessor.getInstance().processLog(new LogEntry(Log.DEBUG, tag, formatArgs(message, args), t));
    }

    /**
     * 記錄 DEBUG 級別的異常
     */
    public static void d(@Nullable Throwable t) {
        String tag = TimberUtil.createStackElementTag();
        TimberProcessor.getInstance().processLog(new LogEntry(Log.DEBUG, tag, null, t));
    }

    /**
     * 記錄 INFO 級別的消息
     */
    public static void i(@Nullable String message, Object... args) {
        String tag = TimberUtil.createStackElementTag();
        TimberProcessor.getInstance().processLog(new LogEntry(Log.INFO, tag, formatArgs(message, args), null));
    }

    /**
     * 記錄 INFO 級別的異常和消息
     */
    public static void i(@Nullable Throwable t, @Nullable String message, Object... args) {
        String tag = TimberUtil.createStackElementTag();
        TimberProcessor.getInstance().processLog(new LogEntry(Log.INFO, tag, formatArgs(message, args), t));
    }

    /**
     * 記錄 INFO 級別的異常
     */
    public static void i(@Nullable Throwable t) {
        String tag = TimberUtil.createStackElementTag();
        TimberProcessor.getInstance().processLog(new LogEntry(Log.INFO, tag, null, t));
    }

    /**
     * 記錄 WARNING 級別的消息
     */
    public static void w(@Nullable String message, Object... args) {
        String tag = TimberUtil.createStackElementTag();
        TimberProcessor.getInstance().processLog(new LogEntry(Log.WARN, tag, formatArgs(message, args), null));
    }

    /**
     * 記錄 WARNING 級別的異常和消息
     */
    public static void w(@Nullable Throwable t, @Nullable String message, Object... args) {
        String tag = TimberUtil.createStackElementTag();
        TimberProcessor.getInstance().processLog(new LogEntry(Log.WARN, tag, formatArgs(message, args), t));
    }

    /**
     * 記錄 WARNING 級別的異常
     */
    public static void w(@Nullable Throwable t) {
        String tag = TimberUtil.createStackElementTag();
        TimberProcessor.getInstance().processLog(new LogEntry(Log.WARN, tag, null, t));
    }

    /**
     * 記錄 ERROR 級別的消息
     */
    public static void e(@Nullable String message, Object... args) {
        String tag = TimberUtil.createStackElementTag();
        TimberProcessor.getInstance().processLog(new LogEntry(Log.ERROR, tag, formatArgs(message, args), null));
    }

    /**
     * 記錄 ERROR 級別的異常和消息
     */
    public static void e(@Nullable Throwable t, @Nullable String message, Object... args) {
        String tag = TimberUtil.createStackElementTag();
        TimberProcessor.getInstance().processLog(new LogEntry(Log.ERROR, tag, formatArgs(message, args), t));
    }

    /**
     * 記錄 ERROR 級別的異常
     */
    public static void e(@Nullable Throwable t) {
        String tag = TimberUtil.createStackElementTag();
        TimberProcessor.getInstance().processLog(new LogEntry(Log.ERROR, tag, null, t));
    }

    /**
     * 記錄 ASSERT 級別的消息
     */
    public static void wtf(@Nullable String message, Object... args) {
        String tag = TimberUtil.createStackElementTag();
        TimberProcessor.getInstance().processLog(new LogEntry(Log.ASSERT, tag, formatArgs(message, args), null));
    }

    /**
     * 記錄 ASSERT 級別的異常和消息
     */
    public static void wtf(@Nullable Throwable t, @Nullable String message, Object... args) {
        String tag = TimberUtil.createStackElementTag();
        TimberProcessor.getInstance().processLog(new LogEntry(Log.ASSERT, tag, formatArgs(message, args), t));
    }

    /**
     * 記錄 ASSERT 級別的異常
     */
    public static void wtf(@Nullable Throwable t) {
        String tag = TimberUtil.createStackElementTag();
        TimberProcessor.getInstance().processLog(new LogEntry(Log.ASSERT, tag, null, t));
    }

    /**
     * 記錄指定優先級的消息
     */
    public static void log(int priority, @Nullable String message, Object... args) {
        String tag = TimberUtil.createStackElementTag();
        TimberProcessor.getInstance().processLog(new LogEntry(priority, tag, formatArgs(message, args), null));
    }

    /**
     * 記錄指定優先級的異常和消息
     */
    public static void log(int priority, @Nullable Throwable t, @Nullable String message, Object... args) {
        String tag = TimberUtil.createStackElementTag();
        TimberProcessor.getInstance().processLog(new LogEntry(priority, tag, formatArgs(message, args), t));
    }

    /**
     * 記錄指定優先級的異常
     */
    public static void log(int priority, @Nullable Throwable t) {
        String tag = TimberUtil.createStackElementTag();
        TimberProcessor.getInstance().processLog(new LogEntry(priority, tag, null, t));
    }

    private static String formatArgs(@Nullable String message, Object... args) {
        if (message == null || message.isEmpty() || args == null || args.length == 0) {
            return message;
        }
        return String.format(message, args);
    }

    /**
     * 設置一次性標籤用於下一次日誌調用
     */
    public static LoggerInterface tag(String tag) {
        return new TaggedTree(tag);
    }

    /**
     * 添加一個新的日誌樹
     */
    public static void plant(Tree tree) {
        if (tree == null) {
            throw new NullPointerException("tree == null");
        }
        FOREST.add(tree);
    }

    /**
     * 添加多個新的日誌樹
     */
    public static void plant(Tree... trees) {
        if (trees == null) {
            throw new NullPointerException("trees == null");
        }
        for (Tree tree : trees) {
            if (tree == null) {
                throw new NullPointerException("trees contains null");
            }
            FOREST.add(tree);
        }
    }

    /**
     * 移除一個已植入的日誌樹
     */
    public static void uproot(Tree tree) {
        if (tree == null) {
            throw new NullPointerException("tree == null");
        }
        if (!FOREST.remove(tree)) {
            throw new IllegalArgumentException("Cannot uproot tree which is not planted: " + tree);
        }
    }

    /**
     * 移除所有已植入的日誌樹
     */
    public static void uprootAll() {
        FOREST.clear();
    }

    /**
     * 返回所有已植入的 {@linkplain Tree 樹} 的副本。
     */
    @NotNull
    public static List<Tree> forest() {
        return Collections.unmodifiableList(new ArrayList<>(FOREST));
    }

    /**
     * 返回所有已植入的 {@linkplain Tree 樹} 的數組。
     */
    @NotNull
    static Tree[] forestAsArray() {
        return FOREST.toArray(new Tree[0]);
    }

    /**
     * 獲取已植入的樹的數量。
     */
    public static int treeCount() {
        return FOREST.size();
    }
}
