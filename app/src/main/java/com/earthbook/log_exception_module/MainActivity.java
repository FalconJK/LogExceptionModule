package com.earthbook.log_exception_module;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.earthbook.log_exception_module.logcat.Log;
import com.earthbook.log_exception_module.logcat.LogcatSession;
import com.earthbook.log_exception_module.timber.Timber;

import java.util.Set;
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ScrollView scrollView = findViewById(R.id.scrollView);
        TextView textView = findViewById(R.id.textview);

        findViewById(R.id.btn_log).setOnClickListener(v -> Timber.d("Hello, World!"));
        findViewById(R.id.btn_tag).setOnClickListener(v -> Timber.tag("456").d("Hello, World!"));
        findViewById(R.id.btn_sessions).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SessionListActivity.class);
            startActivity(intent);
        });



        textView.append("myUid: " + android.os.Process.myUid());
        textView.append("GIT_SHA: " + BuildConfig.GIT_SHA);
        textView.append("BUILD_TIME: " + BuildConfig.BUILD_TIME);
        LogcatSession logcatSession = new LogcatSession(1000, Set.of("main"));
        // 訂閱狀態流
        logcatSession.clearLogs();
        Disposable startLogcat = logcatSession.start()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(status -> Timber.d("Logcat started: " + status.isSuccess()),
                        throwable -> Timber.d("Error starting logcat: " + throwable.getMessage()));
        disposables.add(startLogcat);

        Disposable disposable = logcatSession.getLogs()
                .subscribeOn(Schedulers.computation())
                .map(logs -> {
                    StringBuilder sb = new StringBuilder();
                    for (Log log : logs) {
                        sb.append(log.getTag()).append(" :")
                                .append(log.getMsg()).append("\n");
                    }
                    return sb;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sb -> {
                            textView.append(sb);
                            scrollView.fullScroll(View.FOCUS_DOWN);
                        },
                        throwable -> System.err.println("Error receiving logs: " + throwable.getMessage()));
        disposables.add(disposable);

        Disposable makeLogDisposable = Observable.interval(0, 200, TimeUnit.MILLISECONDS)
                .subscribe(i -> {
                    Timber.d(i.toString());
                    Timber.e(i.toString());
                });
        disposables.add(makeLogDisposable);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理 Activity 的訂閱
        disposables.clear();
    }
}
