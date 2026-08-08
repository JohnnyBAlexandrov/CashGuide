package ru.cashguide.prod.domain.usecase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.threeten.bp.YearMonth;
import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.data.local.db.CashbackCategory;
import ru.cashguide.prod.data.repository.CardRepository;
import ru.cashguide.prod.data.repository.CashbackRepository;
import ru.cashguide.prod.domain.model.CardWithCashback;

/**
 * Возвращает список карт с итоговым кэшбэком, «заработанным» за месяц
 * (сумма потраченного в категориях умножается на процент кэшбэка).
 */
public class GetCardsWithCashbackUseCase {

    private final CardRepository cardRepository;
    private final CashbackRepository cashbackRepository;

    public GetCardsWithCashbackUseCase(CardRepository cardRepository, CashbackRepository cashbackRepository) {
        this.cardRepository = cardRepository;
        this.cashbackRepository = cashbackRepository;
    }

    public List<CardWithCashback> execute(YearMonth month) {
        List<Card> cards = cardRepository.getCards();
        List<CashbackCategory> settings =
                cashbackRepository.getAllForMonth(month.getMonthValue(), month.getYear());

        Map<Long, List<CashbackCategory>> byCard = new HashMap<>();
        for (CashbackCategory setting : settings) {
            byCard.computeIfAbsent(setting.cardId, k -> new ArrayList<>()).add(setting);
        }

        List<CardWithCashback> result = new ArrayList<>();
        for (Card card : cards) {
            double total = 0.0;
            List<CashbackCategory> cardSettings =
                    byCard.getOrDefault(card.id, new ArrayList<>());
            for (CashbackCategory setting : cardSettings) {
                total += setting.spentThisMonth * setting.percent / 100.0;
            }
            result.add(new CardWithCashback(card, total, cardSettings));
        }
        return result;
    }
}