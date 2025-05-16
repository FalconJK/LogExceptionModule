package com.earthbook.log_exception_module;

import android.os.Build;
import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Logging for lazy people.
 */
public final class Timber {

    private static final List<String> TIMBER_CLASSES = List.of(
            Timber.class.getName(),
            Forest.class.getName(),
            Tree.class.getName()
    );

    private static final int MAX_TAG_LENGTH = 23;
    private static final Pattern ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$");

    // 單例 Subject 用於接收所有日誌訊息
    private static final PublishProcessor<LogEntry> logProcessor = PublishProcessor.create();

    // 用於管理所有訂閱
    private static final CompositeDisposable disposables = new CompositeDisposable();

    static {
        // 初始化 RxJava 處理流程
        disposables.add(logProcessor
                .onBackpressureBuffer()
                .flatMap(logEntry -> {
                    List<Tree> trees = Forest.forest();
                    return Flowable.fromIterable(trees)
                            .parallel()
                            .runOn(Schedulers.io())
                            .map(tree -> {
                                if (tree.isLoggable(logEntry.tag, logEntry.priority)) {
                                    tree.log(logEntry.priority, logEntry.tag, logEntry.message, logEntry.throwable);
                                }
                                return tree;
                            })
                            .sequential();
                })
                .subscribe()
        );
    }

    private Timber() {
        throw new AssertionError("No instances.");
    }

    /**
     * Log entry containing all information needed for logging
     */
    private static class LogEntry {
        final int priority;
        final String tag;
        final String message;
        final Throwable throwable;

        LogEntry(int priority, String tag, String message, Throwable throwable) {
            this.priority = priority;
            this.tag = tag;
            this.message = message;
            this.throwable = throwable;
        }
    }

    /**
     * A facade for handling logging calls. Install instances via {@link #plant}.
     */
    public static abstract class Tree {
        private final ThreadLocal<String> explicitTag = new ThreadLocal<>();

        @Nullable
        String getTag() {
            String tag = explicitTag.get();
            if (tag != null) {
                explicitTag.remove();
            }
            return tag;
        }

        /**
         * Log a verbose message with optional format args.
         */
        public void v(@Nullable String message, Object... args) {
            prepareLog(Log.VERBOSE, null, message, args);
        }

        /**
         * Log a verbose exception and a message with optional format args.
         */
        public void v(@Nullable Throwable t, @Nullable String message, Object... args) {
            prepareLog(Log.VERBOSE, t, message, args);
        }

        /**
         * Log a verbose exception.
         */
        public void v(@Nullable Throwable t) {
            prepareLog(Log.VERBOSE, t, null);
        }

        /**
         * Log a debug message with optional format args.
         */
        public void d(@Nullable String message, Object... args) {
            prepareLog(Log.DEBUG, null, message, args);
        }

        /**
         * Log a debug exception and a message with optional format args.
         */
        public void d(@Nullable Throwable t, @Nullable String message, Object... args) {
            prepareLog(Log.DEBUG, t, message, args);
        }

        /**
         * Log a debug exception.
         */
        public void d(@Nullable Throwable t) {
            prepareLog(Log.DEBUG, t, null);
        }

        /**
         * Log an info message with optional format args.
         */
        public void i(@Nullable String message, Object... args) {
            prepareLog(Log.INFO, null, message, args);
        }

        /**
         * Log an info exception and a message with optional format args.
         */
        public void i(@Nullable Throwable t, @Nullable String message, Object... args) {
            prepareLog(Log.INFO, t, message, args);
        }

        /**
         * Log an info exception.
         */
        public void i(@Nullable Throwable t) {
            prepareLog(Log.INFO, t, null);
        }

        /**
         * Log a warning message with optional format args.
         */
        public void w(@Nullable String message, Object... args) {
            prepareLog(Log.WARN, null, message, args);
        }

