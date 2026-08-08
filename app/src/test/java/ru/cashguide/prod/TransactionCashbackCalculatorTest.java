package ru.cashguide.prod;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.data.local.db.CashbackCategory;
import ru.cashguide.prod.data.local.db.Transaction;
import ru.cashguide.prod.domain.model.TransactionCashbackCalculator;

public class TransactionCashbackCalculatorTest {

    private static final ZoneId UTC = ZoneId.of("UTC");

    private static long millis(int year, int month, int day) {
        return LocalDate.of(year, month, day).atStartOfDay(UTC).toInstant().toEpochMilli();
    }

    private static Transaction tx(long id, long cardId, String category, double amount, long date) {
        Transaction t = new Transaction();
        t.id = id;
        t.cardId = cardId;
        t.category = category;
        t.amount = amount;
        t.type = Transaction.TYPE_EXPENSE;
        t.date = date;
        return t;
    }

    private static CashbackCategory setting(long cardId, String category, double percent,
                                            Double limit, int month, int year) {
        CashbackCategory s = new CashbackCategory();
        s.cardId = cardId;
        s.category = category;
        s.percent = percent;
        s.monthlyLimit = limit;
        s.month = month;
        s.year = year;
        return s;
    }

    private static double earned(Map<Long, Double> map, long txId) {
        Double value = map.get(txId);
        return value == null ? 0.0 : value.doubleValue();
    }

    @Test
    public void noSettingsEarnsNothing() {
        long now = millis(2026, 8, 15);
        List<Transaction> txs = Arrays.asList(tx(1, 10, "Продукты", 500, now));
        Map<Long, Double> result = TransactionCashbackCalculator.calculate(
                txs, new ArrayList<>(), new HashMap<>(), UTC);
        assertEquals(0.00, earned(result, 1L), 0.001);
    }

    @Test
    public void earnsPercentWithinLimit() {
        long now = millis(2026, 8, 15);
        List<Transaction> txs = Arrays.asList(tx(1L, 10L, "Продукты", 500, now));
        List<CashbackCategory> settings = Arrays.asList(setting(10L, "Продукты", 10.0, 1000.0, 8, 2026));
        Map<Long, Card> cards = new HashMap<>();
        cards.put(10L, new Card());
        Map<Long, Double> result = TransactionCashbackCalculator.calculate(txs, settings, cards, UTC);
        assertEquals(50.00, earned(result, 1L), 0.001);
    }

    @Test
    public void limitIsConsumedInDateOrder() {
        long day1 = millis(2026, 8, 15);
        long day2 = millis(2026, 8, 16);
        List<Transaction> txs = Arrays.asList(
                tx(1L, 10L, "Продукты", 800, day1),
                tx(2L, 10L, "Продукты", 500, day2));
        List<CashbackCategory> settings = Arrays.asList(setting(10L, "Продукты", 10.0, 1000.0, 8, 2026));
        Map<Long, Double> result = TransactionCashbackCalculator.calculate(txs, settings, new HashMap<>(), UTC);
        assertEquals(80.00, earned(result, 1L), 0.001);
        assertEquals(20.00, earned(result, 2L), 0.001);
    }

    @Test
    public void beyondLimitEarnsNothing() {
        long day1 = millis(2026, 8, 15);
        long day2 = millis(2026, 8, 16);
        List<Transaction> txs = Arrays.asList(
                tx(1L, 10L, "Продукты", 1000, day1),
                tx(2L, 10L, "Продукты", 500, day2));
        List<CashbackCategory> settings = Arrays.asList(setting(10L, "Продукты", 10.0, 1000.0, 8, 2026));
        Map<Long, Double> result = TransactionCashbackCalculator.calculate(txs, settings, new HashMap<>(), UTC);
        assertEquals(100.00, earned(result, 1L), 0.001);
        assertEquals(0.00, earned(result, 2L), 0.001);
    }

    @Test
    public void cardLimitAppliesWhenCategoryHasNone() {
        long now = millis(2026, 8, 15);
        List<Transaction> txs = Arrays.asList(tx(1L, 10L, "Продукты", 300, now));
        List<CashbackCategory> settings = Arrays.asList(setting(10L, "Продукты", 50.0, null, 8, 2026));
        Card card = new Card();
        card.monthlyCashbackLimit = 200.0;
        Map<Long, Card> cards = new HashMap<>();
        cards.put(10L, card);
        Map<Long, Double> result = TransactionCashbackCalculator.calculate(txs, settings, cards, UTC);
        assertEquals(100.00, earned(result, 1L), 0.001);
    }

    @Test
    public void incomeGainsNothing() {
        long now = millis(2026, 8, 15);
        Transaction income = tx(1L, 10L, "Продукты", 500, now);
        income.type = Transaction.TYPE_INCOME;
        List<Transaction> txs = Arrays.asList(income);
        List<CashbackCategory> settings = Arrays.asList(setting(10L, "Продукты", 10.0, 1000.0, 8, 2026));
        Map<Long, Double> result = TransactionCashbackCalculator.calculate(txs, settings, new HashMap<>(), UTC);
        assertEquals(0.00, earned(result, 1L), 0.001);
    }

    @Test
    public void unrelatedCategoryEarnsNothing() {
        long now = millis(2026, 8, 15);
        List<Transaction> txs = Arrays.asList(tx(1L, 10L, "АЗС", 500, now));
        List<CashbackCategory> settings = Arrays.asList(setting(10L, "Продукты", 10.0, 1000.0, 8, 2026));
        Map<Long, Double> result = TransactionCashbackCalculator.calculate(txs, settings, new HashMap<>(), UTC);
        assertEquals(0.00, earned(result, 1L), 0.001);
    }

    @Test
    public void noLimitEarnsFullAmount() {
        long day1 = millis(2026, 8, 15);
        long day2 = millis(2026, 8, 16);
        List<Transaction> txs = Arrays.asList(
                tx(1L, 10L, "Продукты", 400, day1),
                tx(2L, 10L, "Продукты", 600, day2));
        List<CashbackCategory> settings = Arrays.asList(setting(10L, "Продукты", 3.0, null, 8, 2026));
        Map<Long, Double> result = TransactionCashbackCalculator.calculate(txs, settings, new HashMap<>(), UTC);
        assertEquals(12.00, earned(result, 1L), 0.001);
        assertEquals(18.00, earned(result, 2L), 0.001);
    }
}