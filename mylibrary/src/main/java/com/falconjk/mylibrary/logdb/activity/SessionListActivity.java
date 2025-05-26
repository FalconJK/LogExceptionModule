package com.falconjk.mylibrary.logdb.activity;

import android.app.AlertDialog;
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

import com.falconjk.mylibrary.R;
import com.falconjk.mylibrary.logdb.db.LogDatabase;
import com.falconjk.mylibrary.logdb.db.LogExporter;

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
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String sessionId = adapter.getItem(position);
                if (sessionId != null) {
                    exportAndShareSession(sessionId);
                }
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String sessionId = adapter.getItem(position);
                if (sessionId != null) {
                    deleteSession(sessionId);
                    return true;
                }
                return false;
            }
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
        showLoading(true);
        Toast.makeText(this, "正在匯出 " + sessionId + "...", Toast.LENGTH_SHORT).show();

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
                            Toast.makeText(this, "匯出失敗: " + throwable.getMessage(),
                                    Toast.LENGTH_SHORT).show();
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