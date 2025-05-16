package com.earthbook.log_exception_module.log;

import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 用於調試構建的 {@link Tree}。自動從調用類推斷標籤。
 */
public class DebugTree extends Tree {
    private static final int MAX_LOG_LENGTH = 4000;

    /**
     * 將 {@code message} 分解為最大長度的塊（如果需要），並發送到
     * {@link Log#println(int, String, String) Log.println()} 或
     * {@link Log#wtf(String, String) Log.wtf()} 進行日誌記錄。
     */
    @Override
    protected void log(int priority, @Nullable String tag, @NotNull String message, @Nullable Throwable t) {
        if (message.length() <= MAX_LOG_LENGTH) {
            if (priority == Log.ASSERT) {
                Log.wtf(tag, message);
            } else {
                Log.println(priority, tag, message);
            }
            return;
        }

        // 按行分割，然後確保每行都能適應 Log 的最大長度。
        for (int i = 0, length = message.length(); i < length; i++) {
            int newline = message.indexOf('\n', i);
            newline = newline != -1 ? newline : length;
            do {
                int end = Math.min(newline, i + MAX_LOG_LENGTH);
                String part = message.substring(i, end);
                if (priority == Log.ASSERT) {
                    Log.wtf(tag, part);
                } else {
                    Log.println(priority, tag, part);
                }
                i = end;
            } while (i < newline);
            i++;
        }
    }
}
