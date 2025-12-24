# RxTimber with DbTree

![](imgs/logdb.gif)
[中文版本](README_zh.md)

A powerful Android logging library that extends Timber functionality, providing database storage, session management, and export capabilities.

## Features

- **Enhanced Logging**: Built on Timber with reactive extensions
- **Database Storage**: Automatically stores logs in local SQLite database
- **Session Management**: Organizes logs into sessions for easy tracking
- **Export Functionality**: Export logs as JSON files or export entire database
- **Log Filtering**: Filter logs by level, session, or keywords
- **Memory Efficient**: Optimized to minimize memory usage
- **Code Location**: Automatically records code location of log source for quick issue identification

## Setup

### 1. Add Setup JitPack Repository

Add the following to your project-level `settings.gradle` (or root `build.gradle`):

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

### 2. Add Dependencies

Add the following to your app's `build.gradle` file:

```gradle
dependencies {
     // Replace 'Tag' with the latest release version (e.g. 1.0.1)
    implementation 'com.github.FalconJK:LogExceptionModule:1.0.1'
}
```

### 2. Initialize in Application Class

```java
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Plant log trees
        Timber.plant(new Timber.DbTree(this));  // Database logging
        Timber.plant(new Timber.DebugTree());   // Console logging (optional)
    }
}
```

## Basic Usage

### Logging Messages

Timber provides various logging methods corresponding to different log levels:

```java
// VERBOSE level logs
Timber.v("Verbose log message");
Timber.v("Formatted verbose message: User %s login time %d", "John", System.currentTimeMillis());
Timber.v(throwable);  // Log exception only
Timber.v(throwable, "Exception occurred: %s", throwable.getMessage());  // Log exception with message

// DEBUG level logs
Timber.d("Debug message");
Timber.d("Formatted debug message: Count=%d, Status=%b", 42, true);
Timber.d(throwable);
Timber.d(throwable, "Debug exception: %s", throwable.getMessage());

// INFO level logs
Timber.i("Info message");
Timber.i("Formatted info: App version %s, API level %d", BuildConfig.VERSION_NAME, Build.VERSION.SDK_INT);
Timber.i(throwable);
Timber.i(throwable, "Info exception: %s", throwable.getMessage());

// WARN level logs
Timber.w("Warning message");
Timber.w("Formatted warning: Memory usage %.2f%%", 75.6f);
Timber.w(throwable);
Timber.w(throwable, "Warning exception: %s", throwable.getMessage());

// ERROR level logs
Timber.e("Error message");
Timber.e("Formatted error: Operation '%s' failed, error code %d", "File upload", 404);
Timber.e(throwable);
Timber.e(throwable, "Error exception: %s", throwable.getMessage());

// ASSERT level logs (WTF = What a Terrible Failure)
Timber.wtf("Critical error message");
Timber.wtf("Formatted critical error: System crashed at %tc", Calendar.getInstance());
Timber.wtf(throwable);
Timber.wtf(throwable, "Critical error: %s", throwable.getMessage());

// Using custom priority
Timber.log(Log.DEBUG, "Custom priority message");
Timber.log(Log.DEBUG, "Formatted custom message: %d - %s", 100, "Test");
Timber.log(Log.ERROR, throwable);
Timber.log(Log.INFO, throwable, "Custom priority exception: %s", throwable.getMessage());
```

### Using Tags

```java
// Set tag for a single log
Timber.tag("CustomTag").d("Debug message with tag");

// Tag only applies to this one log
Timber.tag("Tag1").d("This log uses Tag1");
Timber.d("This log doesn't use Tag1");
```

### Code Location Feature

This library automatically adds code location information to each log in the format `(FileName:LineNumber)`, for example:

```java
Timber.d("Test message");
// Output: (MainActivity.java:42) Test message
```

This allows you to quickly locate the exact source of the log, especially useful when debugging complex issues.

### Viewing Logs

To open the session list activity:

```java
Timber.startSessionActivity(context);
```

This will display a list of all log sessions. From here, you can:
- View logs for each session
- Share logs as JSON files
- Delete sessions
- Export the entire database

