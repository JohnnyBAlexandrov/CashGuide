package ru.cashguide.prod;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import com.yandex.mobile.ads.banner.BannerAdView;

import ru.cashguide.prod.ads.AdsHelper;

public class MainActivity extends AppCompatActivity {

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
            NavController navController = navHostFragment.getNavController();
            DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
            NavigationView navView = findViewById(R.id.nav_view);
            NavigationUI.setupWithNavController(navView, navController);
            navView.setNavigationItemSelectedListener(item -> {
                NavOptions options = new NavOptions.Builder()
                        .setPopUpTo(navController.getGraph().getStartDestination(), true)
                        .setLaunchSingleTop(true)
                        .build();
                navController.navigate(item.getItemId(), null, options);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            });

            BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
            NavigationUI.setupWithNavController(bottomNav, navController);

            TextView navVersion = findViewById(R.id.navVersion);
            navVersion.setText(getString(R.string.app_version, BuildConfig.VERSION_NAME));

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

        View contentRoot = findViewById(R.id.content_root);
        View drawerContainer = findViewById(R.id.drawer_container);
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.drawer_layout), (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    int top = bars.top;
                    int bottom = bars.bottom;
                    contentRoot.setPadding(0, top, 0, bottom);
                    drawerContainer.setPadding(0, top, 0, bottom);
                    return WindowInsetsCompat.CONSUMED;
                });

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            boolean isNight = (getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            controller.setAppearanceLightStatusBars(!isNight);
        }
    }
}
