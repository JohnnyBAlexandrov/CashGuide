package ru.cashguide.prod.domain.model;

import ru.cashguide.prod.data.local.db.Card;

public class CardWithCashback {

    public final Card card;
    public final double cashbackThisMonth;

    public CardWithCashback(Card card, double cashbackThisMonth) {
        this.card = card;
        this.cashbackThisMonth = cashbackThisMonth;
    }
}