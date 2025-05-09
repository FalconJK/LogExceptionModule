package com.earthbook.log_exception_module;


import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class DebugTree2 extends Timber.DebugTree {
    @Override
    protected void log(int priority, @Nullable String tag, @NonNull String message, @Nullable Throwable t) {
//        super.log(priority, tag, message, t);
        Log.println(priority, tag, message+" DebugTree2");
    }
}
