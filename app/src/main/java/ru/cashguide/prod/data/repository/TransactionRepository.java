package ru.cashguide.prod.data.repository;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import ru.cashguide.prod.data.local.db.AppDatabase;
import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.data.local.db.CardDao;
import ru.cashguide.prod.data.local.db.CashbackCategory;
import ru.cashguide.prod.data.local.db.CashbackDao;
import ru.cashguide.prod.data.local.db.Transaction;
import ru.cashguide.prod.data.local.db.TransactionDao;

public class TransactionRepository {

    private final AppDatabase database;
    private final TransactionDao transactionDao;
    private final CardDao cardDao;
    private final CashbackDao cashbackDao;

    public TransactionRepository(AppDatabase database) {
        this.database = database;
        this.transactionDao = database.transactionDao();
        this.cardDao = database.cardDao();
        this.cashbackDao = database.cashbackDao();
    }

    public Flowable<List<Transaction>> observeTransactions() {
        return transactionDao.observeAll();
    }

    public Single<Transaction> getTransaction(long id) {
        return transactionDao.getById(id);
    }

    public Completable addTransaction(Transaction transaction) {
        return Completable.fromAction(() -> database.runInTransaction(() -> {
            applyEffects(transaction);
            transactionDao.insert(transaction);
        }));
    }

    public Completable updateTransaction(Transaction transaction) {
        return Completable.fromAction(() -> database.runInTransaction(() -> {
            Transaction old = transactionDao.getByIdSync(transaction.id);
            if (old != null) {
                revertEffects(old);
            }
            applyEffects(transaction);
            transactionDao.update(transaction);
        }));
    }

    public Completable deleteTransaction(Transaction transaction) {
        return Completable.fromAction(() -> database.runInTransaction(() -> {
            revertEffects(transaction);
            transactionDao.delete(transaction);
        }));
    }

    private void applyEffects(Transaction transaction) {
        Card card = cardDao.getByIdSync(transaction.cardId);
        if (card == null) {
            return;
        }
        double sign = transaction.type.equals(Transaction.TYPE_EXPENSE) ? -1.0 : 1.0;
        card.balance += sign * transaction.amount;
        cardDao.update(card);

        if (transaction.type.equals(Transaction.TYPE_EXPENSE)) {
            CashbackCategory category = findCategory(transaction);
            if (category != null) {
                category.spentThisMonth += transaction.amount;
                cashbackDao.update(category);
            }
        }
    }

    private void revertEffects(Transaction transaction) {
        Card card = cardDao.getByIdSync(transaction.cardId);
        if (card == null) {
            return;
        }
        double sign = transaction.type.equals(Transaction.TYPE_EXPENSE) ? -1.0 : 1.0;
        card.balance -= sign * transaction.amount;
        cardDao.update(card);

        if (transaction.type.equals(Transaction.TYPE_EXPENSE)) {
            CashbackCategory category = findCategory(transaction);
            if (category != null) {
                category.spentThisMonth = Math.max(0.0, category.spentThisMonth - transaction.amount);
                cashbackDao.update(category);
            }
        }
    }

    private CashbackCategory findCategory(Transaction transaction) {
        LocalDate date = Instant.ofEpochMilli(transaction.date)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        List<CashbackCategory> categories =
                cashbackDao.getForCardAndMonthSync(transaction.cardId, date.getMonthValue(), date.getYear());
        for (CashbackCategory category : categories) {
            if (category.category.equals(transaction.category)) {
                return category;
            }
        }
        return null;
    }
}
