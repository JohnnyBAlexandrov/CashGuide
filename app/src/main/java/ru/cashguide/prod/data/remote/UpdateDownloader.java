package ru.cashguide.prod.data.remote;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import ru.cashguide.prod.BuildConfig;
import ru.cashguide.prod.R;

/**
 * Загружает APK новой версии через системный DownloadManager.
 */
public final class UpdateDownloader {

    private static final String PREFS = "update_prefs";
    private static final String KEY_DOWNLOAD_ID = "download_id";

    private UpdateDownloader() {
    }

    public static long start(Context context) {
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(BuildConfig.APK_URL));
        request.setTitle(context.getString(R.string.update_download_notification));
        request.setMimeType("application/vnd.android.package-archive");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "cashguide.apk");
        long id = manager.enqueue(request);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_DOWNLOAD_ID, id)
                .apply();
        return id;
    }

    public static long getLastDownloadId(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_DOWNLOAD_ID, -1);
    }
}