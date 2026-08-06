package ru.cashguide.prod.data.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONException;

import ru.cashguide.prod.BuildConfig;

/**
 * Проверяет текущую версию приложения на сервере.
 * Возвращает {@link UpdateInfo}, если на сервере новая версия, иначе null (в том числе при ошибке сети).
 */
public final class UpdateChecker {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    /**
     * Блокирующий вызов, должен выполняться в фоновом потоке.
     */
    public UpdateInfo check() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(BuildConfig.UPDATE_URL).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setUseCaches(false);
            try {
                int code = connection.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) {
                    return null;
                }
                String body = readBody(connection.getInputStream());
                UpdateInfo info = UpdateInfo.parse(body);
                return info.getVersion() > BuildConfig.VERSION_CODE ? info : null;
            } finally {
                connection.disconnect();
            }
        } catch (IOException | JSONException e) {
            return null;
        }
    }

    private static String readBody(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}