        /**
         * Log a warning exception and a message with optional format args.
         */
        public void w(@Nullable Throwable t, @Nullable String message, Object... args) {
            prepareLog(Log.WARN, t, message, args);
        }

        /**
         * Log a warning exception.
         */
        public void w(@Nullable Throwable t) {
            prepareLog(Log.WARN, t, null);
        }

        /**
         * Log an error message with optional format args.
         */
        public void e(@Nullable String message, Object... args) {
            prepareLog(Log.ERROR, null, message, args);
        }

        /**
         * Log an error exception and a message with optional format args.
         */
        public void e(@Nullable Throwable t, @Nullable String message, Object... args) {
            prepareLog(Log.ERROR, t, message, args);
        }

        /**
         * Log an error exception.
         */
        public void e(@Nullable Throwable t) {
            prepareLog(Log.ERROR, t, null);
        }

        /**
         * Log an assert message with optional format args.
         */
        public void wtf(@Nullable String message, Object... args) {
            prepareLog(Log.ASSERT, null, message, args);
        }

        /**
         * Log an assert exception and a message with optional format args.
         */
        public void wtf(@Nullable Throwable t, @Nullable String message, Object... args) {
            prepareLog(Log.ASSERT, t, message, args);
        }

        /**
         * Log an assert exception.
         */
        public void wtf(@Nullable Throwable t) {
            prepareLog(Log.ASSERT, t, null);
        }

        /**
         * Log at {@code priority} a message with optional format args.
         */
        public void log(int priority, @Nullable String message, Object... args) {
            prepareLog(priority, null, message, args);
        }

        /**
         * Log at {@code priority} an exception and a message with optional format args.
         */
        public void log(int priority, @Nullable Throwable t, @Nullable String message, Object... args) {
            prepareLog(priority, t, message, args);
        }

        /**
         * Log at {@code priority} an exception.
         */
        public void log(int priority, @Nullable Throwable t) {
            prepareLog(priority, t, null);
        }

        /**
         * Return whether a message at {@code priority} should be logged.
         *
         * @deprecated Use {@link #isLoggable(String, int)} instead.
         */
        @Deprecated
        protected boolean isLoggable(int priority) {
            return true;
        }

        /**
         * Return whether a message at {@code priority} or {@code tag} should be logged.
         */
        protected boolean isLoggable(@Nullable String tag, int priority) {
            return isLoggable(priority);
        }

