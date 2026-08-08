package ru.cashguide.prod.data.local.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {Card.class, CashbackCategory.class, Transaction.class, Category.class, Bank.class},
        version = 4,
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract CardDao cardDao();

    public abstract CashbackDao cashbackDao();

    public abstract TransactionDao transactionDao();

    public abstract CategoryDao categoryDao();

    public abstract BankDao bankDao();
}
