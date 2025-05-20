package com.earthbook.log_exception_module.timber;

import androidx.annotation.Nullable;

/**
 * 定義所有日誌操作的介面
 */
public interface LoggerInterface {
    /**
     * VERBOSE 級別日誌
     */
    void v(@Nullable String message, Object... args);
    void v(@Nullable Throwable t, @Nullable String message, Object... args);
    void v(@Nullable Throwable t);

    /**
     * DEBUG 級別日誌
     */
    void d(@Nullable String message, Object... args);
    void d(@Nullable Throwable t, @Nullable String message, Object... args);
    void d(@Nullable Throwable t);

    /**
     * INFO 級別日誌
     */
    void i(@Nullable String message, Object... args);
    void i(@Nullable Throwable t, @Nullable String message, Object... args);
    void i(@Nullable Throwable t);

    /**
     * WARNING 級別日誌
     */
    void w(@Nullable String message, Object... args);
    void w(@Nullable Throwable t, @Nullable String message, Object... args);
    void w(@Nullable Throwable t);

    /**
     * ERROR 級別日誌
     */
    void e(@Nullable String message, Object... args);
    void e(@Nullable Throwable t, @Nullable String message, Object... args);
    void e(@Nullable Throwable t);

    /**
     * ASSERT 級別日誌
     */
    void wtf(@Nullable String message, Object... args);
    void wtf(@Nullable Throwable t, @Nullable String message, Object... args);
    void wtf(@Nullable Throwable t);

    /**
     * 自定義優先級日誌
     */
    void log(int priority, @Nullable String message, Object... args);
    void log(int priority, @Nullable Throwable t, @Nullable String message, Object... args);
    void log(int priority, @Nullable Throwable t);
}

