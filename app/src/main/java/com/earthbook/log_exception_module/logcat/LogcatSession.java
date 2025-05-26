package com.earthbook.log_exception_module.logcat;

import android.net.Uri;
import android.os.Build;

import com.earthbook.log_exception_module.logdb.Timber;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class LogcatSession {
    private static final long THREAD_JOIN_TIMEOUT = 5_000L; // 5 seconds

    private final int capacity;
    private final Set<String> buffers;
    private volatile long pollIntervalMs = 250;

    private volatile boolean record = false;
    private Thread recordThread = null;

    private final ReentrantLock lock = new ReentrantLock();
    private final LinkedBlockingQueue<List<Log>> recordBuffer = new LinkedBlockingQueue<>();
    private RecordingFileInfo recordingFileInfo = null;

    // 使用ArrayList替代FixedCircularArray
    private final List<Log> allLogs = new ArrayList<>();
    private final List<Log> pendingLogs = new ArrayList<>();

    private final List<Filter> filters = new ArrayList<>();
    private final List<Filter> exclusions = new ArrayList<>();

    // RxJava 3 subjects
    private final PublishSubject<List<Log>> logSubject = PublishSubject.create();
    private final BehaviorSubject<List<Log>> allLogsSubject = BehaviorSubject.create();

    private volatile boolean active = false;
    private volatile boolean paused = false;
    private boolean stopped = false;
    private final Object pauseWaiter = new Object();

    private Process logcatProcess = null;
    private Thread logcatThread = null;
    private Thread pollerThread = null;

    // RxJava 3 subjects for option support
    private static final BehaviorSubject<Boolean> uidOptionSupportedSubject = BehaviorSubject.create();
    private static final BehaviorSubject<Boolean> yearOptionSupportedSubject = BehaviorSubject.create();

    static {
        // Initialize the support checks
        Schedulers.io().scheduleDirect(() -> {
            uidOptionSupportedSubject.onNext(dumpLogcatLogWithOptions("-v", "uid"));
            yearOptionSupportedSubject.onNext(dumpLogcatLogWithOptions("-v", "year"));
        });
    }

    public LogcatSession(int capacity, Set<String> buffers) {
        this.capacity = capacity;
        this.buffers = new HashSet<>(buffers);
    }

    public LogcatSession(int capacity, Set<String> buffers, long pollIntervalMs) {
        this(capacity, buffers);
        this.pollIntervalMs = pollIntervalMs;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean value) {
        synchronized (pauseWaiter) {
            paused = value;
            if (!paused) {
                pauseWaiter.notify();
            }
        }
    }

    public boolean isRecording() {
        lock.lock();
        try {
            return record;
        } finally {
            lock.unlock();
        }
    }

    public static class Status {
        private final boolean success;

        public Status(boolean success) {
            this.success = success;
        }

        public boolean isSuccess() {
            return success;
        }
    }

    // 獲取日誌流
    public Flowable<List<Log>> getLogs() {
        return Flowable.create(emitter -> {
            lock.lock();
            try {
                emitter.onNext(filtered(new ArrayList<>(allLogs)));

                // 設置新日誌的監聽器
                logSubject.subscribe(logs -> {
                    if (!logs.isEmpty()) {
                        emitter.onNext(logs);
                    }
                });
            } finally {
                lock.unlock();
            }
        }, BackpressureStrategy.BUFFER);
    }

    public Flowable<Status> start() {
        Timber.d("starting");
        if (stopped) {
            throw new IllegalStateException("LogcatSession was stopped, it cannot be re-started");
        }
        if (active) {
            throw new IllegalStateException("LogcatSession is already active!");
        }
        active = true;

        return Flowable.create(emitter -> {
            final PublishSubject<Status> statusSubject = PublishSubject.create();
            statusSubject.subscribe(emitter::onNext);

            logcatThread = new Thread(() -> {
                boolean uidSupported = isUidOptionSupported().blockingGet();
                boolean yearSupported = isYearOptionSupported().blockingGet();
                Process process = startLogcatProcess(uidSupported, yearSupported);
                statusSubject.onNext(new Status(process != null));

                if (process != null) {
                    readLogs(process);
                }
                Timber.d("stopped logcat thread");
            });
            logcatThread.start();

            pollerThread = new Thread(this::poll);
            pollerThread.start();
        }, BackpressureStrategy.LATEST);
    }

    private List<Log> filtered(List<Log> logs) {
        if (logs == null) return Collections.emptyList();

        return logs.stream()
                .filter(e -> exclusions.stream().noneMatch(filter -> filter.apply(e)))
                .filter(e -> filters.isEmpty() || filters.stream().anyMatch(filter -> filter.apply(e)))
                .collect(Collectors.toList());
    }

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
//        cmd.add("--pid");
//        cmd.add(String.valueOf(android.os.Process.myPid()));

        try {
            Process process = new ProcessBuilder(cmd).start();
            logcatProcess = process;
            return process;
        } catch (IOException e) {
            e.printStackTrace();
            Timber.d("error starting logcat process");
            return null;
        }
    }

    private void readLogs(Process process) {
        try {
            final Thread stdoutReaderThread = new Thread(() -> {
                try (LogcatStreamReader logs = new LogcatStreamReader(process.getInputStream())) {
                    while (logs.hasNext()) {
                        Log log = logs.next();
                        lock.lock();
                        try {
                            pendingLogs.add(log);
                        } finally {
                            lock.unlock();
                        }
                    }
                } catch (Exception e) {
                    // Ignore
                }
                Timber.d("stopped logcat reader thread");
            });
            stdoutReaderThread.start();

            // We don't care about the exit value as the process doesn't exit normally.
            process.waitFor();
            process.getInputStream().close();
            stdoutReaderThread.join(THREAD_JOIN_TIMEOUT);
        } catch (Exception e) {
            Timber.d("error reading logs");
        }
    }

    private void poll() {
        while (active) {
            synchronized (pauseWaiter) {
                while (paused) {
                    try {
                        pauseWaiter.wait();
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }

            lock.lock();
            try {
                List<Log> pending = new ArrayList<>(pendingLogs);
                pendingLogs.clear();

                // 管理容量限制
                allLogs.addAll(pending);
                if (allLogs.size() > capacity) {
                    allLogs.subList(0, allLogs.size() - capacity).clear();
                }

                // If recording is enabled, then add to record buffer.
                if (record) {
                    recordBuffer.add(filtered(pending));
                }

                List<Log> filteredLogs = filtered(pending);
                if (!filteredLogs.isEmpty()) {
                    logSubject.onNext(filteredLogs);
                    allLogsSubject.onNext(filtered(allLogs));
                }
            } finally {
                lock.unlock();
            }

            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }

    public void stop() {
        Timber.d("stopping");
        stopped = true;
        active = false;
        record = false;
        setPaused(false);

        if (Build.VERSION.SDK_INT >= 26) {
            if (logcatProcess != null) {
                logcatProcess.destroyForcibly();
            }
        } else {
            if (logcatProcess != null) {
                logcatProcess.destroy();
            }
        }

        logcatProcess = null;

        if (logcatThread != null) {
            try {
                logcatThread.join(THREAD_JOIN_TIMEOUT);
            } catch (InterruptedException e) {
                // Ignore
            }
            logcatThread = null;
        }

        if (pollerThread != null) {
            try {
                pollerThread.join(THREAD_JOIN_TIMEOUT);
            } catch (InterruptedException e) {
                // Ignore
            }
            pollerThread = null;
        }

        if (recordThread != null) {
            recordThread.interrupt();
            try {
                recordThread.join(THREAD_JOIN_TIMEOUT);
            } catch (InterruptedException e) {
                // Ignore
            }
            recordThread = null;
        }

        lock.lock();
        try {
            allLogs.clear();
            pendingLogs.clear();
            recordBuffer.clear();
            recordingFileInfo = null;
        } finally {
            lock.unlock();
        }

        Timber.d("stopped");
    }

    public void startRecording(RecordingFileInfo recordingFileInfo, BufferedWriter writer) {
        if (record) {
            return;
        }

        lock.lock();
        try {
            this.recordingFileInfo = recordingFileInfo;
            record = true;
            recordThread = new Thread(() -> {
                while (record) {
                    List<Log> logs;
                    try {
                        logs = recordBuffer.take();
                    } catch (InterruptedException e) {
                        break;
                    }

                    try {
                        for (Log log : logs) {
                            writer.write(log.toString());
                        }
                        writer.flush();
                    } catch (Exception e) {
                        break;
                    }
                }

                try {
                    writer.flush();
                    writer.close();
                } catch (Exception e) {
                    // Ignore
                }
            });
            recordThread.start();
        } finally {
            lock.unlock();
        }
    }

    public RecordingFileInfo stopRecording() {
        lock.lock();
        try {
            record = false;
            if (recordThread != null) {
                recordThread.interrupt();
                try {
                    recordThread.join(THREAD_JOIN_TIMEOUT);
                } catch (InterruptedException e) {
                    // Ignore
                }
                recordThread = null;
            }
            recordBuffer.clear();
            RecordingFileInfo result = recordingFileInfo;
            recordingFileInfo = null;
            return result;
        } finally {
            lock.unlock();
        }
    }

    public void setFilters(List<Filter> filters, boolean exclusion) {
        lock.lock();
        try {
            if (exclusion) {
                exclusions.clear();
                exclusions.addAll(filters);
            } else {
                this.filters.clear();
                this.filters.addAll(filters);
            }
        } finally {
            lock.unlock();
        }
    }

    public void clearLogs() {
        lock.lock();
        try {
            allLogs.clear();
            pendingLogs.clear();
        } finally {
            lock.unlock();
        }
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
    }

    // RxJava接口
    public Flowable<List<Log>> getLogFlowable() {
        return logSubject.toFlowable(BackpressureStrategy.BUFFER);
    }

    public Flowable<List<Log>> getAllLogsFlowable() {
        return Flowable.create(emitter -> {
            lock.lock();
            try {
                emitter.onNext(filtered(new ArrayList<>(allLogs)));
            } finally {
                lock.unlock();
            }

            allLogsSubject.subscribe(
                    logs -> emitter.onNext(logs),
                    error -> emitter.onError(error)
            );
        }, BackpressureStrategy.LATEST);
    }

    public static class RecordingFileInfo {
        private final String fileName;
        private final Uri uri;
        private final boolean isCustomLocation;

        public RecordingFileInfo(String fileName, Uri uri, boolean isCustomLocation) {
            this.fileName = fileName;
            this.uri = uri;
            this.isCustomLocation = isCustomLocation;
        }

        public String getFileName() {
            return fileName;
        }

        public Uri getUri() {
            return uri;
        }

        public boolean isCustomLocation() {
            return isCustomLocation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RecordingFileInfo that = (RecordingFileInfo) o;
            return isCustomLocation == that.isCustomLocation &&
                    fileName.equals(that.fileName) &&
                    uri.equals(that.uri);
        }

        @Override
        public int hashCode() {
            int result = fileName.hashCode();
            result = 31 * result + uri.hashCode();
            result = 31 * result + (isCustomLocation ? 1 : 0);
            return result;
        }
    }

    public static Single<Boolean> isUidOptionSupported() {
        return uidOptionSupportedSubject
                .filter(value -> value != null)
                .firstOrError();
    }

    public static Single<Boolean> isYearOptionSupported() {
        return yearOptionSupportedSubject
                .filter(value -> value != null)
                .firstOrError();
    }

    private static boolean dumpLogcatLogWithOptions(String... options) {
        if (options.length == 0) {
            throw new IllegalArgumentException("Options cannot be empty");
        }

        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("logcat");
            cmd.add("-v");
            cmd.add("long");
            Collections.addAll(cmd, options);
            cmd.add("-d");

            Process process = new ProcessBuilder(cmd).start();
            Thread stdoutReaderThread = new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Just consume the output
                    }
                } catch (Exception e) {
                    // Ignore
                }
            });
            stdoutReaderThread.start();

            boolean result = process.waitFor() == 0;
            stdoutReaderThread.join(THREAD_JOIN_TIMEOUT);
            return result;
        } catch (Exception e) {
            return false;
        }
    }

    // Filter interface
    public interface Filter {
        boolean apply(Log log);
    }
}
