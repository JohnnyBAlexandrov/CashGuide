package ru.cashguide.prod.data.repository;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import org.threeten.bp.YearMonth;
import ru.cashguide.prod.data.local.db.AppDatabase;
import ru.cashguide.prod.data.local.db.CashbackCategory;
import ru.cashguide.prod.data.local.db.CashbackDao;

public class CashbackRepository {

    private final AppDatabase database;
    private final CashbackDao cashbackDao;

    public CashbackRepository(AppDatabase database) {
        this.database = database;
        this.cashbackDao = database.cashbackDao();
    }

    public Flowable<List<CashbackCategory>> observeForCardAndMonth(long cardId, int month, int year) {
        return cashbackDao.observeForCardAndMonth(cardId, month, year);
    }

    public Flowable<List<CashbackCategory>> observeForMonth(int month, int year) {
        return cashbackDao.observeForMonth(month, year);
    }

    public List<CashbackCategory> getAllForMonth(int month, int year) {
        return cashbackDao.getAllForMonth(month, year);
    }

    public List<CashbackCategory> getForCardAndMonth(long cardId, int month, int year) {
        return cashbackDao.getForCardAndMonthSync(cardId, month, year);
    }

    public Completable saveSettings(List<CashbackCategory> items) {
        return Completable.fromAction(() -> {
            if (items == null || items.isEmpty()) {
                return;
            }
            CashbackCategory first = items.get(0);
            long cardId = first.cardId;
            int month = first.month;
            int year = first.year;
            database.runInTransaction(() -> {
                cashbackDao.deleteForCardAndMonth(cardId, month, year);
                cashbackDao.insertAll(items);
            });
        });
    }

    public Completable copyFromPreviousMonth(long cardId, YearMonth target) {
        return Completable.fromAction(() -> {
            YearMonth previous = target.minusMonths(1);
            List<CashbackCategory> previousSettings =
                    cashbackDao.getForCardAndMonthSync(cardId, previous.getMonthValue(), previous.getYear());
            if (previousSettings == null || previousSettings.isEmpty()) {
                return;
            }
            List<CashbackCategory> copies = new ArrayList<>();
            for (CashbackCategory source : previousSettings) {
                CashbackCategory copy = new CashbackCategory();
                copy.cardId = cardId;
                copy.category = source.category;
                copy.percent = source.percent;
                copy.spentThisMonth = 0;
                copy.month = target.getMonthValue();
                copy.year = target.getYear();
                copies.add(copy);
            }
            database.runInTransaction(() -> {
                cashbackDao.deleteForCardAndMonth(cardId, target.getMonthValue(), target.getYear());
                cashbackDao.insertAll(copies);
            });
        });
    }
}
