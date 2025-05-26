package com.falconjk.mylibrary.logdb.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {LogDbEntry.class}, version = 1, exportSchema = false)
public abstract class LogDatabase extends RoomDatabase {
    private static volatile LogDatabase INSTANCE;

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
                            "log_database"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
