package com.falconjk.rxTimber.logdb.db;

import android.content.Context;
import android.os.Environment;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.falconjk.rxTimber.logdb.Timber;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.reactivex.rxjava3.core.Single;

@Database(entities = {LogDbEntry.class}, version = 1, exportSchema = false)
public abstract class LogDatabase extends RoomDatabase {
    private static volatile LogDatabase INSTANCE;
    private static String DATABASE_NAME = "log_database";

    public static void init(Context context) {
        if (INSTANCE == null) {
            synchronized (LogDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            LogDatabase.class,
                            "log_database"
                    ).build();
                }
            }
        }
    }

    public abstract LogDbEntryDao logEntryDao();

    public static LogDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (LogDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            LogDatabase.class,
                            DATABASE_NAME
                    ).build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 導出整個資料庫到外部存儲
     * @param context 應用上下文
     * @return 包含導出路徑的Single
     */
    public Single<String> exportDatabase(Context context) {
        return Single.create(emitter -> {
            try {
                // 獲取當前資料庫文件
                File currentDB = context.getDatabasePath(DATABASE_NAME);
                if (!currentDB.exists()) {
                    emitter.onError(new IOException("資料庫文件不存在"));
                    return;
                }

                // 創建目標目錄
                File exportDir = new File(context.getExternalFilesDir(null), "db");
                if (!exportDir.exists()) {
                    if (!exportDir.mkdirs()) {
                        emitter.onError(new IOException("無法創建導出目錄"));
                        return;
                    }
                }

                // 生成帶時間戳的文件名
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", Locale.getDefault());
                String timestamp = dateFormat.format(new Date());
                String backupDBPath = "LogDb_" + timestamp + ".db";
                File backupDB = new File(exportDir, backupDBPath);

                // 複製資料庫文件
                FileChannel src = new FileInputStream(currentDB).getChannel();
                FileChannel dst = new FileOutputStream(backupDB).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();

                Timber.d("資料庫導出成功: %s", backupDB.getAbsolutePath());
                emitter.onSuccess(backupDB.getAbsolutePath());
            } catch (Exception e) {
                Timber.e(e, "資料庫導出失敗");
                emitter.onError(e);
            }
        });
    }
}
