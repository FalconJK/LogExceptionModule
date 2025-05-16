package com.earthbook.log_exception_module.log;

import java.util.List;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;

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
