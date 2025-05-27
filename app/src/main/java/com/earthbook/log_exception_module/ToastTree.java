package com.earthbook.log_exception_module;

import android.content.Context;
import android.widget.Toast;


import com.falconjk.rxTimber.logdb.core.Tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Scheduler;

public class ToastTree extends Tree {
    public final Context context;
    private final AtomicReference<Toast> atomicToaster = new AtomicReference<>();

    public ToastTree(Context context) {
        this.context = context;
    }

    @Override
    protected void log(int priority, @Nullable String tag, String link, @NotNull String message, @Nullable Throwable t, StackTraceElement[] stackTraces) {
        Toast oldToast = atomicToaster.getAndSet(Toast.makeText(context, "[" + tag + "]" + message, Toast.LENGTH_SHORT));
        if (oldToast != null) {
            oldToast.cancel();
        }
        atomicToaster.get().show();
    }

    @Override
    protected Scheduler getScheduler() {
        return AndroidSchedulers.mainThread();
//        return Schedulers.io();
    }
}
