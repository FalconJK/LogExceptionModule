package com.earthbook.log_exception_module.logcat;

import com.earthbook.log_exception_module.timber.Timber;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class LogcatSession {
    // 狀態類
    public static class Status {
        private final boolean success;

        public Status(boolean success) {
            this.success = success;
        }

        public boolean isSuccess() {
            return success;
        }
    }

    private static final long POLL_INTERVAL = 200L; // 200 milliseconds

    private final List<String> buffers;
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    // 修改為 Log 物件的處理器
    private final PublishProcessor<List<Log>> logsProcessor = PublishProcessor.create();
    private final CompositeDisposable disposables = new CompositeDisposable();

    private Process logcatProcess;

    /**
     * 創建一個新的 LogcatSession 實例
     *
     * @param buffers 要監聽的緩衝區列表，例如 ["main", "system", "radio"]
     */
    public LogcatSession(List<String> buffers) {
        this.buffers = new ArrayList<>(buffers);
    }

    /**
     * 啟動 logcat 會話並返回狀態流
     */
    public Flowable<Status> start() {
        return Flowable.defer(() -> {
            System.out.println("LogcatSession: starting");

            if (stopped.get()) {
                throw new IllegalStateException("LogcatSession was stopped, it cannot be re-started");
            }

            if (active.getAndSet(true)) {
                throw new IllegalStateException("LogcatSession is already active!");
            }

            BehaviorSubject<Status> statusSubject = BehaviorSubject.create();

            // 檢查設備支持的 logcat 選項
            Disposable optionsDisposable = Observable.fromCallable(() -> {
                        boolean uidSupported = isUidOptionSupported();
                        boolean yearSupported = isYearOptionSupported();
                        return new Object[] { uidSupported, yearSupported };
                    })
                    .subscribeOn(Schedulers.io())
                    .subscribe(options -> {
                        boolean uidSupported = (boolean) options[0];
                        boolean yearSupported = (boolean) options[1];

                        // 啟動 logcat 進程
                        Process process = startLogcatProcess(uidSupported, yearSupported);
                        statusSubject.onNext(new Status(process != null));

                        if (process != null) {
                            // 讀取 logcat 輸出
                            setupLogReading(process);
                        }
                    }, throwable -> {
                        System.out.println("LogcatSession: error checking logcat options: " + throwable.getMessage());
                        statusSubject.onNext(new Status(false));
                    });

            disposables.add(optionsDisposable);
            return statusSubject.toFlowable(BackpressureStrategy.LATEST);
        });
    }

    /**
     * 獲取日誌流 - 現在返回 Log 物件列表
     */
    public Flowable<List<Log>> getLogs() {
        return logsProcessor.onBackpressureBuffer();
    }

    /**
     * 停止 logcat 會話
     */
    public void stop() {
        System.out.println("LogcatSession: stopping");

        if (stopped.getAndSet(true)) {
            return;
        }

        // 停止 logcat 進程
        if (logcatProcess != null) {
            logcatProcess.destroy();
            logcatProcess = null;
        }

        // 清理所有訂閱
        disposables.clear();

        // 標記為非活動狀態
        active.set(false);

        System.out.println("LogcatSession: stopped");
    }

    /**
     * 清除日誌
     */
    public void clearLogs() {
        Disposable stopLogcatDisposable = Observable.fromCallable(() -> {
                    try {
                        return new ProcessBuilder("logcat", "-c").start().waitFor() == 0;
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                        return false;
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(
                        success -> System.out.println("LogcatSession: logs cleared: " + success),
                        error -> System.out.println("LogcatSession: error clearing logs: " + error.getMessage())
                );
        disposables.add(stopLogcatDisposable);
    }

    /**
     * 檢查是否支持 uid 選項
     */
    private boolean isUidOptionSupported() {
        return dumpLogcatLogWithOptions("-v", "uid");
    }

    /**
     * 檢查是否支持 year 選項
     */
    private boolean isYearOptionSupported() {
        return dumpLogcatLogWithOptions("-v", "year");
    }

    /**
     * 測試特定 logcat 選項是否被支持
     */
    private boolean dumpLogcatLogWithOptions(String... options) {
        if (options.length == 0) {
            throw new IllegalArgumentException("No options provided");
        }

        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("logcat");
            cmd.add("-v");
            cmd.add("long");
            cmd.addAll(Arrays.asList(options));
            cmd.add("-d");

            Process process = new ProcessBuilder(cmd).start();

            // 使用 RxJava 處理進程輸出
            Disposable outputDisposable = Completable.create(emitter -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null && !emitter.isDisposed()) {
                                // 消費輸出，但不做任何處理
                            }
                            emitter.onComplete();
                        } catch (Exception e) {
                            if (!emitter.isDisposed()) {
                                emitter.onError(e);
                            }
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            () -> {},
                            error -> System.out.println("Error reading process output: " + error.getMessage())
                    );

            // 等待進程完成
            boolean result = process.waitFor() == 0;
            outputDisposable.dispose();
            return result;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 啟動 logcat 進程
     */
    private Process startLogcatProcess(boolean uidSupported, boolean yearSupported) {
        List<String> cmd = new ArrayList<>();
        cmd.add("logcat");
        cmd.add("-v");
        cmd.add("long");

        if (uidSupported) {
            cmd.add("-v");
            cmd.add("uid");
        }

        if (yearSupported) {
            cmd.add("-v");
            cmd.add("year");
        }

        for (String buffer : buffers) {
            cmd.add("-b");
            cmd.add(buffer);
        }

        String pid = android.os.Process.myPid() + "";
        Timber.d(pid);
        cmd.add("--pid");
        cmd.add(pid);

        try {
            Process process = new ProcessBuilder(cmd).start();
            logcatProcess = process;
            return process;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("LogcatSession: error starting logcat process");
            return null;
        }
    }

    /**
     * 設置日誌讀取 - 現在使用 LogcatStreamReader 解析 Log 物件
     */
    private void setupLogReading(Process process) {
        // 創建一個 Observable 來讀取和解析日誌
        Disposable readerDisposable = Observable.<Log>create(emitter -> {
                    try {
                        LogcatStreamReader reader = new LogcatStreamReader(process.getInputStream());

                        // 使用無限循環，只有當 emitter 被處置時才會退出
                        while (!emitter.isDisposed()) {
                            try {
                                // 檢查是否有新日誌，但不要無限阻塞
                                if (reader.hasNext()) {
                                    emitter.onNext(reader.next());
                                } else {
                                    // 如果沒有新日誌，短暫休眠以避免 CPU 使用率過高
                                    Thread.sleep(50);
                                }
                            } catch (InterruptedException ie) {
                                // 處理中斷異常
                                Thread.currentThread().interrupt();
                                break;
                            } catch (Exception e) {
                                // 記錄其他異常但繼續運行
                                System.out.println("LogcatSession: error reading log entry: " + e.getMessage());
                                // 短暫休眠以避免在出錯情況下的快速循環
                                Thread.sleep(100);
                            }
                        }

                        // 不要在這裡調用 onComplete，讓 Observable 保持活躍直到被明確處置
                        // emitter.onComplete();
                    } catch (Exception e) {
                        if (!emitter.isDisposed()) {
                            // 只有在嚴重錯誤時才結束 Observable
                            System.out.println("LogcatSession: critical error in log reader: " + e.getMessage());
                            emitter.onError(e);
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .buffer(POLL_INTERVAL, TimeUnit.MILLISECONDS)
                .filter(logs -> !logs.isEmpty())
                .takeWhile(logs -> !stopped.get())
                .subscribe(
                        logs -> {
                            if (!logs.isEmpty()) {
                                logsProcessor.onNext(logs);
                            }
                        },
                        error -> System.out.println("LogcatSession: error reading logs: " + error.getMessage()),
                        () -> System.out.println("LogcatSession: log reading completed")
                );

        disposables.add(readerDisposable);

        // 監控進程結束
        Disposable processWatcherDisposable = Observable.fromCallable(() -> {
                    try {
                        process.waitFor();
                        return true;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(
                        completed -> {
                            if (completed && !stopped.get()) {
                                System.out.println("LogcatSession: logcat process terminated unexpectedly");
                                // 可以考慮在這裡重啟 logcat 進程
                            }
                        },
                        error -> System.out.println("LogcatSession: error waiting for process: " + error.getMessage())
                );

        disposables.add(processWatcherDisposable);
    }

    /**
     * 日誌優先級常量
     */
    public static class LogPriority {
        public static final String ASSERT = "A";
        public static final String DEBUG = "D";
        public static final String ERROR = "E";
        public static final String FATAL = "F";
        public static final String INFO = "I";
        public static final String VERBOSE = "V";
        public static final String WARNING = "W";
    }
}
