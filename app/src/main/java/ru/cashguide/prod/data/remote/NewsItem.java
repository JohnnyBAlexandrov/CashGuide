package ru.cashguide.prod.data.remote;

/**
 * Новость, полученная с сервера.
 */
public final class NewsItem {

    private final String title;
    private final String date;
    private final String summary;
    private final String url;

    public NewsItem(String title, String date, String summary, String url) {
        this.title = title;
        this.date = date;
        this.summary = summary;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public String getDate() {
        return date;
    }

    public String getSummary() {
        return summary;
    }

    public String getUrl() {
        return url;
    }
}