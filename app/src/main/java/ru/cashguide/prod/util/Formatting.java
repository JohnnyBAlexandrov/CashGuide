package ru.cashguide.prod.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Currency;
import java.util.Locale;

import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.YearMonth;

public final class Formatting {

    private static final DateTimeFormatter MONTH_YEAR =
            DateTimeFormatter.ofPattern("LLLL yyyy", new Locale("ru"));

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private Formatting() {
    }

    public static String formatMoney(double value, String currencyCode) {
        DecimalFormat df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));
        String symbol;
        try {
            symbol = Currency.getInstance(currencyCode).getSymbol();
        } catch (Exception ignored) {
            symbol = currencyCode == null ? "" : currencyCode;
        }
        return df.format(value) + " " + symbol;
    }

    public static String decimal(double value) {
        DecimalFormat df = new DecimalFormat("0.##", new DecimalFormatSymbols(Locale.US));
        return df.format(value);
    }

    public static String formatMonthYear(YearMonth yearMonth) {
        return yearMonth.format(MONTH_YEAR);
    }

    public static String formatDate(long millis) {
        org.threeten.bp.LocalDate date = org.threeten.bp.Instant.ofEpochMilli(millis)
                .atZone(org.threeten.bp.ZoneId.systemDefault())
                .toLocalDate();
        return date.format(DATE);
    }

    public static double parseNumber(String text) {
        if (text == null) {
            return 0.0;
        }
        return Double.parseDouble(text.replace(',', '.').trim());
    }
}
