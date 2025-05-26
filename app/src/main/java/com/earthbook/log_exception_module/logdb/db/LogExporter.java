package com.earthbook.log_exception_module.logdb.db;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LogExporter {
    private static final String TAG = "LogExporter";

    private final Context context;
    private final Gson gson;

    public LogExporter(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public Single<File> exportSessionLogs(String sessionId) {
        return LogDatabase.getInstance(context).logEntryDao().getLogsBySession(sessionId)
                .firstOrError()
                .map(logs -> {
                    // 建立匯出檔案
                    File exportDir = new File(context.getExternalFilesDir(null), "logs");
                    if (!exportDir.exists() && !exportDir.mkdirs()) {
                        throw new IOException("Failed to create export directory");
                    }

                    String fileName = sessionId.replace(":", "-") + ".logcat";
                    File exportFile = new File(exportDir, fileName);

                    // 建立 JSON 結構
                    JsonObject rootObject = new JsonObject();

                    // 添加 metadata
                    JsonObject metadata = new JsonObject();
                    JsonObject device = new JsonObject();
                    device.addProperty("deviceId", android.os.Build.SERIAL);
                    device.addProperty("name", android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
                    device.addProperty("serialNumber", android.os.Build.SERIAL);
                    device.addProperty("isOnline", true);
                    device.addProperty("release", android.os.Build.VERSION.RELEASE);
                    device.addProperty("sdk", android.os.Build.VERSION.SDK_INT);
                    device.addProperty("featureLevel", android.os.Build.VERSION.SDK_INT);
                    device.addProperty("model", android.os.Build.MODEL);
                    device.addProperty("type", "HANDHELD");
                    device.addProperty("isEmulator", isEmulator());

                    metadata.add("device", device);
                    metadata.addProperty("filter", "package:" + context.getPackageName());

                    JsonArray projectApplicationIds = new JsonArray();
                    projectApplicationIds.add(context.getPackageName());
                    projectApplicationIds.add(context.getPackageName() + ".test");
                    metadata.add("projectApplicationIds", projectApplicationIds);

                    rootObject.add("metadata", metadata);

                    // 添加日誌
                    JsonArray logcatMessages = new JsonArray();
                    for (LogDbEntry log : logs) {
                        JsonObject logObject = new JsonObject();

                        JsonObject header = new JsonObject();
                        header.addProperty("logLevel", log.getLogLevel());
                        header.addProperty("pid", log.getPid());
                        header.addProperty("tid", log.getTid());
                        header.addProperty("applicationId", log.getApplicationId());
                        header.addProperty("processName", log.getProcessName());
                        header.addProperty("tag", log.getTag());

                        JsonObject timestamp = new JsonObject();
                        timestamp.addProperty("seconds", log.getTimestampSeconds());
                        timestamp.addProperty("nanos", log.getTimestampNanos());
                        header.add("timestamp", timestamp);

                        logObject.add("header", header);
                        logObject.addProperty("message", log.getMessage());

                        logcatMessages.add(logObject);
                    }

                    rootObject.add("logcatMessages", logcatMessages);

                    // 寫入檔案
                    try (FileWriter writer = new FileWriter(exportFile)) {
                        writer.write(gson.toJson(rootObject));
                    }

                    return exportFile;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
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
