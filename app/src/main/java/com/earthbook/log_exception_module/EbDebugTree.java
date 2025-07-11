package com.earthbook.log_exception_module;

import com.falconjk.rxTimber.logdb.Timber;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EbDebugTree extends Timber.DebugTree {
    @Override
    protected void log(int priority, @Nullable String tag, String link, @NotNull String message, @Nullable Throwable t, StackTraceElement[] stackTraces) {
        super.log(priority, "myEb" + tag, link, message, t, stackTraces);
    }
}
