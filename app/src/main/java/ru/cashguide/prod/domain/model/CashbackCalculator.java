package ru.cashguide.prod.domain.model;

import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.data.local.db.CashbackCategory;

/**
 * Вычисляет кэшбэк с учётом месячных лимитов.
 * Лимит задаёт сумму операций за месяц, на которую начисляется кэшбэк:
 * на траты сверх лимита кэшбэк не начисляется.
 */
public final class CashbackCalculator {

    private CashbackCalculator() {
    }

    /**
     * Возвращает кэшбэк, начисленный по категории за месяц: потраченная сумма
     * в пределах лимита, умноженная на процент. Лимит категории, если он задан,
     * имеет приоритет над общим лимитом карты.
     */
    public static double earnedInCategory(Card card, CashbackCategory setting) {
        if (setting == null || setting.percent <= 0.0 || setting.spentThisMonth <= 0.0) {
            return 0.0;
        }
        double limit = monthlyLimit(card, setting);
        double eligibleSpent = (limit > 0.0 && setting.spentThisMonth > limit)
                ? limit : setting.spentThisMonth;
        return eligibleSpent * setting.percent / 100.0;
    }

    /**
     * Возвращает для категории месячный лимит операций, по которым начисляется
     * кэшбэк: собственный лимит категории, либо общий лимит карты, либо 0,
     * если лимит не задан.
     */
    public static double monthlyLimit(Card card, CashbackCategory setting) {
        if (setting != null && setting.monthlyLimit != null
                && setting.monthlyLimit.doubleValue() > 0.0) {
            return setting.monthlyLimit.doubleValue();
        }
        if (card != null && card.monthlyCashbackLimit != null
                && card.monthlyCashbackLimit.doubleValue() > 0.0) {
            return card.monthlyCashbackLimit.doubleValue();
        }
        return 0.0;
    }
}