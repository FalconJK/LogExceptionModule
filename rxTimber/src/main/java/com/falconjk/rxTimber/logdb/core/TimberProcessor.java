package com.falconjk.rxTimber.logdb.core;

import android.util.Log;

import java.util.List;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.processors.PublishProcessor;

/**
 * 處理日誌消息的處理器
 */
public class TimberProcessor {
    // 單例實例
    private static volatile TimberProcessor instance;

    // 單例 Subject 用於接收所有日誌訊息
    private final PublishProcessor<LogEntry> logProcessor = PublishProcessor.create();

    // 用於管理所有訂閱
    private final CompositeDisposable disposables = new CompositeDisposable();

    private TimberProcessor() {
        // 初始化 RxJava 處理流程
        initProcessor();
    }

    public static TimberProcessor getInstance() {
        if (instance == null) {
            synchronized (TimberProcessor.class) {
                if (instance == null) {
                    instance = new TimberProcessor();
                }
            }
        }
        return instance;
    }

    private void initProcessor() {
        disposables.add(logProcessor
                .onBackpressureBuffer()
                .flatMap(logEntry -> Flowable.defer(() -> {
                    List<Tree> trees = Forest.forest();

                    // 如果沒有任何 Tree，直接忽略此條日誌，不拋出錯誤避免終止整個串流
                    if (trees.isEmpty()) {
                        return Flowable.just("No Trees!!!");
                    }
                    // 確保並行度至少為 1，避免 trees.size() 為 0 時的問題
                    int maxConcurrency = Math.max(1, Math.min(trees.size(), Runtime.getRuntime().availableProcessors()));
                    return Flowable.fromIterable(trees)
                            .flatMap(
                                    tree -> processTree(tree, logEntry),
                                    true,  // delayErrors: 讓其他 Tree 繼續處理，即使其中一個出錯
                                    maxConcurrency
                            );
                }))
                .subscribe(
                        errorMsg -> Log.e("TimberProcessor", errorMsg),
                        e -> {
                            // 這裡只有在 logProcessor 本身出錯時才會進入
                            Log.e("TimberProcessor", "Fatal error in log stream: " + e.getMessage());
                        }
                )
        );
    }

    // 將樹的處理邏輯提取為單獨的方法
    private Flowable<String> processTree(Tree tree, LogEntry logEntry) {
        return Flowable.just(tree)
                .observeOn(tree.getScheduler())
                .flatMap(t -> {
                    if (t.isLoggable(logEntry.stackInfo.tag, logEntry.priority)) {
                        t.log(logEntry.priority, logEntry.stackInfo.tag, logEntry.stackInfo.link, logEntry.message, logEntry.throwable, logEntry.stackTraces);
                    }
                    return Flowable.<String>empty();
                })
                .onErrorResumeNext(throwable -> Flowable.just(""));
    }

    /**
     * 處理日誌條目
     */
    public void processLog(LogEntry logEntry) {
        logProcessor.onNext(logEntry);
    }

    /**
     * 清理所有訂閱
     */
    public void clearDisposables() {
        disposables.clear();
        initProcessor();
    }
}
