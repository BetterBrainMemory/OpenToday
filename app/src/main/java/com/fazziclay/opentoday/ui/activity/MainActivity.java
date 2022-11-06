package com.fazziclay.opentoday.ui.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.fazziclay.opentoday.R;
import com.fazziclay.opentoday.app.App;
import com.fazziclay.opentoday.app.FeatureFlag;
import com.fazziclay.opentoday.app.Telemetry;
import com.fazziclay.opentoday.app.receiver.QuickNoteReceiver;
import com.fazziclay.opentoday.app.updatechecker.UpdateChecker;
import com.fazziclay.opentoday.callback.CallbackImportance;
import com.fazziclay.opentoday.databinding.ActivityMainBinding;
import com.fazziclay.opentoday.ui.UITickService;
import com.fazziclay.opentoday.ui.fragment.MainRootFragment;
import com.fazziclay.opentoday.ui.interfaces.ContainBackStack;
import com.fazziclay.opentoday.util.InlineUtil;
import com.fazziclay.opentoday.util.L;
import com.fazziclay.opentoday.util.NetworkUtil;
import com.fazziclay.opentoday.util.OnDebugLog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int CONTAINER_ID = R.id.content_root;

    private ActivityMainBinding binding;
    private App app;
    private UITickService uiTickService;
    private long lastExitClick = 0;
    private final OnDebugLog onDebugLog = new LocalOnDebugLog();

    // Current Date
    private Handler currentDateHandler;
    private Runnable currentDateRunnable;
    private GregorianCalendar currentDateCalendar;


    // Activity overrides
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long start = System.currentTimeMillis();
        try {
            getSupportActionBar().hide();
        } catch (Exception ignored) {}

        this.app = App.get(this);
        this.app.setAppInForeground(true);
        this.app.getTelemetry().send(new Telemetry.UiOpenLPacket());
        this.uiTickService = new UITickService(this);
        this.binding = ActivityMainBinding.inflate(getLayoutInflater());

        InlineUtil.viewVisible(binding.debugs, false, View.GONE);
        L.getCallbackStorage().addCallback(CallbackImportance.DEFAULT, onDebugLog);

        setContentView(binding.getRoot());

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(CONTAINER_ID, MainRootFragment.create(), "MainRootFragment")
                    .commit();
        }

        setupAppDebugNotify();
        setupUpdateAvailableNotify();
        setupCurrentDate();

        if (app.getSettingsManager().isQuickNoteNotification()) {
            QuickNoteReceiver.sendQuickNoteNotification(this);
        }
        if (!app.isFeatureFlag(FeatureFlag.DISABLE_AUTOMATIC_TICK)) {
            uiTickService.create();
            uiTickService.tick();
        }
        if (app.isFeatureFlag(FeatureFlag.SHOW_MAINACTIVITY_STARTUP_TIME)) {
            long startupTime = System.currentTimeMillis() - start;
            StringBuilder text = new StringBuilder("MainActivity startup time:\n").append(startupTime).append("ms");
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        Runnable exit = super::onBackPressed;
        Runnable def = () -> {
            if (System.currentTimeMillis() - lastExitClick > 1000) {
                Toast.makeText(this, R.string.exit_tab_2_count, Toast.LENGTH_SHORT).show();
                lastExitClick = System.currentTimeMillis();
            } else {
                exit.run();
            }
        };

        Fragment fragment = getMainRootFragment();
        if (fragment instanceof ContainBackStack) {
            ContainBackStack d = (ContainBackStack) fragment;
            if (!d.popBackStack()) {
                def.run();
            }
        } else {
            def.run();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (app != null) {
            app.setAppInForeground(false);
            app.getTelemetry().send(new Telemetry.UiClosedLPacket());
        }
        if (uiTickService != null) {
            uiTickService.destroy();
        }
        currentDateHandler.removeCallbacks(currentDateRunnable);
        L.getCallbackStorage().deleteCallback(onDebugLog);
    }

    private Fragment getMainRootFragment() {
        return getSupportFragmentManager().findFragmentById(CONTAINER_ID);
    }

    // Current Date
    private void setupCurrentDate() {
        currentDateCalendar = new GregorianCalendar();
        setCurrentDate();
        currentDateHandler = new Handler(getMainLooper());
        currentDateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isDestroyed()) return;
                setCurrentDate();
                long millis = System.currentTimeMillis() % 1000;
                currentDateHandler.postDelayed(this, 1000 - millis);
            }
        };
        currentDateHandler.post(currentDateRunnable);
        InlineUtil.viewClick(binding.currentDate, () -> new DatePickerDialog(this).show());
    }

    private void setCurrentDate() {
        currentDateCalendar.setTimeInMillis(System.currentTimeMillis());
        Date time = currentDateCalendar.getTime();

        // TODO: 11.10.2022 IDEA: Pattern to settings
        // Date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd EEEE", Locale.getDefault());
        binding.currentDateDate.setText(dateFormat.format(time));

        // Time
        dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        binding.currentDateTime.setText(dateFormat.format(time));
    }

    // Update checker
    private void setupUpdateAvailableNotify() {
        UpdateChecker.check(app, (available, url) -> runOnUiThread(() -> {
            InlineUtil.viewVisible(binding.updateAvailable, available, View.GONE);
            if (url != null) {
                InlineUtil.viewClick(binding.updateAvailable, () -> NetworkUtil.openBrowser(MainActivity.this, url));
            }
        }));
    }

    // App is DEBUG warning notify
    private void setupAppDebugNotify() {
        InlineUtil.viewVisible(binding.debugApp, App.DEBUG, View.GONE);
    }

    public void toggleLogsOverlay() {
        binding.debugs.setVisibility(binding.debugs.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    private class LocalOnDebugLog implements OnDebugLog {
        @Override
        public void run(String text) {
            if (isDestroyed() || binding == null || !App.DEBUG) {
                return;
            }
            runOnUiThread(() -> binding.debugs.setText(text));
        }
    }
}