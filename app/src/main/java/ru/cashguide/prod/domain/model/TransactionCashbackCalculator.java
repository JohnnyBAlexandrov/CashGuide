package ru.cashguide.prod.domain.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.data.local.db.CashbackCategory;
import ru.cashguide.prod.data.local.db.Transaction;

/**
 * Распределяет месячный кэшбэк по отдельным операциям с учётом лимита:
 * операции в рамках одной карты, категории и месяца упорядочиваются по дате,
 * и лимит «расходуется» по мере поступления операций. На операции сверх
 * лимита кэшбэк уже не начисляется.
 */
public final class TransactionCashbackCalculator {

    private TransactionCashbackCalculator() {
    }

    /** Возвращает id операции -> заработанный по ней кэшбэк (0, если нет). */
    public static Map<Long, Double> calculate(List<Transaction> transactions,
                                              List<CashbackCategory> settings,
                                              Map<Long, Card> cardsById) {
        return calculate(transactions, settings, cardsById, ZoneId.systemDefault());
    }

    /** Возвращает id операции -> заработанный по ней кэшбэк (0, если нет). */
    public static Map<Long, Double> calculate(List<Transaction> transactions,
                                              List<CashbackCategory> settings,
                                              Map<Long, Card> cardsById,
                                              ZoneId zone) {
        Map<Long, Double> result = new HashMap<>();
        if (transactions == null || settings == null || cardsById == null) {
            return result;
        }
        if (zone == null) {
            zone = ZoneId.systemDefault();
        }

        Map<Key, CashbackCategory> settingsByKey = new HashMap<>();
        for (CashbackCategory setting : settings) {
            if (setting != null) {
                settingsByKey.put(Key.ofCard(setting), setting);
            }
        }

        Map<Key, List<Transaction>> grouped = new HashMap<>();
        for (Transaction transaction : transactions) {
            if (transaction == null || !Transaction.TYPE_EXPENSE.equals(transaction.type)) {
                continue;
            }
            LocalDate date = Instant.ofEpochMilli(transaction.date).atZone(zone).toLocalDate();
            Key key = Key.ofTransaction(transaction, date);
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(transaction);
        }

        for (Map.Entry<Key, List<Transaction>> entry : grouped.entrySet()) {
            Key key = entry.getKey();
            CashbackCategory setting = settingsByKey.get(key);
            if (setting == null || setting.percent <= 0.0) {
                continue;
            }
            Card card = cardsById.get(key.cardId);
            double limit = CashbackCalculator.monthlyLimit(card, setting);

            List<Transaction> monthTxs = entry.getValue();
            monthTxs.sort((a, b) -> a.date == b.date
                    ? Long.compare(a.id, b.id)
                    : Long.compare(a.date, b.date));

            double remaining = limit;
            for (Transaction tx : monthTxs) {
                double eligible = (limit > 0.0 && tx.amount > remaining)
                        ? Math.max(0.0, remaining) : tx.amount;
                result.put(tx.id, eligible * setting.percent / 100.0);
                if (limit > 0.0) {
                    remaining -= eligible;
                }
            }
        }
        return result;
    }

    private static final class Key {

        final long cardId;
        final int month;
        final int year;
        final String category;

        Key(long cardId, int month, int year, String category) {
            this.cardId = cardId;
            this.month = month;
            this.year = year;
            this.category = category;
        }

        static Key ofCard(CashbackCategory setting) {
            return new Key(setting.cardId, setting.month, setting.year, setting.category);
        }

        static Key ofTransaction(Transaction transaction, LocalDate date) {
            return new Key(transaction.cardId, date.getMonthValue(), date.getYear(), transaction.category);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key)) {
                return false;
            }
            Key key = (Key) o;
            return cardId == key.cardId && month == key.month && year == key.year
                    && category.equals(key.category);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cardId, month, year, category);
        }
    }
}