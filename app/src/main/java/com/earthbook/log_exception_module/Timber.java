package com.earthbook.log_exception_module;

import android.app.Application;
import android.os.Build;
import android.util.Log;

import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

/**
 * 為懶人設計的日誌工具。
 */
public final class Timber {

    // 保存所有已植入的樹
    private static final CopyOnWriteArrayList<Tree> FOREST = new CopyOnWriteArrayList<>();

    // 用於管理所有 RxJava 訂閱
    private static final CompositeDisposable DISPOSABLES = new CompositeDisposable();

    // 應用程序生命週期觀察者
    private static ApplicationLifecycleObserver lifecycleObserver;

    // 應用程序上下文
    private static Application application;

    // 用於保存當前標籤的類
    private static final class TagHolder {
        @Nullable
        static String tag;
    }

    private Timber() {
        throw new AssertionError("No instances.");
    }

    /**
     * 初始化 Timber 並設置應用程序上下文
     */
    public static void init(Application app) {
        application = app;

        // 初始化生命週期觀察者
        if (lifecycleObserver == null) {
            lifecycleObserver = new ApplicationLifecycleObserver();
        }

        // 註冊內存監控
        registerMemoryMonitor(app);
    }
    /**
     * 註冊內存監控
     */
    private static void registerMemoryMonitor(Application app) {
        app.registerComponentCallbacks(new android.content.ComponentCallbacks2() {
            @Override
            public void onTrimMemory(int level) {
                if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
                    // 當系統內存極低時，清理所有可清理的資源
                    Timber.tag("Timber").d("系統內存極低，清理所有可清理的資源");
                    DISPOSABLES.clear();
                }
            }

            @Override
            public void onConfigurationChanged(android.content.res.Configuration newConfig) {
                // 配置變更時的處理
            }

            @Override
            public void onLowMemory() {
                // 低內存警告時，清理所有 Disposable
                Timber.tag("Timber").d("系統內存不足，清理所有 Disposable");
                DISPOSABLES.clear();
            }
        });
    }

    /**
     * 釋放 Timber 資源
     */
    public static void release() {
        if (lifecycleObserver != null) {
            lifecycleObserver.unregister();
            lifecycleObserver = null;
        }

        clearDisposables();
        uprootAll();
        application = null;
    }

    /**
     * 獲取應用程序上下文
     */
    public static Application getApplication() {
        return application;
    }

    /**
     * 添加一棵樹到森林中
     */
    public static void plant(Tree tree) {
        if (tree == null) {
            throw new NullPointerException("tree == null");
        }
        FOREST.add(tree);
    }

    /**
     * 添加多棵樹到森林中
     */
    public static void plant(Tree... trees) {
        if (trees == null) {
            throw new NullPointerException("trees == null");
        }
        for (Tree tree : trees) {
            if (tree == null) {
                throw new NullPointerException("tree == null");
            }
            FOREST.add(tree);
        }
    }

    /**
     * 從森林中移除一棵樹
     */
    public static void uproot(Tree tree) {
        if (tree == null) {
            throw new NullPointerException("tree == null");
        }
        FOREST.remove(tree);
    }

    /**
     * 移除所有已植入的樹
     */
    public static void uprootAll() {
        FOREST.clear();
    }

    /**
     * 清除所有 RxJava 訂閱
     */
    public static void clearDisposables() {
        DISPOSABLES.clear();
    }

    /**
     * 獲取所有已植入的樹
     */
    public static List<Tree> forest() {
        return Collections.unmodifiableList(FOREST);
    }

    /**
     * 獲取已植入的樹的數量
     */
    public static int treeCount() {
        return FOREST.size();
    }

    /**
     * 設置下一個日誌調用的標籤。
     */
    public static Tree tag(String tag) {
        TagHolder.tag = tag;
        return TREE_OF_SOULS;
    }

    /**
     * 記錄詳細(verbose)級別的訊息，可選格式化參數。
     */
    public static void v(@Nullable String message, Object... args) {
        TREE_OF_SOULS.v(message, args);
    }

    /**
     * 記錄詳細(verbose)級別的異常和訊息，可選格式化參數。
     */
    public static void v(@Nullable Throwable t, @Nullable String message, Object... args) {
        TREE_OF_SOULS.v(t, message, args);
    }

    /**
     * 記錄詳細(verbose)級別的異常。
     */
    public static void v(@Nullable Throwable t) {
        TREE_OF_SOULS.v(t);
    }

    /**
     * 記錄調試(debug)級別的訊息，可選格式化參數。
     */
    public static void d(@Nullable String message, Object... args) {
        TREE_OF_SOULS.d(message, args);
    }

    /**
     * 記錄調試(debug)級別的異常和訊息，可選格式化參數。
     */
    public static void d(@Nullable Throwable t, @Nullable String message, Object... args) {
        TREE_OF_SOULS.d(t, message, args);
    }

    /**
     * 記錄調試(debug)級別的異常。
     */
    public static void d(@Nullable Throwable t) {
        TREE_OF_SOULS.d(t);
    }

    /**
     * 記錄資訊(info)級別的訊息，可選格式化參數。
     */
    public static void i(@Nullable String message, Object... args) {
        TREE_OF_SOULS.i(message, args);
    }

    /**
     * 記錄資訊(info)級別的異常和訊息，可選格式化參數。
     */
    public static void i(@Nullable Throwable t, @Nullable String message, Object... args) {
        TREE_OF_SOULS.i(t, message, args);
    }

    /**
     * 記錄資訊(info)級別的異常。
     */
    public static void i(@Nullable Throwable t) {
        TREE_OF_SOULS.i(t);
    }

    /**
     * 記錄警告(warning)級別的訊息，可選格式化參數。
     */
    public static void w(@Nullable String message, Object... args) {
        TREE_OF_SOULS.w(message, args);
    }

    /**
     * 記錄警告(warning)級別的異常和訊息，可選格式化參數。
     */
    public static void w(@Nullable Throwable t, @Nullable String message, Object... args) {
        TREE_OF_SOULS.w(t, message, args);
    }

    /**
     * 記錄警告(warning)級別的異常。
     */
    public static void w(@Nullable Throwable t) {
        TREE_OF_SOULS.w(t);
    }

    /**
     * 記錄錯誤(error)級別的訊息，可選格式化參數。
     */
    public static void e(@Nullable String message, Object... args) {
        TREE_OF_SOULS.e(message, args);
    }

    /**
     * 記錄錯誤(error)級別的異常和訊息，可選格式化參數。
     */
    public static void e(@Nullable Throwable t, @Nullable String message, Object... args) {
        TREE_OF_SOULS.e(t, message, args);
    }

    /**
     * 記錄錯誤(error)級別的異常。
     */
    public static void e(@Nullable Throwable t) {
        TREE_OF_SOULS.e(t);
    }

    /**
     * 記錄斷言(assert)級別的訊息，可選格式化參數。
     */
    public static void wtf(@Nullable String message, Object... args) {
        TREE_OF_SOULS.wtf(message, args);
    }

    /**
     * 記錄斷言(assert)級別的異常和訊息，可選格式化參數。
     */
    public static void wtf(@Nullable Throwable t, @Nullable String message, Object... args) {
        TREE_OF_SOULS.wtf(t, message, args);
    }

    /**
     * 記錄斷言(assert)級別的異常。
     */
    public static void wtf(@Nullable Throwable t) {
        TREE_OF_SOULS.wtf(t);
    }

    /**
     * 日誌上下文類，用於保存日誌相關信息
     */
    private static class LogContext {
        final int priority;
        final String tag;
        final String message;
        final Object[] args;
        final Throwable throwable;

        LogContext(int priority, String tag, String message, Throwable throwable, Object[] args) {
            this.priority = priority;
            this.tag = tag;
            this.message = message != null ? message : "";
            this.throwable = throwable;
            this.args = args;
        }
    }

    /**
     * 處理日誌呼叫的外觀模式。通過 {@link #plant} 安裝實例。
     */
    public abstract static class Tree {

        /**
         * 記錄詳細(verbose)級別的訊息，可選格式化參數。
         */
        public void v(@Nullable String message, Object... args) {
            prepareLog(Log.VERBOSE, null, message, args);
        }

        /**
         * 記錄詳細(verbose)級別的異常和訊息，可選格式化參數。
         */
        public void v(@Nullable Throwable t, @Nullable String message, Object... args) {
            prepareLog(Log.VERBOSE, t, message, args);
        }

        /**
         * 記錄詳細(verbose)級別的異常。
         */
        public void v(@Nullable Throwable t) {
            prepareLog(Log.VERBOSE, t, null);
        }

        /**
         * 記錄調試(debug)級別的訊息，可選格式化參數。
         */
        public void d(@Nullable String message, Object... args) {
            prepareLog(Log.DEBUG, null, message, args);
        }

        /**
         * 記錄調試(debug)級別的異常和訊息，可選格式化參數。
         */
        public void d(@Nullable Throwable t, @Nullable String message, Object... args) {
            prepareLog(Log.DEBUG, t, message, args);
        }

        /**
         * 記錄調試(debug)級別的異常。
         */
        public void d(@Nullable Throwable t) {
            prepareLog(Log.DEBUG, t, null);
        }

        /**
         * 記錄資訊(info)級別的訊息，可選格式化參數。
         */
        public void i(@Nullable String message, Object... args) {
            prepareLog(Log.INFO, null, message, args);
        }

        /**
         * 記錄資訊(info)級別的異常和訊息，可選格式化參數。
         */
        public void i(@Nullable Throwable t, @Nullable String message, Object... args) {
            prepareLog(Log.INFO, t, message, args);
        }

        /**
         * 記錄資訊(info)級別的異常。
         */
        public void i(@Nullable Throwable t) {
            prepareLog(Log.INFO, t, null);
        }

        /**
         * 記錄警告(warning)級別的訊息，可選格式化參數。
         */
        public void w(@Nullable String message, Object... args) {
            prepareLog(Log.WARN, null, message, args);
        }

        /**
         * 記錄警告(warning)級別的異常和訊息，可選格式化參數。
         */
        public void w(@Nullable Throwable t, @Nullable String message, Object... args) {
            prepareLog(Log.WARN, t, message, args);
        }

        /**
         * 記錄警告(warning)級別的異常。
         */
        public void w(@Nullable Throwable t) {
            prepareLog(Log.WARN, t, null);
        }

        /**
         * 記錄錯誤(error)級別的訊息，可選格式化參數。
         */
        public void e(@Nullable String message, Object... args) {
            prepareLog(Log.ERROR, null, message, args);
        }

        /**
         * 記錄錯誤(error)級別的異常和訊息，可選格式化參數。
         */
        public void e(@Nullable Throwable t, @Nullable String message, Object... args) {
            prepareLog(Log.ERROR, t, message, args);
        }

        /**
         * 記錄錯誤(error)級別的異常。
         */
        public void e(@Nullable Throwable t) {
            prepareLog(Log.ERROR, t, null);
        }

        /**
         * 記錄斷言(assert)級別的訊息，可選格式化參數。
         */
        public void wtf(@Nullable String message, Object... args) {
            prepareLog(Log.ASSERT, null, message, args);
        }

        /**
         * 記錄斷言(assert)級別的異常和訊息，可選格式化參數。
         */
        public void wtf(@Nullable Throwable t, @Nullable String message, Object... args) {
            prepareLog(Log.ASSERT, t, message, args);
        }

        /**
         * 記錄斷言(assert)級別的異常。
         */
        public void wtf(@Nullable Throwable t) {
            prepareLog(Log.ASSERT, t, null);
        }

        /**
         * 返回指定優先級和標籤的訊息是否應該被記錄。
         */
        protected boolean isLoggable(@Nullable String tag, int priority) {
            return true;
        }

        // 準備日誌訊息，處理格式化和異常
        private void prepareLog(int priority, @Nullable Throwable t, @Nullable String message, Object... args) {
            // 獲取標籤
            String tag = getTag();

            if (!isLoggable(tag, priority)) {
                return;
            }

            // 處理消息
            String formattedMessage = message;
            if (message != null && args.length > 0) {
                try {
                    formattedMessage = String.format(message, args);
                } catch (Exception e) {
                    formattedMessage = message + " (格式化失敗)";
                }
            }

            // 處理異常
            if (t != null) {
                if (formattedMessage == null || formattedMessage.isEmpty()) {
                    formattedMessage = Log.getStackTraceString(t);
                } else {
                    formattedMessage += "\n" + Log.getStackTraceString(t);
                }
            }

            // 如果消息為空，則不記錄
            if (formattedMessage == null || formattedMessage.isEmpty()) {
                return;
            }

            // 記錄日誌
            log(priority, tag, formattedMessage, t);
        }

        // 獲取標籤的方法
        @Nullable
        private String getTag() {
            // 優先使用顯式設置的標籤
            String tag = TagHolder.tag;
            if (tag != null) {
                TagHolder.tag = null; // 清除標籤，因為它是一次性的
                return tag;
            }

            // 否則嘗試從調用堆疊推斷標籤
            return createStackElementTag();
        }

        // 從堆疊推斷標籤
        @Nullable
        private String createStackElementTag() {
            StackTraceElement[] stackTrace = new Throwable().getStackTrace();
            List<String> fqcnIgnore = Arrays.asList(
                    Timber.class.getName(),
                    Tree.class.getName(),
                    DebugTree.class.getName()
            );

            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                if (!fqcnIgnore.contains(className)) {
                    return createTagFromStackElement(element);
                }
            }
            return null;
        }

        // 從堆疊元素創建標籤
        @Nullable
        protected String createTagFromStackElement(StackTraceElement element) {
            String tag = element.getClassName();
            tag = tag.substring(tag.lastIndexOf('.') + 1);

            // 處理匿名類和內部類
            Pattern anonymousClassPattern = Pattern.compile("(\\$\\d+)+$");
            if (anonymousClassPattern.matcher(tag).find()) {
                tag = anonymousClassPattern.matcher(tag).replaceAll("");
            }

            // 處理標籤長度限制
            if (tag.length() > MAX_TAG_LENGTH && Build.VERSION.SDK_INT < 26) {
                tag = tag.substring(0, MAX_TAG_LENGTH);
            }

            return tag;
        }

        /**
         * 將日誌訊息寫入目的地。
         */
        protected abstract void log(int priority, @Nullable String tag, String message, @Nullable Throwable t);
    }

    /**
     * 用於調試構建的 {@link Tree}。使用 Android 的 Log 類記錄日誌。
     */
    public static class DebugTree extends Tree {
        @Override
        protected void log(int priority, @Nullable String tag, String message, @Nullable Throwable t) {
            String finalTag = tag != null ? tag : "Timber";

            if (message.length() < MAX_LOG_LENGTH) {
                if (priority == Log.ASSERT) {
                    Log.wtf(finalTag, message);
                } else {
                    Log.println(priority, finalTag, message);
                }
                return;
            }

            // 處理長消息
            int i = 0;
            int length = message.length();
            while (i < length) {
                int newline = message.indexOf('\n', i);
                newline = newline != -1 ? newline : length;
                do {
                    int end = Math.min(newline, i + MAX_LOG_LENGTH);
                    String part = message.substring(i, end);
                    if (priority == Log.ASSERT) {
                        Log.wtf(finalTag, part);
                    } else {
                        Log.println(priority, finalTag, part);
                    }
                    i = end;
                } while (i < newline);
                i++;
            }
        }
    }

    /**
     * 應用程序生命週期觀察者，使用 ProcessLifecycleOwner
     */
    public static class ApplicationLifecycleObserver implements DefaultLifecycleObserver {

        public ApplicationLifecycleObserver() {
            // 註冊為生命週期觀察者
            ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        }

        @Override
        public void onCreate(LifecycleOwner owner) {
            Timber.tag("TimberLifecycle").d("應用程序已創建");
        }

        @Override
        public void onStart(LifecycleOwner owner) {
            Timber.tag("TimberLifecycle").d("應用程序已啟動（前台）");
        }

        @Override
        public void onResume(LifecycleOwner owner) {
            Timber.tag("TimberLifecycle").d("應用程序已恢復（可見）");
        }

        @Override
        public void onPause(LifecycleOwner owner) {
            Timber.tag("TimberLifecycle").d("應用程序已暫停");
        }

        @Override
        public void onStop(LifecycleOwner owner) {
            Timber.tag("TimberLifecycle").d("應用程序已停止（後台）");
            // 當應用進入後台時，清理部分資源
            cleanupBackgroundDisposables();
        }

        @Override
        public void onDestroy(LifecycleOwner owner) {
            Timber.tag("TimberLifecycle").d("應用程序已銷毀");
            // 清理所有資源
            DISPOSABLES.clear();
        }

        /**
         * 清理後台運行時不需要的 Disposable
         */
        private void cleanupBackgroundDisposables() {
            // 這裡可以實現更細緻的清理邏輯
            Timber.tag("TimberLifecycle").d("應用進入後台，清理部分 Disposable");
        }

        /**
         * 解除觀察者註冊
         */
        public void unregister() {
            ProcessLifecycleOwner.get().getLifecycle().removeObserver(this);
        }
    }


    // 靈魂之樹 - 所有日誌調用的入口點
    private static final Tree TREE_OF_SOULS = new Tree() {
        @Override
        protected void log(int priority, @Nullable String tag, String message, @Nullable Throwable t) {
            // 獲取所有已植入的樹
            List<Tree> forest = FOREST;
            if (forest.isEmpty()) {
                return;
            }

            // 使用 RxJava 將日誌分發給所有樹
            final String finalTag = tag;
            final String finalMessage = message;
            final Throwable finalT = t;

            Disposable disposable = Flowable.fromIterable(forest)
                    .subscribeOn(Schedulers.io())
                    .doOnNext(tree -> {
                        // 為每棵樹設置標籤
                        if (finalTag != null) {
                            TagHolder.tag = finalTag;
                        }

                        // 根據優先級調用相應的日誌方法
                        switch (priority) {
                            case Log.VERBOSE:
                                if (finalT != null) {
                                    tree.v(finalT, finalMessage);
                                } else {
                                    tree.v(finalMessage);
                                }
                                break;
                            case Log.DEBUG:
                                if (finalT != null) {
                                    tree.d(finalT, finalMessage);
                                } else {
                                    tree.d(finalMessage);
                                }
                                break;
                            case Log.INFO:
                                if (finalT != null) {
                                    tree.i(finalT, finalMessage);
                                } else {
                                    tree.i(finalMessage);
                                }
                                break;
                            case Log.WARN:
                                if (finalT != null) {
                                    tree.w(finalT, finalMessage);
                                } else {
                                    tree.w(finalMessage);
                                }
                                break;
                            case Log.ERROR:
                                if (finalT != null) {
                                    tree.e(finalT, finalMessage);
                                } else {
                                    tree.e(finalMessage);
                                }
                                break;
                            case Log.ASSERT:
                                if (finalT != null) {
                                    tree.wtf(finalT, finalMessage);
                                } else {
                                    tree.wtf(finalMessage);
                                }
                                break;
                        }
                    })
                    .doOnError(throwable -> System.err.println("Timber error: " + throwable.getMessage()))
                    .onErrorResumeNext(throwable -> Flowable.empty())
                    .subscribe();

            // 將訂閱添加到 CompositeDisposable
            DISPOSABLES.add(disposable);
        }
    };

    // 常量
    private static final int MAX_LOG_LENGTH = 4000;  // Android 日誌的最大長度限制
    private static final int MAX_TAG_LENGTH = 23;    // 標籤的最大長度限制
}
