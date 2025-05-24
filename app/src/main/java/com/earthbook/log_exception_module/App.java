package com.earthbook.log_exception_module;

import android.app.Application;

import com.earthbook.log_exception_module.db.DbTree;
import com.earthbook.log_exception_module.timber.DebugTree;
import com.earthbook.log_exception_module.timber.Timber;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 植入調試樹
        Timber.plant(new DebugTree());
        Timber.plant(new Timber.DbTree2(this));
//        Timber.plant(new ToastTree(getApplicationContext()));

//        Timber.d("應用程序已啟動");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        // 低內存時清理資源
        Timber.clearDisposables();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        // 根據內存壓力級別決定是否清理資源
        if (level >= TRIM_MEMORY_COMPLETE) {
            Timber.clearDisposables();
        }
    }
}
