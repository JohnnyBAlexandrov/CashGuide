package ru.cashguide.prod.util;

/**
 * Правила расчёта базы кэшбэка по операции:
 * процент начисляется не от всей суммы, а от суммы, округлённой вниз до сотен
 * (например 299,99 -> 200; 99 -> 0).
 */
public final class CashbackRounding {

    private CashbackRounding() {
    }

    /** Возвращает базу кэшбэка: сумма, округлённая вниз до сотен. */
    public static double roundedBase(double amount) {
        if (amount <= 0.0) {
            return 0.0;
        }
        return Math.floor(amount / 100.0) * 100.0;
    }
}