package com.falconjk.rxTimber.logdb.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface LogDbEntryDao {

    @Insert
    @Transaction
    Completable insertLogs(List<LogDbEntry> logEntries);

    @Query("SELECT * FROM log_entries WHERE session_id = :sessionId ORDER BY timestamp_seconds ASC, timestamp_nanos ASC")
    Flowable<List<LogDbEntry>> getLogsBySession(String sessionId);

    // 新增：分頁查詢方法
    @Query("SELECT * FROM log_entries WHERE session_id = :sessionId ORDER BY timestamp_seconds ASC, timestamp_nanos ASC LIMIT :limit OFFSET :offset")
    Flowable<List<LogDbEntry>> getLogsBySessionWithLimit(String sessionId, int limit, int offset);

    @Query("SELECT DISTINCT session_id FROM log_entries ORDER BY created_at DESC")
    Flowable<List<String>> getAllSessions();

    @Query("DELETE FROM log_entries WHERE session_id = :sessionId")
    @Transaction
    Completable deleteLogsBySession(String sessionId);

    @Query("DELETE FROM log_entries")
    @Transaction
    Completable deleteAllLogs();

    @Query("SELECT COUNT(*) FROM log_entries WHERE session_id = :sessionId")
    Single<Integer> getLogCountForSession(String sessionId);

    // 新增：獲取 session 的估計大小
    @Query("SELECT SUM(LENGTH(message) + LENGTH(tag) + LENGTH(application_id) + LENGTH(process_name) + 50) FROM log_entries WHERE session_id = :sessionId")
    Single<Long> getSessionEstimatedSize(String sessionId);

    @Query("DELETE FROM log_entries WHERE created_at < :timestamp")
    @Transaction
    Completable deleteOldLogs(long timestamp);

    @Query("SELECT * FROM log_entries WHERE log_level = :level AND session_id = :sessionId ORDER BY timestamp_seconds ASC")
    Flowable<List<LogDbEntry>> getLogsByLevelAndSession(String level, String sessionId);

    @Query("SELECT COUNT(*) FROM log_entries WHERE log_level = 'ERROR' AND session_id = :sessionId")
    Single<Integer> getErrorCountForSession(String sessionId);

    @Query("SELECT * FROM log_entries WHERE message LIKE '%' || :keyword || '%' AND session_id = :sessionId ORDER BY timestamp_seconds ASC")
    Flowable<List<LogDbEntry>> searchLogsByKeyword(String keyword, String sessionId);

    // 新增：刪除過大的日誌訊息
    @Query("DELETE FROM log_entries WHERE session_id = :sessionId AND LENGTH(message) > :maxMessageLength")
    @Transaction
    Completable deleteLargeMessages(String sessionId, int maxMessageLength);
}