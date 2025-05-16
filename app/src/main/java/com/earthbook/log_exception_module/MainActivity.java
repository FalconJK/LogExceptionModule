package com.earthbook.log_exception_module;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.earthbook.log_exception_module.log.Timber;
import com.earthbook.log_exception_module.log.Tree;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private CompositeDisposable disposables = new CompositeDisposable();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 初始化所有按鈕
        initButtons();
    }

    private void initButtons() {
        // Debug 日誌測試
        Button btnTestDebug = findViewById(R.id.btnTestDebug);
        btnTestDebug.setOnClickListener(v -> {
            Timber.d("這是一條調試日誌");
        });

        // Verbose 日誌測試
        Button btnTestVerbose = findViewById(R.id.btnTestVerbose);
        btnTestVerbose.setOnClickListener(v -> {
            Timber.v("這是一條詳細日誌");
        });

        // Info 日誌測試
        Button btnTestInfo = findViewById(R.id.btnTestInfo);
        btnTestInfo.setOnClickListener(v -> {
            Timber.i("這是一條信息日誌");
        });

        // Warning 日誌測試
        Button btnTestWarning = findViewById(R.id.btnTestWarning);
        btnTestWarning.setOnClickListener(v -> {
            Timber.w("這是一條警告日誌");
        });

        // Error 日誌測試
        Button btnTestError = findViewById(R.id.btnTestError);
        btnTestError.setOnClickListener(v -> {
            Timber.e("這是一條錯誤日誌");
        });

        // WTF 日誌測試
        Button btnTestWtf = findViewById(R.id.btnTestWtf);
        btnTestWtf.setOnClickListener(v -> {
            Timber.wtf("這是一條嚴重錯誤日誌");
        });

        // 異常日誌測試
        Button btnTestException = findViewById(R.id.btnTestException);
        btnTestException.setOnClickListener(v -> {
            try {
                throw new RuntimeException("測試異常");
            } catch (Exception e) {
                Timber.e(e, "捕獲到異常");
            }
        });

        // 自定義標籤測試
        Button btnTestCustomTag = findViewById(R.id.btnTestCustomTag);
        btnTestCustomTag.setOnClickListener(v -> {
            Timber.tag("CustomTag").v("這是一條帶有自定義標籤的日誌");
            Timber.tag("CustomTag").d("這是一條帶有自定義標籤的日誌");
            Timber.tag("CustomTag").i("這是一條帶有自定義標籤的日誌");
            Timber.tag("CustomTag").w("這是一條帶有自定義標籤的日誌");
            Timber.tag("CustomTag").e("這是一條帶有自定義標籤的日誌");
            Timber.tag("CustomTag").wtf("這是一條帶有自定義標籤的日誌");

        });

        // 格式化日誌測試
        Button btnTestFormatting = findViewById(R.id.btnTestFormatting);
        btnTestFormatting.setOnClickListener(v -> {
            Timber.d("格式化測試: %d, %s, %.2f", 123, "字符串", 3.14159);
        });

        // 多樹植入測試
        Button btnTestMultipleTree = findViewById(R.id.btnTestMultipleTree);
        btnTestMultipleTree.setOnClickListener(v -> {
            // 創建自定義樹
            Tree customTree = new Tree() {
                @Override
                protected void log(int priority, String tag, String message, Throwable t) {
                    // 將日誌輸出到 Toast

                    Log.println(priority, "自定義" + tag, message);

//                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
//                            "自定義樹: " + message, Toast.LENGTH_SHORT).show());
                }
            };

            // 植入自定義樹
            Timber.plant(customTree);

            // 測試日誌
            Timber.d("這條日誌會同時發送到 Logcat 和 Toast");

            // 測試完成後移除自定義樹
            Disposable disposable = Observable.timer(20, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(aLong -> {
                        Timber.uproot(customTree);
                    });

            disposables.add(disposable);
        });

        // 清理訂閱測試
        Button btnClearDisposables = findViewById(R.id.btnClearDisposables);
        btnClearDisposables.setOnClickListener(v -> {
            Timber.clearDisposables();
            disposables.clear();
        });

        // 測試應用程序生命週期
        Button btnTestAppLifecycle = findViewById(R.id.btnTestAppLifecycle);
        btnTestAppLifecycle.setOnClickListener(v -> {
            // 模擬應用程序進入後台
//            showToast("請按 Home 鍵將應用置於後台，觀察生命週期日誌");

            // 添加一個延遲任務，模擬後台任務
            Disposable disposable = Observable.timer(5, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(aLong -> {
                        // 這個任務會在應用進入後台 5 秒後執行
                        Timber.d("後台任務執行");
                    });

            // 將訂閱添加到 Timber 的 CompositeDisposable
            disposables.add(disposable);
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 清理 Activity 的訂閱
        disposables.clear();
    }
}
