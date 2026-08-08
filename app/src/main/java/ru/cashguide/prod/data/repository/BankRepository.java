package ru.cashguide.prod.data.repository;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import ru.cashguide.prod.data.BankCatalog;
import ru.cashguide.prod.data.local.db.AppDatabase;
import ru.cashguide.prod.data.local.db.Bank;
import ru.cashguide.prod.data.local.db.BankDao;

public class BankRepository {

    private final AppDatabase database;
    private final BankDao bankDao;

    public BankRepository(AppDatabase database) {
        this.database = database;
        this.bankDao = database.bankDao();
    }

    public Flowable<List<Bank>> observeAll() {
        return bankDao.observeAll();
    }

    public List<Bank> getAll() {
        return bankDao.getAll();
    }

    public Completable ensureSeeded() {
        return Completable.fromAction(() -> {
            if (bankDao.count() > 0) {
                return;
            }
            database.runInTransaction(() -> {
                List<BankCatalog.Bank> catalog = BankCatalog.getAll();
                for (int i = 0; i < catalog.size(); i++) {
                    BankCatalog.Bank source = catalog.get(i);
                    Bank bank = new Bank();
                    bank.name = source.name;
                    bank.slug = source.slug;
                    bank.isCustom = false;
                    bank.sortOrder = i;
                    bankDao.insert(bank);
                }
            });
        });
    }

    public Completable addCustom(String name) {
        return Completable.fromAction(() -> {
            String trimmed = name == null ? "" : name.trim();
            if (trimmed.isEmpty()) {
                return;
            }
            List<Bank> all = bankDao.getAll();
            Bank bank = new Bank();
            bank.name = trimmed;
            bank.slug = "";
            bank.isCustom = true;
            bank.sortOrder = all.isEmpty() ? 0 : all.size();
            database.runInTransaction(() -> bankDao.insert(bank));
        });
    }

    public Completable rename(long id, String name) {
        return Completable.fromAction(() -> {
            String trimmed = name == null ? "" : name.trim();
            if (trimmed.isEmpty()) {
                return;
            }
            bankDao.rename(id, trimmed);
        });
    }

    public Completable delete(long id) {
        return Completable.fromAction(() -> bankDao.delete(id));
    }
}