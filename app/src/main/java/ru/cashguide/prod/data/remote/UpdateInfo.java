package ru.cashguide.prod.data.remote;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Информация о версии приложения, полученная с сервера.
 */
public final class UpdateInfo {

    private final int version;

    private UpdateInfo(int version) {
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

    public static UpdateInfo parse(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        return new UpdateInfo(obj.getInt("version"));
    }
}
