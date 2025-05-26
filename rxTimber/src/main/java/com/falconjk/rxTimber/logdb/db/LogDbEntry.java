package com.falconjk.rxTimber.logdb.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "log_entries",
        indices = {
                @Index(value = {"session_id", "timestamp_seconds"}),
                @Index(value = {"log_level"}),
                @Index(value = {"created_at"})
        }
)
public class LogDbEntry {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "session_id")
    private String sessionId;

    @ColumnInfo(name = "log_level")
    private String logLevel;

    @ColumnInfo(name = "pid")
    private int pid;

    @ColumnInfo(name = "tid")
    private int tid;

    @ColumnInfo(name = "application_id")
    private String applicationId;

    @ColumnInfo(name = "process_name")
    private String processName;

    @ColumnInfo(name = "tag")
    private String tag;

    @ColumnInfo(name = "timestamp_seconds")
    private long timestampSeconds;

    @ColumnInfo(name = "timestamp_nanos")
    private long timestampNanos;

    @ColumnInfo(name = "message")
    private String message;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    // 建構函數、Getters 和 Setters
    public LogDbEntry(String sessionId, String logLevel, int pid, int tid, String applicationId,
                      String processName, String tag, long timestampSeconds, long timestampNanos,
                      String message) {
        this.sessionId = sessionId;
        this.logLevel = logLevel;
        this.pid = pid;
        this.tid = tid;
        this.applicationId = applicationId;
        this.processName = processName;
        this.tag = tag;
        this.timestampSeconds = timestampSeconds;
        this.timestampNanos = timestampNanos;
        this.message = message;
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * 計算物件的大概大小（以位元組為單位）
     * @return 物件大小（bytes）
     */
    public long getBytes() {
        long size = 0;

        // 基本資料型別大小
        size += 8; // long id
        size += 4; // int pid
        size += 4; // int tid
        size += 8; // long timestampSeconds
        size += 8; // long timestampNanos
        size += 8; // long createdAt

        // String 欄位大小計算（UTF-8 編碼）
        size += getStringBytes(sessionId);
        size += getStringBytes(logLevel);
        size += getStringBytes(applicationId);
        size += getStringBytes(processName);
        size += getStringBytes(tag);
        size += getStringBytes(message);

        return size;
    }

    /**
     * 計算字串的位元組大小
     * @param str 要計算的字串
     * @return 字串大小（bytes）
     */
    private long getStringBytes(String str) {
        if (str == null) {
            return 0;
        }
        try {
            return str.getBytes("UTF-8").length;
        } catch (Exception e) {
            // 如果編碼失敗，使用預設編碼
            return str.getBytes().length;
        }
    }

    // Getters 和 Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public int getTid() {
        return tid;
    }

    public void setTid(int tid) {
        this.tid = tid;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public long getTimestampSeconds() {
        return timestampSeconds;
    }

    public void setTimestampSeconds(long timestampSeconds) {
        this.timestampSeconds = timestampSeconds;
    }

    public long getTimestampNanos() {
        return timestampNanos;
    }

    public void setTimestampNanos(long timestampNanos) {
        this.timestampNanos = timestampNanos;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}