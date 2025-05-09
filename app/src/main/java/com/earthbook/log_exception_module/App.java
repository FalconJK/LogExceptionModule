package com.earthbook.log_exception_module;

import android.app.Application;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化 Timber
        Timber.init(this);

        // 植入調試樹
        Timber.plant(new Timber.DebugTree());

//        Timber.d("應用程序已啟動");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        // 釋放 Timber 資源
        Timber.release();
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
