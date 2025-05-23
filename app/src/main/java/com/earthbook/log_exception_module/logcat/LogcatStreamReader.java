package com.earthbook.log_exception_module.logcat;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

public class LogcatStreamReader implements Iterator<Log>, Closeable {
    private final BufferedReader reader;
    private final StringBuilder msgBuffer = new StringBuilder();
    private Log log;
    private int id = 0;

    public LogcatStreamReader(InputStream inputStream) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
    }

    @Override
    public boolean hasNext() {
        try {
            while (true) {
                String metadata = reader.readLine();
                if (metadata == null) {
                    return false;
                }
                metadata = metadata.trim();

                if (metadata.startsWith("[")) {
                    String msg = reader.readLine();
                    if (msg == null) {
                        return false;
                    }
                    msgBuffer.append(msg);

                    msg = reader.readLine();
                    if (msg == null) {
                        return false;
                    }

                    while (!msg.isEmpty()) {
                        msgBuffer.append("\n").append(msg);
                        msg = reader.readLine();
                        if (msg == null) {
                            return false;
                        }
                    }

                    try {
                        log = Log.parse(id, metadata, msgBuffer.toString());
                        id++;
                        return true;
                    } catch (Exception e) {
                        // Logger.debug(Logcat.class, e.getMessage() + ": " + metadata);
                        return false;
                    } finally {
                        msgBuffer.setLength(0);
                    }
                }
            }
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public Log next() {
        return log;
    }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException ignored) {
            // Ignore exception
        }
    }
}
