package ru.cashguide.prod.domain.model;

import java.util.List;

import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.data.local.db.CashbackCategory;

/**
 * Вычисляет кэшбэк с учётом месячных лимитов выплаты.
 * Лимит — это максимальная сумма кэшбэка, которую банк начислит за месяц:
 * лимит категории ограничивает кэшбэк по категории, общий лимит карты —
 * сумму кэшбэка по всем категориям карты в целом.
 */
public final class CashbackCalculator {

    private CashbackCalculator() {
    }

    /** Месячный лимит выплаты по категории, если задан (иначе 0). */
    public static double categoryLimit(CashbackCategory setting) {
        if (setting != null && setting.monthlyLimit != null
                && setting.monthlyLimit.doubleValue() > 0.0) {
            return setting.monthlyLimit.doubleValue();
        }
        return 0.0;
    }

    /** Месячный лимит выплаты по карте, если задан (иначе 0). */
    public static double cardCap(Card card) {
        if (card != null && card.monthlyCashbackLimit != null
                && card.monthlyCashbackLimit.doubleValue() > 0.0) {
            return card.monthlyCashbackLimit.doubleValue();
        }
        return 0.0;
    }

    /** Кэшбэк по категории за месяц, не превышающий лимит категории. */
    public static double earnedInCategory(Card card, CashbackCategory setting) {
        if (setting == null || setting.percent <= 0.0 || setting.spentThisMonth <= 0.0) {
            return 0.0;
        }
        double earned = setting.spentThisMonth * setting.percent / 100.0;
        double limit = categoryLimit(setting);
        return (limit > 0.0) ? Math.min(earned, limit) : earned;
    }

    /**
     * Итоговый кэшбэк карты за месяц: сумма по категориям (каждая внутри своего
     * лимита), но не выше общего лимита карты, если он задан.
     */
    public static double earnedOnCard(Card card, List<CashbackCategory> settings) {
        double total = 0.0;
        if (settings != null) {
            for (CashbackCategory setting : settings) {
                total += earnedInCategory(card, setting);
            }
        }
        double limit = cardCap(card);
        return (limit > 0.0) ? Math.min(total, limit) : total;
    }
}