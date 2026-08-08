package ru.cashguide.prod.data.repository;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import ru.cashguide.prod.data.local.db.AppDatabase;
import ru.cashguide.prod.data.local.db.Category;
import ru.cashguide.prod.data.local.db.CategoryDao;
import ru.cashguide.prod.util.CategoryCatalog;

public class CategoryRepository {

    private final AppDatabase database;
    private final CategoryDao categoryDao;

    public CategoryRepository(AppDatabase database) {
        this.database = database;
        this.categoryDao = database.categoryDao();
    }

    public Flowable<List<Category>> observeAll() {
        return categoryDao.observeAll();
    }

    public List<Category> getAll() {
        return categoryDao.getAll();
    }

    public Completable ensureSeeded() {
        return Completable.fromAction(() -> {
            if (categoryDao.count() > 0) {
                return;
            }
            database.runInTransaction(() -> {
                for (int i = 0; i < CategoryCatalog.ALL.size(); i++) {
                    Category category = new Category();
                    category.name = CategoryCatalog.ALL.get(i);
                    category.sortOrder = i;
                    category.isCustom = false;
                    categoryDao.insert(category);
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
            List<Category> all = categoryDao.getAll();
            int maxOrder = all.isEmpty() ? 0 : all.size();
            Category category = new Category();
            category.name = trimmed;
            category.sortOrder = maxOrder;
            category.isCustom = true;
            database.runInTransaction(() -> categoryDao.insert(category));
        });
    }

    public Completable delete(long id) {
        return Completable.fromAction(() -> categoryDao.delete(id));
    }

    public Completable rename(long id, String name) {
        return Completable.fromAction(() -> {
            String trimmed = name == null ? "" : name.trim();
            if (trimmed.isEmpty()) {
                return;
            }
            categoryDao.rename(id, trimmed);
        });
    }
}