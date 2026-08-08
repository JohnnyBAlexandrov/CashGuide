package ru.cashguide.prod.di;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import ru.cashguide.prod.data.local.db.AppDatabase;
import ru.cashguide.prod.data.repository.BankRepository;
import ru.cashguide.prod.data.repository.CardRepository;
import ru.cashguide.prod.data.repository.CashbackRepository;
import ru.cashguide.prod.data.repository.CategoryRepository;
import ru.cashguide.prod.data.repository.TransactionRepository;

/**
 * Простой контейнер зависимостей (синглтон-репозитории вместо Dagger).
 */
public final class AppContainer {

    private static AppDatabase database;
    private static CardRepository cardRepository;
    private static CashbackRepository cashbackRepository;
    private static TransactionRepository transactionRepository;
    private static CategoryRepository categoryRepository;
    private static BankRepository bankRepository;

    private AppContainer() {
    }

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`sortOrder` INTEGER NOT NULL, " +
                    "`isCustom` INTEGER NOT NULL)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `banks` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`slug` TEXT NOT NULL, " +
                    "`isCustom` INTEGER NOT NULL, " +
                    "`sortOrder` INTEGER NOT NULL)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_banks_name` ON `banks` (`name`)");
            db.execSQL("ALTER TABLE `cashback_categories` ADD COLUMN `monthlyLimit` REAL");
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `cards` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static synchronized AppDatabase getDatabase(Context context) {
        if (database == null) {
            database = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "cashguide.db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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

    public static synchronized CategoryRepository getCategoryRepository(Context context) {
        if (categoryRepository == null) {
            categoryRepository = new CategoryRepository(getDatabase(context));
        }
        return categoryRepository;
    }

    public static synchronized BankRepository getBankRepository(Context context) {
        if (bankRepository == null) {
            bankRepository = new BankRepository(getDatabase(context));
        }
        return bankRepository;
    }
}
