package ru.cashguide.prod.util;

import android.content.Context;
import android.content.SharedPreferences;

import org.threeten.bp.YearMonth;

/** Хранение текущего выбранного месяца (и других настроек) в SharedPreferences. */
public final class MonthPreference {

    private static final String PREFERENCES = "cashguide_prefs";
    private static final String KEY_MONTH = "current_month";
    private static final String KEY_YEAR = "current_year";

    private MonthPreference() {
    }

    public static YearMonth getCurrentMonth(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        int month = prefs.getInt(KEY_MONTH, 0);
        int year = prefs.getInt(KEY_YEAR, 0);
        YearMonth now = YearMonth.now();
        if (month == 0 || year == 0) {
            return now;
        }
        return YearMonth.of(year, month);
    }

    public static void setCurrentMonth(Context context, YearMonth yearMonth) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_MONTH, yearMonth.getMonthValue())
                .putInt(KEY_YEAR, yearMonth.getYear())
                .apply();
    }
}
