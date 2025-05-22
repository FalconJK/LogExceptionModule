package com.earthbook.log_exception_module.logcat;

import android.os.Parcel;
import android.os.Parcelable;

public class Log implements Parcelable {
    private final int id;
    private final String date;
    private final String time;
    private final Uid uid;
    private final String pid;
    private final String tid;
    private final String priority;
    private final String tag;
    private final String msg;

    public Log(int id, String date, String time, Uid uid, String pid, String tid, String priority, String tag, String msg) {
        this.id = id;
        this.date = date;
        this.time = time;
        this.uid = uid;
        this.pid = pid;
        this.tid = tid;
        this.priority = priority;
        this.tag = tag;
        this.msg = msg;
    }

    public static class Uid implements Parcelable {
        private final String value;
        private final boolean isNum;

        public Uid(String value) {
            this.value = value;
            this.isNum = isDigitsOnly(value);
        }

        private boolean isDigitsOnly(String str) {
            if (str == null || str.isEmpty()) {
                return false;
            }
            for (int i = 0; i < str.length(); i++) {
                if (!Character.isDigit(str.charAt(i))) {
                    return false;
                }
            }
            return true;
        }

        public String getValue() {
            return value;
        }

        public boolean isNum() {
            return isNum;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(value);
        }

        public static final Creator<Uid> CREATOR = new Creator<Uid>() {
            @Override
            public Uid createFromParcel(Parcel in) {
                return new Uid(in.readString());
            }

            @Override
            public Uid[] newArray(int size) {
                return new Uid[size];
            }
        };
    }

    public String metadataToString() {
        return "[" + date + " " + time + " " + (uid != null ? uid.getValue() : "") + ":" + pid + ":" + tid + " " + priority + "/" + tag + "]";
    }

    @Override
    public String toString() {
        return metadataToString() + "\n" + msg + "\n\n";
    }

    public static Log parse(int id, String metadata, String msg) {
        String date;
        String time;
        String uidStr = null;
        String pid;
        String tid;
        String priority;
        String tag;

        String trimmedMetadata = metadata.substring(1, metadata.length() - 1).trim();
        int startIndex = 0;

        int index = trimmedMetadata.indexOf(' ', startIndex);
        date = trimmedMetadata.substring(startIndex, index);
        startIndex = index + 1;

        index = trimmedMetadata.indexOf(' ', startIndex);
        time = trimmedMetadata.substring(startIndex, index);
        startIndex = index + 1;

        // Skip spaces
        while (startIndex < trimmedMetadata.length() && trimmedMetadata.charAt(startIndex) == ' ') {
            startIndex++;
        }

        boolean hasUid = trimmedMetadata.substring(
                startIndex,
                trimmedMetadata.indexOf('/', startIndex)
        ).chars().filter(ch -> ch == ':').count() == 2;

        if (hasUid) {
            index = trimmedMetadata.indexOf(':', startIndex);
            uidStr = trimmedMetadata.substring(startIndex, index);
            startIndex = index + 1;

            // Skip spaces
            while (startIndex < trimmedMetadata.length() && trimmedMetadata.charAt(startIndex) == ' ') {
                startIndex++;
            }
        }

        index = trimmedMetadata.indexOf(':', startIndex);
        pid = trimmedMetadata.substring(startIndex, index);
        startIndex = index + 1;

        // Skip spaces
        while (startIndex < trimmedMetadata.length() && trimmedMetadata.charAt(startIndex) == ' ') {
            startIndex++;
        }

        index = trimmedMetadata.indexOf(' ', startIndex);
        tid = trimmedMetadata.substring(startIndex, index);
        startIndex = index + 1;

        index = trimmedMetadata.indexOf('/', startIndex);
        priority = trimmedMetadata.substring(startIndex, index);
        startIndex = index + 1;

        tag = trimmedMetadata.substring(startIndex).trim();

        Uid uid = uidStr != null ? new Uid(uidStr) : null;

        return new Log(id, date, time, uid, pid, tid, priority, tag, msg);
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public Uid getUid() {
        return uid;
    }

    public String getPid() {
        return pid;
    }

    public String getTid() {
        return tid;
    }

    public String getPriority() {
        return priority;
    }

    public String getTag() {
        return tag;
    }

    public String getMsg() {
        return msg;
    }

    // Parcelable implementation
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(date);
        dest.writeString(time);
        dest.writeParcelable(uid, flags);
        dest.writeString(pid);
        dest.writeString(tid);
        dest.writeString(priority);
        dest.writeString(tag);
        dest.writeString(msg);
    }

    public static final Creator<Log> CREATOR = new Creator<Log>() {
        @Override
        public Log createFromParcel(Parcel in) {
            int id = in.readInt();
            String date = in.readString();
            String time = in.readString();
            Uid uid = in.readParcelable(Uid.class.getClassLoader());
            String pid = in.readString();
            String tid = in.readString();
            String priority = in.readString();
            String tag = in.readString();
            String msg = in.readString();
            return new Log(id, date, time, uid, pid, tid, priority, tag, msg);
        }

        @Override
        public Log[] newArray(int size) {
            return new Log[size];
        }
    };
}
