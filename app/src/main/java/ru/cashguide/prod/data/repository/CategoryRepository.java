package ru.cashguide.prod.data.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import ru.cashguide.prod.data.local.db.AppDatabase;
import ru.cashguide.prod.data.local.db.Category;
import ru.cashguide.prod.data.local.db.CategoryDao;
import ru.cashguide.prod.util.CategoryCatalog;
import ru.cashguide.prod.util.CategoryNormalizer;

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

    /**
     * Добавляет в каталог отсутствующие названия как пользовательские категории.
     * Проверка существования — регистронезависимая; всё выполняется одной транзакцией.
     */
    public Completable ensurePresent(List<String> names) {
        return Completable.fromAction(() -> {
            if (names == null || names.isEmpty()) {
                return;
            }
            List<Category> existing = categoryDao.getAll();
            List<String> lowerExisting = new ArrayList<>();
            for (Category category : existing) {
                if (category.name != null) {
                    lowerExisting.add(category.name.toLowerCase(Locale.ROOT));
                }
            }
            List<Category> additions = new ArrayList<>();
            int sortOrder = existing.size();
            for (String name : names) {
                String trimmed = name == null ? "" : name.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (lowerExisting.contains(trimmed.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                lowerExisting.add(trimmed.toLowerCase(Locale.ROOT));
                Category category = new Category();
                category.name = trimmed;
                category.sortOrder = sortOrder++;
                category.isCustom = true;
                additions.add(category);
            }
            if (!additions.isEmpty()) {
                database.runInTransaction(() -> categoryDao.insertAll(additions));
            }
        });
    }

    public Completable delete(long id) {
        return Completable.fromAction(() -> categoryDao.delete(id));
    }

    /**
     * Удаляет из каталога и из настроек кэшбэка всех карт
     * «мусорные» категории (названия с латиницей — искажения OCR).
     * Возвращает число удалённых названий.
     */
    public Single<Integer> cleanJunk() {
        return Single.fromCallable(() -> {
            List<Category> all = categoryDao.getAll();
            List<String> junk = new ArrayList<>();
            for (Category category : all) {
                if (CategoryNormalizer.isJunkName(category.name)) {
                    junk.add(category.name);
                }
            }
            if (junk.isEmpty()) {
                return 0;
            }
            database.runInTransaction(() -> {
                categoryDao.deleteByName(junk);
                database.cashbackDao().deleteByCategory(junk);
            });
            return junk.size();
        });
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