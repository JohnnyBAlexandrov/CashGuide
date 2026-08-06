package ru.cashguide.prod.domain.model;

import ru.cashguide.prod.data.local.db.Card;

public class CardCashbackResult {

    public final Card card;
    public final double percent;
    public final double cashbackAmount;

    public CardCashbackResult(Card card, double percent, double cashbackAmount) {
        this.card = card;
        this.percent = percent;
        this.cashbackAmount = cashbackAmount;
    }
}