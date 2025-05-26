package com.earthbook.log_exception_module.logdb.core;

import androidx.annotation.Nullable;

public class StackInfo {
    @Nullable
    public final String tag;
    @Nullable
    public final String link;

    public StackInfo(@Nullable String tag, @Nullable String link) {
        this.tag = tag;
        this.link = link;
    }
}
