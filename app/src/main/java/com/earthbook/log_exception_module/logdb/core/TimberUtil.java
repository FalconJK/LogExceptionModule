package com.earthbook.log_exception_module.logdb.core;

import android.os.Build;

import com.earthbook.log_exception_module.logdb.Timber;

import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Timber 工具類
 */
public class TimberUtil {
    private static final List<String> TIMBER_CLASSES = List.of(
            Timber.class.getName(),
            Forest.class.getName(),
            Tree.class.getName(),
            TimberUtil.class.getName(),
            TimberProcessor.class.getName(),
            TaggedTree.class.getName()
    );

    private static final int MAX_TAG_LENGTH = 23;
    private static final Pattern ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$");

    private TimberUtil() {
        // 防止實例化
    }

    /**
     * 從 {@code element} 中提取應該用於消息的標籤。
     */
    @Nullable
    public static String createStackElementTag() {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        if (stackTrace.length <= 2) {
            return null;
        }

        // 尋找第一個非 Timber 類的調用者
        for (int i = 2; i < stackTrace.length; i++) {
            String className = stackTrace[i].getClassName();
            if (!TIMBER_CLASSES.contains(className)) {
                String tag = className.substring(className.lastIndexOf('.') + 1);
                Matcher m = ANONYMOUS_CLASS.matcher(tag);
                if (m.find()) {
                    tag = m.replaceAll("");
                }
                // API 26 中刪除了標籤長度限制。
                if (tag.length() <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= 26) {
                    return tag;
                } else {
                    return tag.substring(0, MAX_TAG_LENGTH);
                }
            }
        }

        return null;
    }

    @Nullable
    public static String getLogLink() {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        if (stackTrace.length <= 2) {
            return null;
        }

        // 尋找第一個非 Timber 類的調用者
        for (int i = 2; i < stackTrace.length; i++) {
            String className = stackTrace[i].getClassName();
            if (!TIMBER_CLASSES.contains(className)) {
                String fileName = stackTrace[i].getFileName();
                String fileInfo;

                if (fileName != null) {
                    fileInfo = fileName;
                } else {
                    // fallback 到類名（移除包名和匿名類後綴）
                    fileInfo = className.substring(className.lastIndexOf('.') + 1);
                    Matcher m = ANONYMOUS_CLASS.matcher(fileInfo);
                    if (m.find()) {
                        fileInfo = m.replaceAll("");
                    }
                }

                return fileInfo + ":" + stackTrace[i].getLineNumber();
            }
        }

        return null;
    }

    @Nullable
    public static StackInfo createStackElementInfo() {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        if (stackTrace.length <= 2) {
            return null;
        }

        // 尋找第一個非 Timber 類的調用者
        for (int i = 2; i < stackTrace.length; i++) {
            String className = stackTrace[i].getClassName();
            if (!TIMBER_CLASSES.contains(className)) {
                // 創建 tag
                String tag = className.substring(className.lastIndexOf('.') + 1);
                Matcher m = ANONYMOUS_CLASS.matcher(tag);
                if (m.find()) {
                    tag = m.replaceAll("");
                }
                // API 26 中刪除了標籤長度限制
                if (tag.length() > MAX_TAG_LENGTH && Build.VERSION.SDK_INT < 26) {
                    tag = tag.substring(0, MAX_TAG_LENGTH);
                }

                // 創建 link
                String fileName = stackTrace[i].getFileName();
                String fileInfo;

                if (fileName != null) {
                    fileInfo = fileName;
                } else {
                    // fallback 到類名（移除包名和匿名類後綴）
                    fileInfo = className.substring(className.lastIndexOf('.') + 1);
                    Matcher matcher = ANONYMOUS_CLASS.matcher(fileInfo);
                    if (matcher.find()) {
                        fileInfo = matcher.replaceAll("");
                    }
                }

                String link = fileInfo + ":" + stackTrace[i].getLineNumber();

                return new StackInfo(tag, link);
            }
        }

        return null;
    }

    public static String getStackTraceString(Throwable t) {
        // 不要用 Log.getStackTraceString() 替換這個 - 它會隱藏 UnknownHostException，這不是我們想要的。
        StringWriter sw = new StringWriter(256);
        PrintWriter pw = new PrintWriter(sw, false);
        t.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
}
