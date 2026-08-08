package ru.cashguide.prod.data.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.cashguide.prod.BuildConfig;

/**
 * Загружает список новостей с сервера.
 * Формат ответа: JSON-массив [{"title": "...", "date": "01.01.2026", "summary": "...", "url": "..."}, ...].
 * При ошибке сети возвращает пустой список.
 */
public final class NewsFetcher {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    /**
     * Блокирующий вызов, должен выполняться в фоновом потоке.
     */
    public List<NewsItem> fetch() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(BuildConfig.NEWS_URL).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setUseCaches(false);
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                return Collections.emptyList();
            }
            String body = readBody(connection.getInputStream());
            return parse(body);
        } catch (IOException | JSONException e) {
            return Collections.emptyList();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static List<NewsItem> parse(String json) throws JSONException {
        JSONArray array = new JSONArray(json);
        List<NewsItem> result = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            result.add(new NewsItem(
                    obj.optString("title"),
                    obj.optString("date"),
                    obj.optString("summary"),
                    obj.optString("url")));
        }
        return result;
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