package ru.cashguide.prod.di;

import android.content.Context;

import androidx.room.Room;

import ru.cashguide.prod.data.local.db.AppDatabase;
import ru.cashguide.prod.data.repository.CardRepository;
import ru.cashguide.prod.data.repository.CashbackRepository;
import ru.cashguide.prod.data.repository.TransactionRepository;

/**
 * Простой контейнер зависимостей (синглтон-репозитории вместо Dagger).
 */
public final class AppContainer {

    private static AppDatabase database;
    private static CardRepository cardRepository;
    private static CashbackRepository cashbackRepository;
    private static TransactionRepository transactionRepository;

    private AppContainer() {
    }

    public static synchronized AppDatabase getDatabase(Context context) {
        if (database == null) {
            database = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "cashguide.db")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return database;
    }

    public static synchronized CardRepository getCardRepository(Context context) {
        if (cardRepository == null) {
            cardRepository = new CardRepository(getDatabase(context));
        }
        return cardRepository;
    }

    public static synchronized CashbackRepository getCashbackRepository(Context context) {
        if (cashbackRepository == null) {
            cashbackRepository = new CashbackRepository(getDatabase(context));
        }
        return cashbackRepository;
    }

    public static synchronized TransactionRepository getTransactionRepository(Context context) {
        if (transactionRepository == null) {
            transactionRepository = new TransactionRepository(getDatabase(context));
        }
        return transactionRepository;
    }
}
