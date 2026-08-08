package ru.cashguide.prod.domain.model;

import java.util.ArrayList;
import java.util.List;

import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.data.local.db.CashbackCategory;

public class CardWithCashback {

    public final Card card;
    public final double cashbackThisMonth;
    public final List<CashbackCategory> categories;

    public CardWithCashback(Card card, double cashbackThisMonth, List<CashbackCategory> categories) {
        this.card = card;
        this.cashbackThisMonth = cashbackThisMonth;
        this.categories = categories == null ? new ArrayList<>() : new ArrayList<>(categories);
    }
}