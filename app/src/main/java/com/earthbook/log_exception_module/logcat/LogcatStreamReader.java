package com.earthbook.log_exception_module.logcat;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * LogcatStreamReader 類別用於讀取和解析 Logcat 輸出流
 * 實現了 Iterator<Log> 介面以便逐條讀取日誌，並實現 Closeable 介面以便安全關閉資源
 */
public class LogcatStreamReader implements Iterator<Log>, Closeable {
    // 使用 BufferedReader 包裝輸入流以便高效讀取
    private final BufferedReader reader;
    // 用於暫存日誌訊息內容的緩衝區
    private final StringBuilder msgBuffer = new StringBuilder();
    // 當前解析出的日誌對象
    private Log log;
    // 日誌的唯一識別符，會隨著每次讀取遞增
    private int id = 0;
    // 是否有下一個元素
    private boolean hasNextElement = false;
    // 是否已經嘗試讀取下一個元素
    private boolean nextElementRead = false;

    public LogcatStreamReader(InputStream inputStream) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
    }

    /**
     * 檢查是否還有下一條日誌
     * 此方法會嘗試讀取並解析下一條日誌，如果成功則返回 true
     */
    @Override
    public boolean hasNext() {
        if (nextElementRead) {
            return hasNextElement;
        }

        nextElementRead = true;

        while (true) {
            try {
                // 讀取日誌的元數據行，如果為空則表示已讀取完畢
                String metadata = reader.readLine();
                if (metadata == null) {
                    hasNextElement = false;
                    return false;
                }

                metadata = metadata.trim();

                // 檢查是否是有效的日誌開頭（以 "[" 開始）
                if (metadata.startsWith("[")) {
                    // 讀取日誌的第一行訊息內容
                    String msg = reader.readLine();
                    if (msg == null) {
                        hasNextElement = false;
                        return false;
                    }

                    msgBuffer.append(msg);

                    // 繼續讀取下一行
                    msg = reader.readLine();
                    if (msg == null) {
                        hasNextElement = false;
                        return false;
                    }

                    // 持續讀取直到遇到空行，表示當前日誌條目結束
                    while (msg.length() > 0) {
                        // 將多行訊息合併，保留換行符
                        msgBuffer.append("\n").append(msg);

                        msg = reader.readLine();
                        if (msg == null) {
                            hasNextElement = false;
                            return false;
                        }
                    }

                    try {
                        // 嘗試解析日誌，將元數據和訊息內容轉換為 Log 對象
                        log = Log.parse(id, metadata, msgBuffer.toString());
                        // 遞增日誌 ID
                        id += 1;
                        hasNextElement = true;
                        return true;
                    } catch (Exception e) {
                        // 解析失敗時忽略異常，繼續讀取下一條
                        // System.out.println("LogcatStreamReader: error parsing log: " + e.getMessage());
                    } finally {
                        // 清空訊息緩衝區，準備下一次讀取
                        msgBuffer.setLength(0);
                    }
                }
            } catch (IOException e) {
                hasNextElement = false;
                return false;
            }
        }
    }

    /**
     * 返回當前解析好的日誌對象
     * 此方法應在 hasNext() 返回 true 後調用
     */
    @Override
    public Log next() {
        if (!nextElementRead) {
            if (!hasNext()) {
                throw new NoSuchElementException("No more logs available");
            }
        }

        if (!hasNextElement) {
            throw new NoSuchElementException("No more logs available");
        }

        nextElementRead = false;
        return log;
    }

    /**
     * 關閉資源，釋放底層的 BufferedReader
     */
    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException ignored) {
            // 忽略關閉時可能發生的 IO 異常
        }
    }
}
