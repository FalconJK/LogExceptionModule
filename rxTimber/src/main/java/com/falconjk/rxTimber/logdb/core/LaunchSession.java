package com.falconjk.rxTimber.logdb.core;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LaunchSession {

    private final String session;

    private static final class InstanceHolder {
        @SuppressLint("SimpleDateFormat")
        static final LaunchSession instance = new LaunchSession(
                "LaunchSession_" +
                        new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS")
                                .format(new Date())
        );
    }

    public LaunchSession(String session) {
        this.session = session;
    }

    private static LaunchSession getInstance() {
        return InstanceHolder.instance;
    }

    public static String getSession() {
        return getInstance().session;
    }
}
