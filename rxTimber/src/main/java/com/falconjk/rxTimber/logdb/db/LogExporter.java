package com.falconjk.rxTimber.logdb.db;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LogExporter {
    private static final String TAG = "LogExporter";
    private static final int BATCH_SIZE = 1000; // 每批處理 1000 筆記錄
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB 限制

    private final Context context;
    private final Gson gson;

    public LogExporter(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public Single<File> exportSessionLogs(String sessionId) {
        return Single.fromCallable(() -> {
                    // 先檢查日誌數量
                    int logCount = LogDatabase.getInstance(context)
                            .logEntryDao()
                            .getLogCountForSession(sessionId)
                            .blockingGet();

                    if (logCount == 0) {
                        throw new IllegalStateException("沒有找到日誌資料");
                    }

                    // 建立匯出檔案
                    File exportDir = new File(context.getExternalFilesDir(null), "logs");
                    if (!exportDir.exists() && !exportDir.mkdirs()) {
                        throw new IOException("Failed to create export directory");
                    }

                    String fileName = sessionId.replace(":", "-") + ".logcat";
                    File exportFile = new File(exportDir, fileName);

                    // 使用 JsonWriter 進行串流寫入
                    try (FileWriter fileWriter = new FileWriter(exportFile);
                         JsonWriter jsonWriter = new JsonWriter(fileWriter)) {

                        jsonWriter.setIndent("  ");
                        jsonWriter.beginObject();

                        // 寫入 metadata
                        writeMetadata(jsonWriter);

                        // 寫入日誌資料（分批處理）
                        writeLogcatMessages(jsonWriter, sessionId, logCount);

                        jsonWriter.endObject();
                    }

                    // 檢查檔案大小
                    if (exportFile.length() > MAX_FILE_SIZE) {
                        exportFile.delete();
                        throw new IOException("匯出檔案太大 (" + (exportFile.length() / 1024 / 1024) + "MB)，請考慮縮小日誌範圍");
                    }

                    return exportFile;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void writeMetadata(JsonWriter jsonWriter) throws IOException {
        jsonWriter.name("metadata");
        jsonWriter.beginObject();

        // Device info
        jsonWriter.name("device");
        jsonWriter.beginObject();
        jsonWriter.name("deviceId").value(android.os.Build.SERIAL);
        jsonWriter.name("name").value(android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
        jsonWriter.name("serialNumber").value(android.os.Build.SERIAL);
        jsonWriter.name("isOnline").value(true);
        jsonWriter.name("release").value(android.os.Build.VERSION.RELEASE);
        jsonWriter.name("sdk").value(android.os.Build.VERSION.SDK_INT);
        jsonWriter.name("featureLevel").value(android.os.Build.VERSION.SDK_INT);
        jsonWriter.name("model").value(android.os.Build.MODEL);
        jsonWriter.name("type").value("HANDHELD");
        jsonWriter.name("isEmulator").value(isEmulator());
        jsonWriter.endObject();

        jsonWriter.name("filter").value("package:" + context.getPackageName());

        // Project application IDs
        jsonWriter.name("projectApplicationIds");
        jsonWriter.beginArray();
        jsonWriter.value(context.getPackageName());
        jsonWriter.value(context.getPackageName() + ".test");
        jsonWriter.endArray();

        jsonWriter.endObject();
    }

    private void writeLogcatMessages(JsonWriter jsonWriter, String sessionId, int totalCount) throws IOException {
        jsonWriter.name("logcatMessages");
        jsonWriter.beginArray();

        int offset = 0;
        int processedCount = 0;

        while (offset < totalCount) {
            // 分批查詢日誌
            List<LogDbEntry> batch = LogDatabase.getInstance(context)
                    .logEntryDao()
                    .getLogsBySessionWithLimit(sessionId, BATCH_SIZE, offset)
                    .blockingFirst();

            if (batch.isEmpty()) {
                break;
            }

            // 寫入這批日誌
            for (LogDbEntry log : batch) {
                writeLogEntry(jsonWriter, log);
                processedCount++;

                // 可以在這裡添加進度回調
                if (processedCount % 100 == 0) {
                    // 可以發送進度更新
                }
            }

            offset += BATCH_SIZE;
        }

        jsonWriter.endArray();
    }

    private void writeLogEntry(JsonWriter jsonWriter, LogDbEntry log) throws IOException {
        jsonWriter.beginObject();

        // Header
        jsonWriter.name("header");
        jsonWriter.beginObject();
        jsonWriter.name("logLevel").value(log.getLogLevel());
        jsonWriter.name("pid").value(log.getPid());
        jsonWriter.name("tid").value(log.getTid());
        jsonWriter.name("applicationId").value(log.getApplicationId());
        jsonWriter.name("processName").value(log.getProcessName());
        jsonWriter.name("tag").value(log.getTag());

        // Timestamp
        jsonWriter.name("timestamp");
        jsonWriter.beginObject();
        jsonWriter.name("seconds").value(log.getTimestampSeconds());
        jsonWriter.name("nanos").value(log.getTimestampNanos());
        jsonWriter.endObject();

        jsonWriter.endObject();

        // Message (限制長度)
        String message = log.getMessage();
        if (message != null && message.length() > 10000) {
            message = message.substring(0, 10000) + "... [訊息過長，已截斷]";
        }
        jsonWriter.name("message").value(message);

        jsonWriter.endObject();
    }

    private boolean isEmulator() {
        return android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(android.os.Build.PRODUCT);
    }
}