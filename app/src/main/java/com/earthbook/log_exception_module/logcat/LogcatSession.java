package com.earthbook.log_exception_module.logcat;

import com.earthbook.log_exception_module.BuildConfig;
import com.earthbook.log_exception_module.timber.Timber;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;

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

    private static final long THREAD_JOIN_TIMEOUT = 1000L; // 1 second
    private static final long POLL_INTERVAL = 200L; // 200 milliseconds

    private final List<String> buffers;
    private final List<String> pendingLogs = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    private Process logcatProcess;
    private Thread logcatThread;
    private Thread pollerThread;

    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private final PublishSubject<List<String>> logsSubject = PublishSubject.create();

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

            // 啟動 logcat 線程
            logcatThread = new Thread(() -> {
                // 檢查設備支持的 logcat 選項
                boolean uidSupported = isUidOptionSupported();
                boolean yearSupported = isYearOptionSupported();

                // 啟動 logcat 進程
                Process process = startLogcatProcess(uidSupported, yearSupported);
                statusSubject.onNext(new Status(process != null));

                if (process != null) {
                    // 讀取 logcat 輸出
                    readLogs(process);
                }
            });
            logcatThread.start();

            // 啟動輪詢線程
            pollerThread = new Thread(this::poll);
            pollerThread.start();

            return statusSubject.toFlowable(BackpressureStrategy.BUFFER);
        });
    }

    /**
     * 獲取日誌流
     */
    public Flowable<List<String>> getLogs() {
        return logsSubject.toFlowable(BackpressureStrategy.BUFFER);
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

        // 等待線程結束
        if (logcatThread != null) {
            try {
                logcatThread.join(THREAD_JOIN_TIMEOUT);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            logcatThread = null;
        }

        if (pollerThread != null) {
            try {
                pollerThread.join(THREAD_JOIN_TIMEOUT);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            pollerThread = null;
        }

        // 標記為非活動狀態
        active.set(false);

        System.out.println("LogcatSession: stopped");
    }

    /**
     * 清除日誌
     */
    public void clearLogs() {
        try {
            new ProcessBuilder("logcat", "-c").start().waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
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

            Thread stdoutReaderThread = new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // 消費輸出，但不做任何處理
                    }
                } catch (Exception e) {
                    // 忽略異常
                }
            });
            stdoutReaderThread.start();

            // 如果進程正常退出（返回0），則表示選項被支持
            boolean result = process.waitFor() == 0;
            stdoutReaderThread.join(THREAD_JOIN_TIMEOUT);
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
        cmd.add("--pid");
        String pid = android.os.Process.myPid() + "";
        Timber.d(pid);
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
     * 讀取 logcat 輸出
     */
    private void readLogs(Process process) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // 啟動一個線程讀取標準輸出
            Thread stdoutReaderThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lock.lock();
                        try {
                            pendingLogs.add(line);
                        } finally {
                            lock.unlock();
                        }
                    }
                } catch (Exception e) {
                    // 忽略異常
                }
                System.out.println("LogcatSession: stopped logcat reader thread");
            });
            stdoutReaderThread.start();

            // 等待進程結束
            process.waitFor();
            reader.close();
            stdoutReaderThread.join(THREAD_JOIN_TIMEOUT);
        } catch (Exception e) {
            System.out.println("LogcatSession: error reading logs");
        }
    }

    /**
     * 輪詢並發布日誌
     */
    private void poll() {
        Disposable disposable = Observable.interval(POLL_INTERVAL, TimeUnit.MILLISECONDS)
                .takeWhile(tick -> !stopped.get())
                .subscribeOn(Schedulers.io())
                .subscribe(tick -> {
                    List<String> logs = new ArrayList<>();

                    lock.lock();
                    try {
                        if (!pendingLogs.isEmpty()) {
                            logs.addAll(pendingLogs);
                            pendingLogs.clear();
                        }
                    } finally {
                        lock.unlock();
                    }

                    if (!logs.isEmpty()) {
                        logsSubject.onNext(logs);
                    }
                }, throwable -> {
                    System.out.println("LogcatSession: error in polling thread: " + throwable.getMessage());
                });
    }
}

