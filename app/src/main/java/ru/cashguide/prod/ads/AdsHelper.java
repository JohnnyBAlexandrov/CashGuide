package ru.cashguide.prod.ads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yandex.mobile.ads.banner.BannerAdSize;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.YandexAds;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader;

import ru.cashguide.prod.BuildConfig;

public final class AdsHelper {

    private static final String BANNER_AD_UNIT_ID = BuildConfig.BANNER_AD_UNIT_ID;
    private static final String INTERSTITIAL_AD_UNIT_ID = BuildConfig.INTERSTITIAL_AD_UNIT_ID;
    private static final long MIN_INTERSTITIAL_INTERVAL_MS = 60_000L;

    private static InterstitialAd interstitialAd;
    private static boolean interstitialLoading;
    private static long lastInterstitialShownAt;

    private AdsHelper() {
    }

    public static void init(Context context) {
        YandexAds.initialize(context, null);
        YandexAds.enableLogging(BuildConfig.DEBUG);
    }

    public static void loadBanner(BannerAdView banner, Context context) {
        BannerAdSize size = BannerAdSize.sticky(context, screenWidthDp(context));
        banner.setAdSize(size);
        banner.loadAd(new AdRequest.Builder(BANNER_AD_UNIT_ID).build());
    }

    public static void loadInterstitial(Context context) {
        if (interstitialLoading) {
            return;
        }
        interstitialLoading = true;
        InterstitialAdLoader loader =
                new InterstitialAdLoader(context.getApplicationContext());
        loader.loadAd(new AdRequest.Builder(INTERSTITIAL_AD_UNIT_ID).build(),
                new InterstitialAdLoadListener() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialLoading = false;
                        interstitialAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull AdRequestError error) {
                        interstitialLoading = false;
                    }
                });
    }

    public static void showInterstitialIfReady(Activity activity) {
        if (interstitialAd == null) {
            loadInterstitial(activity);
            return;
        }
        if (!enoughTimeSinceLastShow()) {
            return;
        }
        InterstitialAd ad = interstitialAd;
        interstitialAd = null;
        ad.setAdEventListener(new InterstitialAdEventListener() {
            @Override
            public void onAdShown() {
            }

            @Override
            public void onAdFailedToShow(@NonNull AdError error) {
            }

            @Override
            public void onAdDismissed() {
                lastInterstitialShownAt = System.currentTimeMillis();
                loadInterstitial(activity);
            }

            @Override
            public void onAdClicked() {
            }

            @Override
            public void onAdImpression(@Nullable ImpressionData impressionData) {
            }
        });
        ad.show(activity);
    }

    private static boolean enoughTimeSinceLastShow() {
        return System.currentTimeMillis() - lastInterstitialShownAt
                >= MIN_INTERSTITIAL_INTERVAL_MS;
    }

    private static int screenWidthDp(Context context) {
        return Math.round(context.getResources().getDisplayMetrics().widthPixels
                / context.getResources().getDisplayMetrics().density);
    }
}
