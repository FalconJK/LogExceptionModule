package com.earthbook.log_exception_module.db;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.earthbook.log_exception_module.timber.LaunchSession;
import com.earthbook.log_exception_module.timber.Tree;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class DbTree extends Tree {
    private static final String TAG = "DbTree";
    private static final int BUFFER_SIZE = 1000; // 降低緩衝區大小
    private static final int BUFFER_TIMEOUT_SECONDS = 30; // 改為30秒
    private static final long MAX_BUFFER_SIZE_BYTES = 20 * 1024 * 1024; // 20MB

    private final Context context;
    private final PublishSubject<LogDbEntry> logSubject;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final String currentSessionId;
    private final PublishSubject<Object> emergencyFlush;

    private final AtomicLong currentBufferSize;

    public DbTree(Application application) {
        this.context = application.getApplicationContext();
        this.currentSessionId = LaunchSession.getSession();
        LogDatabase.init(application);

        logSubject = PublishSubject.create();
        emergencyFlush = PublishSubject.create();
        PublishSubject<Object> sizeFlush = PublishSubject.create();
        currentBufferSize = new AtomicLong(0);

        // 創建刷新信號：定時 + 手動 + 容量監控
        Flowable<?> flushSignal = Flowable.merge(
                Flowable.interval(BUFFER_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                emergencyFlush.toFlowable(BackpressureStrategy.BUFFER),
                sizeFlush.toFlowable(BackpressureStrategy.BUFFER)
        );

        // 設置緩衝並保存到資料庫
        Disposable bufferDisposable = logSubject
                .toFlowable(BackpressureStrategy.BUFFER)
                .buffer(flushSignal, BUFFER_SIZE)
                .filter(logs -> !logs.isEmpty())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMapCompletable(logs -> {
                    Log.d(TAG, "Saving " + logs.size() + " logs to database");
                    // 重置緩衝區大小計數器
                    currentBufferSize.set(0);
                    return LogDatabase.getInstance(context).logEntryDao().insertLogs(logs);
                })
                .subscribe(
                        () -> Log.d(TAG, "Logs saved to database successfully"),
                        throwable -> Log.e(TAG, "Error saving logs", throwable)
                );
        disposables.add(bufferDisposable);

        // 監控緩衝區大小
        Disposable sizeMonitor = logSubject
                .map(logDbEntry -> currentBufferSize.addAndGet(logDbEntry.getBytes()))
                .filter(totalSize -> totalSize > MAX_BUFFER_SIZE_BYTES)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .subscribe(
                        totalSize -> {
                            Log.d(TAG, "Buffer size exceeded: " + totalSize + " bytes, triggering flush");
                            sizeFlush.onNext(new Object());
                        },
                        throwable -> Log.e(TAG, "Error monitoring buffer size", throwable)
                );
        disposables.add(sizeMonitor);


        // 註冊生命週期回調
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            private int activityCount = 0;

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                activityCount++;
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                activityCount--;
                if (activityCount == 0) {
                    forceFlushLogs();
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                forceFlushLogs();
            }
        });
    }

    @Override
    protected void log(int priority, String tag, @NonNull String message, Throwable t) {
        try {
            String fullMessage = message;
            if (t != null) {
                fullMessage = message + "\n" + Log.getStackTraceString(t);
            }

            LogDbEntry logEntry = new LogDbEntry(
                    currentSessionId,
                    getLogLevelString(priority),
                    android.os.Process.myPid(),
                    (int) Thread.currentThread().getId(),
                    context.getPackageName(),
                    context.getPackageName(),
                    tag != null ? tag : "",
                    System.currentTimeMillis() / 1000,
                    (System.currentTimeMillis() % 1000) * 1000000,
                    fullMessage
            );

            logSubject.onNext(logEntry);
        } catch (Exception e) {
            Log.e(TAG, "Error emitting log", e);
        }
    }

    private String getLogLevelString(int priority) {
        switch (priority) {
            case Log.VERBOSE:
                return "VERBOSE";
            case Log.DEBUG:
                return "DEBUG";
            case Log.INFO:
                return "INFO";
            case Log.WARN:
                return "WARN";
            case Log.ERROR:
                return "ERROR";
            case Log.ASSERT:
                return "ASSERT";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * 強制刷新所有待處理的日誌到資料庫
     */
    public void forceFlushLogs() {
        try {
            currentBufferSize.set(0);
            emergencyFlush.onNext(new Object());
            // 給一點時間讓緩衝區處理
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.w(TAG, "Interrupted while flushing logs");
        }
    }

    /**
     * 清理舊日誌（保留最近7天）
     */
    public void cleanOldLogs() {
        long sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
        Disposable cleanupDisposable = LogDatabase.getInstance(context)
                .logEntryDao()
                .deleteOldLogs(sevenDaysAgo)
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> Log.d(TAG, "Old logs cleaned up"),
                        throwable -> Log.e(TAG, "Error cleaning old logs", throwable)
                );
        disposables.add(cleanupDisposable);
    }

    public void shutdown() {
        forceFlushLogs();
        disposables.clear();
        if (!logSubject.hasComplete()) {
            logSubject.onComplete();
        }
        if (!emergencyFlush.hasComplete()) {
            emergencyFlush.onComplete();
        }
    }

    /**
     * 獲取當前緩衝區大小（位元組）
     */
    public long getCurrentBufferSize() {
        return currentBufferSize.get();
    }

    /**
     * 重置緩衝區大小計數器
     */
    public void resetBufferSize() {
        currentBufferSize.set(0);
    }
}