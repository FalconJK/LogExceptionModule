package com.earthbook.log_exception_module;

import android.content.Context;
import android.util.Log;

import com.earthbook.log_exception_module.logdb.core.Tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Scheduler;

public class ToastTree extends Tree {
    public final Context context;

    public ToastTree(Context context) {
        this.context = context;
    }

    @Override
    protected void log(int priority, @Nullable String tag, String link, @NotNull String message, @Nullable Throwable t, StackTraceElement[] stackTraces) {
//        Toast.makeText(context, "[" + tag + "]" + message, Toast.LENGTH_SHORT).show();
        for (StackTraceElement stackTrace : stackTraces) {
            Log.d(this.getClass().getSimpleName(), stackTrace.getFileName());
        }
    }

    @Override
    protected Scheduler getScheduler() {
        return AndroidSchedulers.mainThread();
    }
}