## Advanced Features

### Export Database

```java
LogDatabase.getInstance(context)
    .exportDatabase(context)
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(
        filePath -> {
            // Database successfully exported to filePath
        },
        throwable -> {
            // Handle error
        }
    );
```

### Clean Old Logs

```java
// Delete logs older than 7 days
long sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
LogDatabase.getInstance(context)
    .logEntryDao()
    .deleteOldLogs(sevenDaysAgo)
    .subscribeOn(Schedulers.io())
    .subscribe();
```

### Search Logs

```java
LogDatabase.getInstance(context)
    .logEntryDao()
    .searchLogsByKeyword("error", sessionId)
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(logs -> {
        // Handle filtered logs
    });
```

## Custom Tree

You can create your own Tree to customize log handling logic:
1. [ToastTree](app/src/main/java/com/earthbook/log_exception_module/ToastTree.java)
2. [EbDebugTree: let Log start with "myEb"](app/src/main/java/com/earthbook/log_exception_module/EbDebugTree.java) 
```java
public class MyCustomTree extends Tree {
    @Override
    protected void log(int priority, @Nullable String tag, String link, 
                      @NotNull String message, @Nullable Throwable t, 
                      StackTraceElement[] stackTraces) {
        // Custom log handling logic
        String formattedMessage = String.format("(%s) %s", link, message);
        
        // Can filter by priority
        if (priority >= Log.WARN) {
            // Handle warning and above level logs
        }
        
        // Can add custom formatting
        String customFormat = String.format("[%s] %s: %s", 
                                          getLogLevelString(priority), 
                                          tag, 
                                          formattedMessage);
        
        // Can send logs to custom destinations
        // e.g., network service, custom files, Toast
    }
    
    private String getLogLevelString(int priority) {
        switch (priority) {
            case Log.VERBOSE: return "VERBOSE";
            case Log.DEBUG: return "DEBUG";
            case Log.INFO: return "INFO";
            case Log.WARN: return "WARN";
            case Log.ERROR: return "ERROR";
            case Log.ASSERT: return "ASSERT";
            default: return "UNKNOWN";
        }
    }
    
    // Optional: Custom scheduler
    @Override
    protected Scheduler getScheduler() {
        // Return custom scheduler, e.g., computation scheduler
        return AndroidSchedulers.mainThread();
    }
}

// Use custom Tree
Timber.plant(new MyCustomTree());
```

## Best Practices

1. **Clean up subscriptions**: Always dispose of your subscriptions to prevent memory leaks
   ```java
   @Override
   protected void onDestroy() {
       disposables.clear();
       super.onDestroy();
   }
   ```

2. **Use tags effectively**: Tags help organize and filter logs
   ```java
   Timber.tag("NetworkModule").d("API request started");
   ```

3. **Include context in error logs**: Add relevant information to help with debugging
   ```java
   Timber.e("Failed to load user data. User ID: %s, Status code: %d", userId, statusCode);
   ```

4. **Export logs regularly**: For critical applications, consider automatic log export

5. **Leverage code location feature**: Use location information in logs to quickly identify problem sources

## Special Features

### Large Session Handling

When a session contains a large number of logs, the system provides multiple handling options:
- **Export errors only**: Export only ERROR level logs
- **Clean and export**: Delete overly long messages before export
- **Force export**: Export complete session directly (may be large)

### Log Size Estimation

The `LogDbEntry` class provides a `getBytes()` method to estimate the size of log entries, helping manage memory usage:

```java
LogDbEntry entry = new LogDbEntry(/* parameters */);
long sizeInBytes = entry.getBytes();
```

### Auto-flush Mechanism

The logging system has an intelligent buffering mechanism that automatically writes logs to the database in the following situations:
- Buffer size limit reached (default 1000 entries)
- Time threshold reached (default 30 seconds)
- Buffer size exceeds memory limit (default 20MB)
- App goes to background or activity pauses/stops

## License

```
MIT License

Copyright (c) 2025 FalconJK

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```