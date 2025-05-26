package com.falconjk.rxTimber.logdb;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.database.Cursor;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.falconjk.rxTimber.R;

import java.util.Arrays;

public class ShortcutInstaller extends ContentProvider {

    private Context context;

    @Override
    public boolean onCreate() {
        context = getContext();
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            installShortcuts(context);
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private void installShortcuts(Context context) {
        try {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            if (shortcutManager == null) {
                return;
            }

            // 創建 SessionList 快捷方式
            Intent sessionListIntent = new Intent();
            sessionListIntent.setClassName(context.getPackageName(),
                    "com.falconjk.rxTimber.logdb.activity.SessionListActivity");
            sessionListIntent.setAction(Intent.ACTION_VIEW);

            ShortcutInfo sessionListShortcut = new ShortcutInfo.Builder(context, "session_list")
                    .setShortLabel(context.getString(R.string.session_list_short))
                    .setLongLabel(context.getString(R.string.session_list_long))
                    .setIcon(Icon.createWithResource(context, R.drawable.log_45))
                    .setIntent(sessionListIntent)
                    .build();
            
            // 設置動態快捷方式
            shortcutManager.setDynamicShortcuts(Arrays.asList(sessionListShortcut));

        } catch (Exception e) {
            // 靜默處理錯誤，避免影響應用啟動
            e.printStackTrace();
        }
    }

    // ContentProvider 必需的方法實現
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
                        @Nullable String selection, @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}