        private void prepareLog(int priority, @Nullable Throwable t, @Nullable String message, Object... args) {
            // 自動獲取 tag
            String tag = getTag();
            if (tag == null) {
                tag = createStackElementTag();
            }

            if (!isLoggable(tag, priority)) {
                return;
            }

            if (message == null || message.isEmpty()) {
                if (t == null) {
                    return; // Swallow message if it's null and there's no throwable.
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
            logProcessor.onNext(new LogEntry(priority, tag, message, t));
        }

        /**
         * Formats a log message with optional arguments.
         */
        protected String formatMessage(@NotNull String message, @NotNull Object[] args) {
            return String.format(message, args);
        }

        private String getStackTraceString(Throwable t) {
            // Don't replace this with Log.getStackTraceString() - it hides
            // UnknownHostException, which is not what we want.
            StringWriter sw = new StringWriter(256);
            PrintWriter pw = new PrintWriter(sw, false);
            t.printStackTrace(pw);
            pw.flush();
            return sw.toString();
        }

        /**
         * Write a log message to its destination. Called for all level-specific methods by default.
         *
         * @param priority Log level. See {@link Log} for constants.
         * @param tag      Explicit or inferred tag. May be {@code null}.
         * @param message  Formatted log message.
         * @param t        Accompanying exceptions. May be {@code null}.
         */
        protected abstract void log(int priority, @Nullable String tag, @NotNull String message, @Nullable Throwable t);
    }

    /**
     * A {@link Tree} for debug builds. Automatically infers the tag from the calling class.
     */
    public static class DebugTree extends Tree {
        private static final int MAX_LOG_LENGTH = 4000;

        /**
         * Break up {@code message} into maximum-length chunks (if needed) and send to either
         * {@link Log#println(int, String, String) Log.println()} or
         * {@link Log#wtf(String, String) Log.wtf()} for logging.
         */
        @Override
        protected void log(int priority, @Nullable String tag, @NotNull String message, @Nullable Throwable t) {
            if (message.length() <= MAX_LOG_LENGTH) {
                if (priority == Log.ASSERT) {
                    Log.wtf(tag, message);
                } else {
                    Log.println(priority, tag, message);
                }
                return;
            }

            // Split by line, then ensure each line can fit into Log's maximum length.
            for (int i = 0, length = message.length(); i < length; i++) {
                int newline = message.indexOf('\n', i);
                newline = newline != -1 ? newline : length;
                do {
                    int end = Math.min(newline, i + MAX_LOG_LENGTH);
                    String part = message.substring(i, end);
                    if (priority == Log.ASSERT) {
                        Log.wtf(tag, part);
                    } else {
                        Log.println(priority, tag, part);
                    }
                    i = end;
                } while (i < newline);
                i++;
            }
        }
    }

    /**
     * Extract the tag which should be used for the message from the {@code element}.
     */
    @Nullable
    private static String createStackElementTag() {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        if (stackTrace.length <= 2) {
            return null;
        }

        // 尋找第一個非 Timber 類的調用者
        for (int i = 2; i < stackTrace.length; i++) {
            String className = stackTrace[i].getClassName();
            if (!TIMBER_CLASSES.contains(className)) {
                String tag = className.substring(className.lastIndexOf('.') + 1);
                Matcher m = ANONYMOUS_CLASS.matcher(tag);
                if (m.find()) {
                    tag = m.replaceAll("");
                }
                // Tag length limit was removed in API 26.
                if (tag.length() <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= 26) {
                    return tag;
                } else {
                    return tag.substring(0, MAX_TAG_LENGTH);
                }
            }
        }

        return null;
    }

    /**
     * 清理所有訂閱
     */
    public static void clearDisposables() {
        disposables.clear();

        // 重新初始化處理流程
        disposables.add(
                logProcessor
                        .onBackpressureBuffer()
                        .flatMap(logEntry -> {
                            List<Tree> trees = Forest.forest();
                            return Flowable.fromIterable(trees)
                                    .parallel()
                                    .runOn(Schedulers.io())
                                    .map(tree -> {
                                        if (tree.isLoggable(logEntry.tag, logEntry.priority)) {
                                            tree.log(logEntry.priority, logEntry.tag, logEntry.message, logEntry.throwable);
                                        }
                                        return tree;
                                    })
                                    .sequential();
                        })
                        .subscribe()
        );
    }

    /**
     * Global static methods.
     */
    public static final class Forest {

        private Forest() {
            throw new AssertionError("No instances.");
        }

        /**
         * A thread-safe list of all planted trees.
         */
        private static final CopyOnWriteArrayList<Tree> FOREST = new CopyOnWriteArrayList<>();

        /**
         * Log a verbose message with optional format args.
         */
        public static void v(@Nullable String message, Object... args) {
            String tag = createStackElementTag();
            logProcessor.onNext(new LogEntry(Log.VERBOSE, tag, formatArgs(message, args), null));
        }

        /**
         * Log a verbose exception and a message with optional format args.
         */
        public static void v(@Nullable Throwable t, @Nullable String message, Object... args) {
            String tag = createStackElementTag();
            logProcessor.onNext(new LogEntry(Log.VERBOSE, tag, formatArgs(message, args), t));
        }

        /**
         * Log a verbose exception.
         */
        public static void v(@Nullable Throwable t) {
            String tag = createStackElementTag();
            logProcessor.onNext(new LogEntry(Log.VERBOSE, tag, null, t));
        }

        /**
         * Log a debug message with optional format args.
         */
        public static void d(@Nullable String message, Object... args) {
            String tag = createStackElementTag();
            logProcessor.onNext(new LogEntry(Log.DEBUG, tag, formatArgs(message, args), null));
        }

        /**
         * Log a debug exception and a message with optional format args.
         */
        public static void d(@Nullable Throwable t, @Nullable String message, Object... args) {
            String tag = createStackElementTag();
            logProcessor.onNext(new LogEntry(Log.DEBUG, tag, formatArgs(message, args), t));
        }

        /**
         * Log a debug exception.
         */
        public static void d(@Nullable Throwable t) {
            String tag = createStackElementTag();
            logProcessor.onNext(new LogEntry(Log.DEBUG, tag, null, t));
        }

        /**
         * Log an info message with optional format args.
         */
        public static void i(@Nullable String message, Object... args) {
            String tag = createStackElementTag();
            logProcessor.onNext(new LogEntry(Log.INFO, tag, formatArgs(message, args), null));
        }

        /**
         * Log an info exception and a message with optional format args.
         */
        public static void i(@Nullable Throwable t, @Nullable String message, Object... args) {
            String tag = createStackElementTag();
            logProcessor.onNext(new LogEntry(Log.INFO, tag, formatArgs(message, args), t));
        }

        /**
         * Log an info exception.
         */
        public static void i(@Nullable Throwable t) {
            String tag = createStackElementTag();
            logProcessor.onNext(new LogEntry(Log.INFO, tag, null, t));
        }

        /**
         * Log a warning message with optional format args.
         */
        public static void w(@Nullable String message, Object... args) {
            String tag = createStackElementTag();
            logProcessor.onNext(new LogEntry(Log.WARN, tag, formatArgs(message, args), null));
        }

        /**
         * Log a warning exception and a message with optional format args.
         */
        public static void w(@Nullable Throwable t, @Nullable String message, Object... args) {
            String tag = createStackElementTag();
            logProcessor.onNext(new LogEntry(Log.WARN, tag, formatArgs(message, args), t));
        }

        /**
         * Log a warning exception.
         */
        public static void w(@Nullable Throwable t) {
            String tag = createStackElementTag();
            logProcessor.onNext(new LogEntry(Log.WARN, tag, null, t));
        }

        /**
         * Log an error message with optional format args.
         */
        public static void e(@Nullable String message, Object... args) {
            String tag = createStackElementTag();
            logProcessor.onNext(new LogEntry(Log.ERROR, tag, formatArgs(message, args), null));
        }

        /**
         * Log an error exception and a message with optional format args.
         */
        public static void e(@Nullable Throwable t, @Nullable String message, Object... args) {
            String tag = createStackElementTag();
            logProcessor.onNext(new LogEntry(Log.ERROR, tag, formatArgs(message, args), t));
        }

        /**
         * Log an error exception.
         */
        public static void e(@Nullable Throwable t) {
            String tag = createStackElementTag();
            logProcessor.onNext(new LogEntry(Log.ERROR, tag, null, t));
        }

        /**
         * Log an assert message with optional format args.
         */
        public static void wtf(@Nullable String message, Object... args) {
            String tag = createStackElementTag();
            logProcessor.onNext(new LogEntry(Log.ASSERT, tag, formatArgs(message, args), null));
        }

        /**
         * Log an assert exception and a message with optional format args.
         */
        public static void wtf(@Nullable Throwable t, @Nullable String message, Object... args) {
            String tag = createStackElementTag();
            logProcessor.onNext(new LogEntry(Log.ASSERT, tag, formatArgs(message, args), t));
        }

        /**
         * Log an assert exception.
         */
        public static void wtf(@Nullable Throwable t) {
            String tag = createStackElementTag();
            logProcessor.onNext(new LogEntry(Log.ASSERT, tag, null, t));
        }

        /**
         * Log at {@code priority} a message with optional format args.
         */
        public static void log(int priority, @Nullable String message, Object... args) {
            String tag = createStackElementTag();
            logProcessor.onNext(new LogEntry(priority, tag, formatArgs(message, args), null));
        }

        /**
         * Log at {@code priority} an exception and a message with optional format args.
         */
        public static void log(int priority, @Nullable Throwable t, @Nullable String message, Object... args) {
            String tag = createStackElementTag();
            logProcessor.onNext(new LogEntry(priority, tag, formatArgs(message, args), t));
        }

        /**
         * Log at {@code priority} an exception.
         */
        public static void log(int priority, @Nullable Throwable t) {
            String tag = createStackElementTag();
            logProcessor.onNext(new LogEntry(priority, tag, null, t));
        }

        private static String formatArgs(@Nullable String message, Object... args) {
            if (message == null || message.isEmpty() || args == null || args.length == 0) {
                return message;
            }
            return String.format(message, args);
        }

        /**
         * Set a one-time tag for use on the next logging call.
         */
        public static TaggedTree tag(String tag) {
            return new TaggedTree(tag);
        }

        /**
         * Add a new logging tree.
         */
        public static void plant(Tree tree) {
            if (tree == null) {
                throw new NullPointerException("tree == null");
            }
            FOREST.add(tree);
        }

        /**
         * Adds new logging trees.
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
         * Remove a planted tree.
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
         * Remove all planted trees.
         */
        public static void uprootAll() {
            FOREST.clear();
        }

        /**
         * Return a copy of all planted {@linkplain Tree trees}.
         */
        @NotNull
        public static List<Tree> forest() {
            return Collections.unmodifiableList(new ArrayList<>(FOREST));
        }

        /**
         * Return an array of all planted {@linkplain Tree trees}.
         */
        @NotNull
        static Tree[] forestAsArray() {
            return FOREST.toArray(new Tree[0]);
        }

        /**
         * Get the number of planted trees.
         */
        public static int treeCount() {
            return FOREST.size();
        }
    }

    // 在 Timber 類中添加一個新的 TaggedTree 類
    public static class TaggedTree {
        private final String tag;

        TaggedTree(String tag) {
            this.tag = tag;
        }

        /**
         * 記錄帶標籤的 VERBOSE 級別消息
         */
        public void v(@Nullable String message, Object... args) {
            logProcessor.onNext(new LogEntry(Log.VERBOSE, tag, formatArgs(message, args), null));
        }

        /**
         * 記錄帶標籤的 VERBOSE 級別異常和消息
         */
        public void v(@Nullable Throwable t, @Nullable String message, Object... args) {
            logProcessor.onNext(new LogEntry(Log.VERBOSE, tag, formatArgs(message, args), t));
        }

        /**
         * 記錄帶標籤的 VERBOSE 級別異常
         */
        public void v(@Nullable Throwable t) {
            logProcessor.onNext(new LogEntry(Log.VERBOSE, tag, null, t));
        }

        /**
         * 記錄帶標籤的 DEBUG 級別消息
         */
        public void d(@Nullable String message, Object... args) {
            logProcessor.onNext(new LogEntry(Log.DEBUG, tag, formatArgs(message, args), null));
        }

        /**
         * 記錄帶標籤的 DEBUG 級別異常和消息
         */
        public void d(@Nullable Throwable t, @Nullable String message, Object... args) {
            logProcessor.onNext(new LogEntry(Log.DEBUG, tag, formatArgs(message, args), t));
        }

        /**
         * 記錄帶標籤的 DEBUG 級別異常
         */
        public void d(@Nullable Throwable t) {
            logProcessor.onNext(new LogEntry(Log.DEBUG, tag, null, t));
        }

        /**
         * 記錄帶標籤的 INFO 級別消息
         */
        public void i(@Nullable String message, Object... args) {
            logProcessor.onNext(new LogEntry(Log.INFO, tag, formatArgs(message, args), null));
        }

        /**
         * 記錄帶標籤的 INFO 級別異常和消息
         */
        public void i(@Nullable Throwable t, @Nullable String message, Object... args) {
            logProcessor.onNext(new LogEntry(Log.INFO, tag, formatArgs(message, args), t));
        }

        /**
         * 記錄帶標籤的 INFO 級別異常
         */
        public void i(@Nullable Throwable t) {
            logProcessor.onNext(new LogEntry(Log.INFO, tag, null, t));
        }

        /**
         * 記錄帶標籤的 WARNING 級別消息
         */
        public void w(@Nullable String message, Object... args) {
            logProcessor.onNext(new LogEntry(Log.WARN, tag, formatArgs(message, args), null));
        }

        /**
         * 記錄帶標籤的 WARNING 級別異常和消息
         */
        public void w(@Nullable Throwable t, @Nullable String message, Object... args) {
            logProcessor.onNext(new LogEntry(Log.WARN, tag, formatArgs(message, args), t));
        }

        /**
         * 記錄帶標籤的 WARNING 級別異常
         */
        public void w(@Nullable Throwable t) {
            logProcessor.onNext(new LogEntry(Log.WARN, tag, null, t));
        }

        /**
         * 記錄帶標籤的 ERROR 級別消息
         */
        public void e(@Nullable String message, Object... args) {
            logProcessor.onNext(new LogEntry(Log.ERROR, tag, formatArgs(message, args), null));
        }

        /**
         * 記錄帶標籤的 ERROR 級別異常和消息
         */
        public void e(@Nullable Throwable t, @Nullable String message, Object... args) {
            logProcessor.onNext(new LogEntry(Log.ERROR, tag, formatArgs(message, args), t));
        }

        /**
         * 記錄帶標籤的 ERROR 級別異常
         */
        public void e(@Nullable Throwable t) {
            logProcessor.onNext(new LogEntry(Log.ERROR, tag, null, t));
        }

        /**
         * 記錄帶標籤的 ASSERT 級別消息
         */
        public void wtf(@Nullable String message, Object... args) {
            logProcessor.onNext(new LogEntry(Log.ASSERT, tag, formatArgs(message, args), null));
        }

        /**
         * 記錄帶標籤的 ASSERT 級別異常和消息
         */
        public void wtf(@Nullable Throwable t, @Nullable String message, Object... args) {
            logProcessor.onNext(new LogEntry(Log.ASSERT, tag, formatArgs(message, args), t));
        }

        /**
         * 記錄帶標籤的 ASSERT 級別異常
         */
        public void wtf(@Nullable Throwable t) {
            logProcessor.onNext(new LogEntry(Log.ASSERT, tag, null, t));
        }

        /**
         * 記錄帶標籤的指定優先級消息
         */
        public void log(int priority, @Nullable String message, Object... args) {
            logProcessor.onNext(new LogEntry(priority, tag, formatArgs(message, args), null));
        }

        /**
         * 記錄帶標籤的指定優先級異常和消息
         */
        public void log(int priority, @Nullable Throwable t, @Nullable String message, Object... args) {
            logProcessor.onNext(new LogEntry(priority, tag, formatArgs(message, args), t));
        }

        /**
         * 記錄帶標籤的指定優先級異常
         */
        public void log(int priority, @Nullable Throwable t) {
            logProcessor.onNext(new LogEntry(priority, tag, null, t));
        }

        private String formatArgs(@Nullable String message, Object... args) {
            if (message == null || message.isEmpty() || args == null || args.length == 0) {
                return message;
            }
            return String.format(message, args);
        }
    }


    // 在 Timber 類中添加以下靜態方法

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
    public static TaggedTree tag(String tag) {
        return new TaggedTree(tag);
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

    /**
     * 記錄指定優先級的異常
     */
    public static void log(int priority, @Nullable Throwable t) {
        Forest.log(priority, t);
    }
}
