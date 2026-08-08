package ru.cashguide.prod.domain.model;

import ru.cashguide.prod.data.local.db.Transaction;

/**
 * Операция с вычисленным кэшбэком, который она принесла в этом месяце
 * с учётом месячного лимита категории (или карты).
 */
public class TransactionWithCashback {

    public final Transaction transaction;
    public final double earnedCashback;

    public TransactionWithCashback(Transaction transaction, double earnedCashback) {
        this.transaction = transaction;
        this.earnedCashback = earnedCashback;
    }
}