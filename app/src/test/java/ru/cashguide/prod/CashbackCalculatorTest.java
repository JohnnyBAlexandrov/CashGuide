package ru.cashguide.prod;

import static org.junit.Assert.assertEquals;

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
    public void spentWithinLimitGivesFullCashback() {
        CashbackCategory setting = category(2.0, 500, 1000.0);
        assertEquals(10.00, CashbackCalculator.earnedInCategory(card(1000.0), setting), 0.001);
    }

    @Test
    public void spentAtLimitGivesFullCashback() {
        CashbackCategory setting = category(2.0, 1000, 1000.0);
        assertEquals(20.00, CashbackCalculator.earnedInCategory(card(1000.0), setting), 0.001);
    }

    @Test
    public void spentAboveLimitIsCappedByCategoryLimit() {
        CashbackCategory setting = category(10.0, 3000, 1000.0);
        assertEquals(100.00, CashbackCalculator.earnedInCategory(card(2000.0), setting), 0.001);
    }

    @Test
    public void noLimitGivesFullCashback() {
        CashbackCategory setting = category(3.0, 400, null);
        assertEquals(12.00, CashbackCalculator.earnedInCategory(card(null), setting), 0.001);
    }

    @Test
    public void cardLimitAppliesWhenCategoryHasNone() {
        CashbackCategory setting = category(5.0, 2000, null);
        assertEquals(50.00, CashbackCalculator.earnedInCategory(card(1000.0), setting), 0.001);
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
    public void monthlyLimitPrefersCategoryOverCard() {
        CashbackCategory setting = category(2.0, 2000, 1500.0);
        assertEquals(30.00, CashbackCalculator.earnedInCategory(card(500.0), setting), 0.001);
    }
}