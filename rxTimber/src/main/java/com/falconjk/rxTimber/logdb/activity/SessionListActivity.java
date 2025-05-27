package com.falconjk.rxTimber.logdb.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.falconjk.rxTimber.R;
import com.falconjk.rxTimber.logdb.Timber;
import com.falconjk.rxTimber.logdb.db.LogDatabase;
import com.falconjk.rxTimber.logdb.db.LogExporter;

import java.io.File;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class SessionListActivity extends AppCompatActivity {
    private static final String TAG = "SessionListActivity";

    private final CompositeDisposable disposables = new CompositeDisposable();
    private ListView listView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private ArrayAdapter<String> adapter;
    private LogExporter logExporter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_list);

        initViews();
        setupAdapter();
        loadSessions();

        logExporter = new LogExporter(this);
    }

    private void initViews() {
        listView = findViewById(R.id.listView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);

        // 設置空視圖
        listView.setEmptyView(emptyView);

        // 設置點擊監聽器
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String sessionId = adapter.getItem(position);
            if (sessionId != null) {
                exportAndShareSession(sessionId);
            }
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            String sessionId = adapter.getItem(position);
            if (sessionId != null) {
                deleteSession(sessionId);
                return true;
            }
            return false;
        });

        findViewById(R.id.btn_clear_all_log).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("刪除 Session")
                    .setMessage("確定要全部 Session 嗎？")
                    .setPositiveButton("確定", (dialog, which) -> {
                        disposables.add(LogDatabase.getInstance(SessionListActivity.this)
                                .logEntryDao()
                                .deleteAllLogs()
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(
                                        () -> {
                                            Toast.makeText(this, "刪除全部 Session 成功", Toast.LENGTH_SHORT).show();
                                            loadSessions();
                                        },
                                        throwable -> {
                                            Log.e(TAG, "Error deleting session", throwable);
                                            Toast.makeText(this, "刪除全部 Session 失敗: " + throwable.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                )
                        );
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        findViewById(R.id.btn_export_db).setOnClickListener(v -> {
            Context context = SessionListActivity.this;
            Disposable disposable = LogDatabase.getInstance(context).exportDatabase(context)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            filePath -> {
                                Timber.d("資料庫導出成功: %s", filePath);
                                showExportSuccessDialog(filePath);
                            },
                            throwable -> {
                                Toast.makeText(context, "資料庫導出失敗: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                Timber.e(throwable, "資料庫導出失敗");
                            }
                    );
            disposables.add(disposable);
        });
    }

    private void showExportSuccessDialog(String filePath) {
        new AlertDialog.Builder(this)
                .setTitle("導出成功")
                .setMessage("資料庫已導出到:\n" + filePath + "\n\n是否要分享該文件？")
                .setPositiveButton("分享", (dialog, which) -> shareDatabaseFile(filePath))
                .setNegativeButton("關閉", null)
                .show();
    }

    private void shareDatabaseFile(String filePath) {
        try {
            File dbFile = new File(filePath);
            if (!dbFile.exists()) {
                Toast.makeText(this, "找不到資料庫文件", Toast.LENGTH_SHORT).show();
                return;
            }

            // 使用正確的 authority - 確保與 AndroidManifest.xml 中的一致
            String authority = getPackageName() + ".fileprovider";

            // 使用FileProvider獲取Uri
            Uri contentUri = FileProvider.getUriForFile(
                    this,
                    authority,  // 確保這個 authority 與 manifest 中的一致
                    dbFile);

            // 創建分享文件的Intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.setType("application/octet-stream");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // 顯示分享選擇器
            Intent chooser = Intent.createChooser(shareIntent, "分享資料庫文件");
            if (shareIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(chooser);
            } else {
                Toast.makeText(this, "沒有找到可以分享此文件的應用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Timber.e(e, "分享資料庫文件失敗: %s", e.getMessage());
            Toast.makeText(this, "分享資料庫文件失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteSession(String sessionId) {
        new AlertDialog.Builder(this)
                .setTitle("刪除 Session")
                .setMessage("確定要刪除這個 Session 嗎？")
                .setPositiveButton("確定", (dialog, which) -> deleteSessionFromDatabase(sessionId))
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteSessionFromDatabase(String sessionId) {
        Disposable disposable = LogDatabase.getInstance(this)
                .logEntryDao()
                .deleteLogsBySession(sessionId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            Toast.makeText(this, "刪除 Session 成功", Toast.LENGTH_SHORT).show();
                            loadSessions();
                        },
                        throwable -> {
                            Log.e(TAG, "Error deleting session", throwable);
                            Toast.makeText(this, "刪除 Session 失敗: " + throwable.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                );
        disposables.add(disposable);
    }

    private void setupAdapter() {
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(adapter);
    }

    private void loadSessions() {
        showLoading(true);

        Disposable disposable = LogDatabase.getInstance(this)
                .logEntryDao()
                .getAllSessions()
                .firstOrError()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        sessions -> {
                            showLoading(false);
                            updateSessionList(sessions);
                        },
                        throwable -> {
                            showLoading(false);
                            Log.e(TAG, "Error loading sessions", throwable);
                            Toast.makeText(this, "載入 Session 失敗: " + throwable.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                );

        disposables.add(disposable);
    }

    private void updateSessionList(List<String> sessions) {
        adapter.clear();
        adapter.addAll(sessions);
        adapter.notifyDataSetChanged();

        if (sessions.isEmpty()) {
            emptyView.setText("沒有找到任何 Session");
        }
    }

    private void exportAndShareSession(String sessionId) {
        // 先檢查 session 大小
        showLoading(true);

        Disposable sizeCheckDisposable = LogDatabase.getInstance(this)
                .logEntryDao()
                .getLogCountForSession(sessionId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        count -> {
                            if (count > 10000) { // 超過 10000 筆記錄
                                showLoading(false);
                                showLargeSessionDialog(sessionId, count);
                            } else {
                                // 直接匯出
                                performExport(sessionId);
                            }
                        },
                        throwable -> {
                            showLoading(false);
                            Log.e(TAG, "Error checking session size", throwable);
                            Toast.makeText(this, "檢查 Session 大小失敗: " + throwable.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                );

        disposables.add(sizeCheckDisposable);
    }

    private void showLargeSessionDialog(String sessionId, int count) {
        new AlertDialog.Builder(this)
                .setTitle("大型 Session 警告")
                .setMessage("這個 Session 包含 " + count + " 筆記錄，可能會很大。\n\n請選擇處理方式：")
                .setPositiveButton("僅匯出錯誤日誌", (dialog, which) -> {
                    exportErrorLogsOnly(sessionId);
                })
                .setNeutralButton("清理後匯出", (dialog, which) -> {
                    cleanAndExportSession(sessionId);
                })
                .setNegativeButton("強制匯出", (dialog, which) -> {
                    performExport(sessionId);
                })
                .show();
    }

    private void exportErrorLogsOnly(String sessionId) {
        showLoading(true);
        Toast.makeText(this, "正在匯出錯誤日誌...", Toast.LENGTH_SHORT).show();

        // 這裡可以實作只匯出 ERROR 級別的日誌
        // 需要在 LogExporter 中添加相應方法
        performExport(sessionId);
    }

    private void cleanAndExportSession(String sessionId) {
        new AlertDialog.Builder(this)
                .setTitle("清理選項")
                .setMessage("選擇要清理的內容：")
                .setPositiveButton("刪除超長訊息", (dialog, which) -> {
                    cleanLargeMessages(sessionId);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void cleanLargeMessages(String sessionId) {
        showLoading(true);

        Disposable cleanDisposable = LogDatabase.getInstance(this)
                .logEntryDao()
                .deleteLargeMessages(sessionId, 5000) // 刪除超過 5000 字元的訊息
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            Toast.makeText(this, "清理完成，開始匯出...", Toast.LENGTH_SHORT).show();
                            performExport(sessionId);
                        },
                        throwable -> {
                            showLoading(false);
                            Log.e(TAG, "Error cleaning session", throwable);
                            Toast.makeText(this, "清理失敗: " + throwable.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                );

        disposables.add(cleanDisposable);
    }

    private void performExport(String sessionId) {
        Toast.makeText(this, "正在匯出 " + sessionId + "...", Toast.LENGTH_SHORT).show();
        showLoading(true);
        Disposable disposable = logExporter.exportSessionLogs(sessionId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        file -> {
                            showLoading(false);
                            shareFile(file, sessionId);
                        },
                        throwable -> {
                            showLoading(false);
                            Log.e(TAG, "Error exporting session", throwable);

                            String errorMessage = throwable.getMessage();
                            if (errorMessage != null && errorMessage.contains("太大")) {
                                Toast.makeText(this, "匯出失敗：" + errorMessage +
                                        "\n建議使用「清理後匯出」選項", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this, "匯出失敗: " + errorMessage,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                );

        disposables.add(disposable);
    }

    private void shareFile(File file, String sessionId) {
        try {
            // 使用 FileProvider 創建安全的 URI
            Uri fileUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/json");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Log Session: " + sessionId);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "分享 Log Session: " + sessionId);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(shareIntent, "分享 Log 檔案");
            startActivity(chooser);

        } catch (Exception e) {
            Log.e(TAG, "Error sharing file", e);
            Toast.makeText(this, "分享失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        listView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}