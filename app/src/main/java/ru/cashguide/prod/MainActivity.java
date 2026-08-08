package ru.cashguide.prod;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.navigation.fragment.NavHostFragment;

import com.yandex.mobile.ads.banner.BannerAdView;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.Single;
import ru.cashguide.prod.ads.AdsHelper;
import ru.cashguide.prod.data.remote.UpdateChecker;
import ru.cashguide.prod.data.remote.UpdateDownloader;
import ru.cashguide.prod.data.remote.UpdateInfo;

public class MainActivity extends AppCompatActivity {

    private static final AtomicBoolean UPDATE_CHECKED = new AtomicBoolean(false);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ================= РЕКЛАМА (ОТКЛЮЧЕНА) =================
        // Чтобы включить рекламу обратно — удали/раскомментируй 3 строки ниже,
        // и блок про showInterstitialIfReady далее. Не передавай null в initialize.
        // AdsHelper.init(this);
        //
        // BannerAdView bannerView = findViewById(R.id.bannerView);
        // AdsHelper.loadBanner(bannerView, this);
        // AdsHelper.loadInterstitial(this);
        // =========================================================

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navHostFragment.getNavController().addOnDestinationChangedListener(
                    (controller, destination, arguments) -> {
                        int id = destination.getId();
                        if (id == R.id.searchFragment || id == R.id.historyFragment) {
                            // РЕКЛАМА: раскомментируй при включении рекламы
                            // AdsHelper.showInterstitialIfReady(MainActivity.this);
                        }
                    });
        }

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        View content = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (bars.top > 0 || bars.bottom > 0) {
                v.setPadding(0, bars.top, 0, bars.bottom);
                return WindowInsetsCompat.CONSUMED;
            }
            return insets;
        });

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            boolean isNight = (getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            controller.setAppearanceLightStatusBars(!isNight);
        }

        checkForUpdates();
    }

    private void checkForUpdates() {
        if (!UPDATE_CHECKED.getAndSet(true)) {
            Single.fromCallable(new UpdateChecker()::check)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::showUpdateDialog, error -> {
                    });
        }
    }

    private void showUpdateDialog(UpdateInfo info) {
        if (info == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.update_title)
                    .setMessage(R.string.update_need_install_permission)
                    .setPositiveButton(R.string.update_goto_settings, (d, w) -> openInstallSettings())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.update_title)
                .setMessage(getString(R.string.update_message, info.getVersion()))
                .setPositiveButton(R.string.update_download, (d, w) -> UpdateDownloader.start(this))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openInstallSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName())));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }
}
