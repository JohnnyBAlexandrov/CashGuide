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
import ru.cashguide.prod.util.CashbackRounding;

/**
 * Распределяет месячный кэшбэк по отдельным операциям с учётом лимитов выплаты.
 * Операции карты за месяц упорядочиваются по дате, и лимиты «расходуются»
 * по мере поступления: кэшбэк по каждой операции ограничен остатком лимита
 * своей категории и общим остатком лимита карты. На операции сверх лимита
 * кэшбэк уже не начисляется.
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

    /** Возвращает id -> заработанный по операции кэшбэк (0, если нет). */
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
                settingsByKey.put(Key.ofSetting(setting), setting);
            }
        }

        Map<CardMonth, List<Transaction>> groups = new HashMap<>();
        Map<Long, Card> cardById = new HashMap<>(cardsById);
        for (Transaction transaction : transactions) {
            if (transaction == null || !Transaction.TYPE_EXPENSE.equals(transaction.type)) {
                continue;
            }
            LocalDate date = Instant.ofEpochMilli(transaction.date).atZone(zone).toLocalDate();
            CardMonth group = new CardMonth(transaction.cardId, date.getMonthValue(), date.getYear());
            groups.computeIfAbsent(group, k -> new ArrayList<>()).add(transaction);
        }

        for (Map.Entry<CardMonth, List<Transaction>> groupEntry : groups.entrySet()) {
            CardMonth group = groupEntry.getKey();
            Card card = cardById.get(group.cardId);
            double cardCap = CashbackCalculator.cardCap(card);
            double cardUsed = 0.0;
            Map<Key, Double> categoryUsed = new HashMap<>();

            List<Transaction> monthTxs = groupEntry.getValue();
            monthTxs.sort((a, b) -> a.date == b.date
                    ? Long.compare(a.id, b.id)
                    : Long.compare(a.date, b.date));

            for (Transaction tx : monthTxs) {
                Key categoryKey = Key.of(group, tx.category);
                CashbackCategory setting = settingsByKey.get(categoryKey);
                if (setting == null || setting.percent <= 0.0) {
                    continue;
                }
                double cashBase = CashbackRounding.roundedBase(tx.amount);
                double txCash = cashBase * setting.percent / 100.0;

                double categoryRemaining = Double.POSITIVE_INFINITY;
                double categoryCap = CashbackCalculator.categoryLimit(setting);
                if (categoryCap > 0.0) {
                    double used = categoryUsed.getOrDefault(categoryKey, 0.0);
                    categoryRemaining = Math.max(0.0, categoryCap - used);
                }

                double cardRemaining = Double.POSITIVE_INFINITY;
                if (cardCap > 0.0) {
                    cardRemaining = Math.max(0.0, cardCap - cardUsed);
                }

                double applied = Math.min(txCash, Math.min(categoryRemaining, cardRemaining));
                if (applied <= 0.0) {
                    continue;
                }
                result.put(tx.id, applied);
                categoryUsed.merge(categoryKey, applied, Double::sum);
                cardUsed += applied;
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

        static Key ofSetting(CashbackCategory setting) {
            return new Key(setting.cardId, setting.month, setting.year, setting.category);
        }

        static Key of(CardMonth group, String category) {
            return new Key(group.cardId, group.month, group.year, category);
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

    private static final class CardMonth {

        final long cardId;
        final int month;
        final int year;

        CardMonth(long cardId, int month, int year) {
            this.cardId = cardId;
            this.month = month;
            this.year = year;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CardMonth)) {
                return false;
            }
            CardMonth other = (CardMonth) o;
            return cardId == other.cardId && month == other.month && year == other.year;
        }

        @Override
        public int hashCode() {
            return Objects.hash(cardId, month, year);
        }
    }
}