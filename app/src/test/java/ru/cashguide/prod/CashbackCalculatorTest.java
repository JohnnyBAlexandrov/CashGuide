package ru.cashguide.prod;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.data.local.db.CashbackCategory;
import ru.cashguide.prod.domain.model.CashbackCalculator;

public class CashbackCalculatorTest {

    private static Card card(Double monthlyCashbackLimit) {
        Card card = new Card();
        card.monthlyCashbackLimit = monthlyCashbackLimit;
        return card;
    }

    private static CashbackCategory category(double percent, double spent, Double monthlyLimit) {
        CashbackCategory setting = new CashbackCategory();
        setting.percent = percent;
        setting.spentThisMonth = spent;
        setting.monthlyLimit = monthlyLimit;
        return setting;
    }

    @Test
    public void spentEarnsPercentWithoutLimits() {
        CashbackCategory setting = category(2.0, 500, null);
        assertEquals(10.00, CashbackCalculator.earnedInCategory(card(null), setting), 0.001);
    }

    @Test
    public void categoryPayoutIsCappedByItsLimit() {
        CashbackCategory setting = category(30.0, 5000, 1000.0);
        assertEquals(1000.00, CashbackCalculator.earnedInCategory(card(5000.0), setting), 0.001);
    }

    @Test
    public void categoryUnderLimitKeepsFullCashback() {
        CashbackCategory setting = category(10.0, 3000, 1000.0);
        assertEquals(300.00, CashbackCalculator.earnedInCategory(card(null), setting), 0.001);
    }

    @Test
    public void zeroPercentGivesNothing() {
        CashbackCategory setting = category(0.0, 1000, null);
        assertEquals(0.00, CashbackCalculator.earnedInCategory(card(null), setting), 0.001);
    }

    @Test
    public void nullSettingsGivesNothing() {
        assertEquals(0.00, CashbackCalculator.earnedInCategory(card(null), null), 0.001);
    }

    @Test
    public void cardCapCapsCategorySum() {
        CashbackCategory food = category(2.0, 3000, null);
        CashbackCategory fuel = category(3.0, 2000, null);
        assertEquals(120.00, CashbackCalculator.earnedOnCard(card(null),
                Arrays.asList(food, fuel)), 0.001);
        assertEquals(100.00, CashbackCalculator.earnedOnCard(card(100.0),
                Arrays.asList(food, fuel)), 0.001);
    }

    @Test
    public void categoryCapAppliesBeforeCardCap() {
        CashbackCategory food = category(2.0, 3000, 40.0);
        CashbackCategory fuel = category(3.0, 2000, 30.0);
        // 40 + 30 = 70 по категориям, лимит карты 50
        assertEquals(50.00, CashbackCalculator.earnedOnCard(card(50.0),
                Arrays.asList(food, fuel)), 0.001);
    }

    @Test
    public void emptyCardSettingsGivesZero() {
        assertEquals(0.00, CashbackCalculator.earnedOnCard(card(100.0), null), 0.001);
    }
}