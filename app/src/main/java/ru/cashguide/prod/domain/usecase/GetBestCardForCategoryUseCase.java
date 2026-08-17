package ru.cashguide.prod.domain.usecase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.threeten.bp.YearMonth;
import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.data.local.db.CashbackCategory;
import ru.cashguide.prod.data.repository.CardRepository;
import ru.cashguide.prod.data.repository.CashbackRepository;
import ru.cashguide.prod.domain.model.CardCashbackResult;
import ru.cashguide.prod.domain.model.CashbackCalculator;
import ru.cashguide.prod.util.CashbackRounding;

/**
 * Ищет лучшую карту для покупки по категории и сумме.
 * Учитывает настройки кэшбэка за месяц и месячные лимиты выплаты:
 * кэшбэк по новой покупке ограничен остатком лимита категории
 * и общим остатком лимита карты.
 */
public class GetBestCardForCategoryUseCase {

    private final CardRepository cardRepository;
    private final CashbackRepository cashbackRepository;

    public GetBestCardForCategoryUseCase(CardRepository cardRepository, CashbackRepository cashbackRepository) {
        this.cardRepository = cardRepository;
        this.cashbackRepository = cashbackRepository;
    }

    public List<CardCashbackResult> execute(String category, double amount, YearMonth month) {
        List<Card> cards = cardRepository.getCards();
        List<CashbackCategory> settings =
                cashbackRepository.getAllForMonth(month.getMonthValue(), month.getYear());

        Map<Long, CashbackCategory> byCard = new HashMap<>();
        Map<Long, List<CashbackCategory>> byCardSettings = new HashMap<>();
        for (CashbackCategory setting : settings) {
            byCardSettings.computeIfAbsent(setting.cardId, k -> new ArrayList<>()).add(setting);
            if (setting.category.equals(category)) {
                byCard.put(setting.cardId, setting);
            }
        }

        List<CardCashbackResult> results = new ArrayList<>();
        for (Card card : cards) {
            CashbackCategory setting = byCard.get(card.id);
            List<CashbackCategory> cardSettings = byCardSettings.get(card.id);
            double percent = (setting != null) ? setting.percent : 0.0;
            double cashback = calculateCashback(card, setting, cardSettings, amount);
            results.add(new CardCashbackResult(card, percent, cashback));
        }

        Collections.sort(results, (a, b) -> Double.compare(b.cashbackAmount, a.cashbackAmount));
        return results;
    }

    private double calculateCashback(Card card, CashbackCategory setting,
                                     List<CashbackCategory> cardSettings, double amount) {
        if (setting == null || setting.percent <= 0.0) {
            return 0.0;
        }
        double base = CashbackRounding.roundedBase(amount);
        double direct = base * setting.percent / 100.0;

        double categoryRemaining = Double.POSITIVE_INFINITY;
        double categoryCap = CashbackCalculator.categoryLimit(setting);
        if (categoryCap > 0.0) {
            double alreadyEarned = CashbackCalculator.earnedInCategory(card, setting);
            categoryRemaining = Math.max(0.0, categoryCap - alreadyEarned);
        }

        double cardRemaining = Double.POSITIVE_INFINITY;
        double cardCap = CashbackCalculator.cardCap(card);
        if (cardCap > 0.0) {
            double alreadyEarned = CashbackCalculator.earnedOnCard(card, cardSettings);
            cardRemaining = Math.max(0.0, cardCap - alreadyEarned);
        }

        return Math.min(direct, Math.min(categoryRemaining, cardRemaining));
    }
}