package ru.cashguide.prod.presentation.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.threeten.bp.YearMonth;
import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.data.local.db.CashbackCategory;
import ru.cashguide.prod.data.local.db.Category;
import ru.cashguide.prod.data.repository.CardRepository;
import ru.cashguide.prod.data.repository.CashbackRepository;
import ru.cashguide.prod.data.repository.CategoryRepository;
import ru.cashguide.prod.di.AppContainer;
import ru.cashguide.prod.presentation.util.SingleLiveEvent;
import ru.cashguide.prod.util.CategoryNormalizer;
import ru.cashguide.prod.util.MonthPreference;

public class CashbackSetupViewModel extends AndroidViewModel {

    private final CardRepository cardRepository;
    private final CashbackRepository cashbackRepository;
    private final CategoryRepository categoryRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<Card> card = new MutableLiveData<>();
    private final MutableLiveData<List<CashbackCategory>> settings = new MutableLiveData<>();
    private final MutableLiveData<List<String>> availableCategories = new MutableLiveData<>();
    private final MutableLiveData<List<String>> allCategoryNames = new MutableLiveData<>();
    private final MutableLiveData<YearMonth> month = new MutableLiveData<>();
    private final SingleLiveEvent<String> message = new SingleLiveEvent<>();
    private final SingleLiveEvent<List<String>> categoriesAddedToCatalog = new SingleLiveEvent<>();
    private final SingleLiveEvent<List<String>> unrecognizedCategories = new SingleLiveEvent<>();

    private final List<CashbackCategory> editorItems = new ArrayList<>();
    private final List<Category> allCategories = new ArrayList<>();

    private long cardId = -1L;

    public CashbackSetupViewModel(@NonNull Application application) {
        super(application);
        cardRepository = AppContainer.getCardRepository(application);
        cashbackRepository = AppContainer.getCashbackRepository(application);
        categoryRepository = AppContainer.getCategoryRepository(application);
    }

    public LiveData<Card> getCard() {
        return card;
    }

    public LiveData<List<CashbackCategory>> getSettings() {
        return settings;
    }

    public LiveData<YearMonth> getMonth() {
        return month;
    }

    public LiveData<List<String>> getAvailableCategories() {
        return availableCategories;
    }

    public LiveData<List<String>> getAllCategoryNames() {
        return allCategoryNames;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public LiveData<List<String>> getCategoriesAddedToCatalog() {
        return categoriesAddedToCatalog;
    }

    public LiveData<List<String>> getUnrecognizedCategories() {
        return unrecognizedCategories;
    }

    public void init(long id) {
        this.cardId = id;
        YearMonth currentMonth = MonthPreference.getCurrentMonth(getApplication());
        month.setValue(currentMonth);

        subscribeCategories();
        if (id > 0) {
            Disposable cardDisposable = cardRepository.getCard(id)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            loaded -> card.setValue(loaded),
                            throwable -> message.setValue("Ошибка загрузки карты"));
            disposables.add(cardDisposable);
            subscribeSettings();
        }
    }

    private void subscribeCategories() {
        Disposable disposable = categoryRepository.observeAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        categories -> {
                            allCategories.clear();
                            List<String> names = new ArrayList<>();
                            if (categories != null) {
                                for (Category category : categories) {
                                    allCategories.add(category);
                                    names.add(category.name);
                                }
                            }
                            allCategoryNames.setValue(names);
                            refreshAvailableCategories();
                        },
                        throwable -> message.setValue("Ошибка загрузки категорий"));
        disposables.add(disposable);
    }

    private void subscribeSettings() {
        YearMonth currentMonth = month.getValue();
        Disposable disposable = cashbackRepository
                .observeForCardAndMonth(cardId, currentMonth.getMonthValue(), currentMonth.getYear())
                .map(stored -> buildEditorList(stored, currentMonth))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        list -> {
                            editorItems.clear();
                            editorItems.addAll(list);
                            settings.setValue(new ArrayList<>(editorItems));
                            refreshAvailableCategories();
                        },
                        throwable -> message.setValue("Ошибка загрузки настроек"));
        disposables.add(disposable);
    }

    public void addCategory(String name, double percent, Double limit) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            message.setValue("Введите название категории");
            return;
        }
        for (CashbackCategory item : editorItems) {
            if (item.category.equalsIgnoreCase(trimmed)) {
                message.setValue("Категория уже добавлена");
                return;
            }
        }
        YearMonth currentMonth = month.getValue();
        if (currentMonth == null) {
            return;
        }
        CashbackCategory item = new CashbackCategory();
        item.cardId = cardId;
        item.category = trimmed;
        item.percent = percent;
        item.monthlyLimit = limit;
        item.spentThisMonth = 0.0;
        item.month = currentMonth.getMonthValue();
        item.year = currentMonth.getYear();
        editorItems.add(item);
        settings.setValue(new ArrayList<>(editorItems));
        refreshAvailableCategories();
        message.setValue("Категория добавлена");
    }

    public void removeCategory(CashbackCategory item) {
        if (item == null) {
            return;
        }
        editorItems.remove(item);
        settings.setValue(new ArrayList<>(editorItems));
        refreshAvailableCategories();
        message.setValue("Категория удалена");
    }

    /**
     * Применяет распознанные со скриншота проценты и лимиты:
     * обновляет существующие строки (в т.ч. «чинит» искажённые названия)
     * и добавляет новые для найденных категорий.
     */
    public int applyRecognized(Map<String, Double> percentByCategory,
                               Map<String, Double> limitByCategory,
                               List<String> unrecognizedLines) {
        Set<String> updated = new HashSet<>();
        List<String> addedToCatalog = new ArrayList<>();
        YearMonth currentMonth = month.getValue();
        for (Map.Entry<String, Double> entry : percentByCategory.entrySet()) {
            String canonical = entry.getKey();
            if (canonical == null || canonical.trim().isEmpty()) {
                continue;
            }
            Double percent = entry.getValue();
            CashbackCategory target = findMatchingItem(canonical);
            if (target != null) {
                if (!target.category.equalsIgnoreCase(canonical)) {
                    target.category = canonical;
                }
                target.percent = percent == null ? 0.0 : percent.doubleValue();
                Double limit = findValue(limitByCategory, canonical);
                if (limit != null) {
                    target.monthlyLimit = limit.doubleValue();
                }
                updated.add(canonical.toLowerCase(Locale.ROOT));
            } else if (currentMonth != null) {
                CashbackCategory item = new CashbackCategory();
                item.cardId = cardId;
                item.category = canonical;
                item.percent = percent == null ? 0.0 : percent.doubleValue();
                item.monthlyLimit = findValue(limitByCategory, canonical);
                item.spentThisMonth = 0.0;
                item.month = currentMonth.getMonthValue();
                item.year = currentMonth.getYear();
                editorItems.add(item);
                updated.add(canonical.toLowerCase(Locale.ROOT));
                addedToCatalog.add(canonical);
            }
        }
        if (!updated.isEmpty()) {
            settings.setValue(new ArrayList<>(editorItems));
            refreshAvailableCategories();
        }
        if (!addedToCatalog.isEmpty()) {
            Disposable disposable = categoryRepository.ensurePresent(new ArrayList<>(addedToCatalog))
                    .subscribeOn(Schedulers.io())
                    .subscribe(() -> {
                    }, throwable -> {
                    });
            disposables.add(disposable);
            categoriesAddedToCatalog.setValue(new ArrayList<>(addedToCatalog));
        }
        if (unrecognizedLines != null && !unrecognizedLines.isEmpty()) {
            unrecognizedCategories.setValue(new ArrayList<>(unrecognizedLines));
        }
        return updated.size();
    }

    /** Ищет существующую строку с точным или близким (OCR-искажение) названием. */
    private CashbackCategory findMatchingItem(String canonical) {
        CashbackCategory exact = null;
        CashbackCategory fuzzy = null;
        int bestDistance = Integer.MAX_VALUE;
        String normalizedCanonical = CategoryNormalizer.normalize(canonical);
        for (CashbackCategory item : editorItems) {
            if (item.category.equalsIgnoreCase(canonical)) {
                if (exact == null) {
                    exact = item;
                }
                continue;
            }
            if (normalizedCanonical.length() < 3) {
                continue;
            }
            int distance = CategoryNormalizer.levenshtein(normalizedCanonical,
                    CategoryNormalizer.normalize(item.category));
            int threshold = CategoryNormalizer.fuzzyThreshold(normalizedCanonical);
            if (distance <= threshold) {
                if (bestDistance > distance) {
                    bestDistance = distance;
                    fuzzy = item;
                }
            }
        }
        return exact != null ? exact : fuzzy;
    }

    private static Double findValue(Map<String, Double> map, String category) {
        if (map == null || category == null) {
            return null;
        }
        Double direct = map.get(category);
        if (direct != null) {
            return direct;
        }
        String normalized = category.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            if (entry.getKey() != null
                    && entry.getKey().toLowerCase(Locale.ROOT).contains(normalized)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void refreshAvailableCategories() {
        List<String> result = new ArrayList<>();
        for (Category category : allCategories) {
            boolean present = false;
            for (CashbackCategory item : editorItems) {
                if (item.category.equalsIgnoreCase(category.name)) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                result.add(category.name);
            }
        }
        availableCategories.setValue(result);
    }

    public void saveAll(List<CashbackCategory> editedItems) {
        YearMonth currentMonth = month.getValue();
        if (cardId <= 0 || currentMonth == null) {
            message.setValue("Нет данных для сохранения");
            return;
        }
        List<CashbackCategory> items = editedItems == null ? new ArrayList<>() : editedItems;
        List<String> names = new ArrayList<>();
        for (CashbackCategory item : items) {
            if (item.category != null) {
                names.add(item.category);
            }
        }
        Completable save = cashbackRepository.saveSettings(
                        cardId, currentMonth.getMonthValue(), currentMonth.getYear(), items)
                .andThen(categoryRepository.ensurePresent(names));
        Disposable disposable = save
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> message.setValue("Настройки сохранены"),
                        throwable -> message.setValue("Ошибка сохранения настроек"));
        disposables.add(disposable);
    }

    /** Обновляет изменённую пользователем строку кэшбэка. */
    public void updateCategory(CashbackCategory item) {
        if (item == null) {
            return;
        }
        String trimmed = item.category == null ? "" : item.category.trim();
        if (trimmed.isEmpty()) {
            message.setValue("Введите название категории");
            return;
        }
        for (CashbackCategory other : editorItems) {
            if (other != item && other.category.equalsIgnoreCase(trimmed)) {
                message.setValue("Категория уже добавлена");
                return;
            }
        }
        item.category = trimmed;
        settings.setValue(new ArrayList<>(editorItems));
        refreshAvailableCategories();
        message.setValue("Категория изменена");
    }

    /** Переименовывает существующую строку кэшбэка (исправляет OCR-искажение). */
    public void renameCategory(CashbackCategory item, String newName) {
        if (item == null) {
            return;
        }
        String trimmed = newName == null ? "" : newName.trim();
        if (trimmed.isEmpty()) {
            message.setValue("Введите название категории");
            return;
        }
        for (CashbackCategory other : editorItems) {
            if (other != item && other.category.equalsIgnoreCase(trimmed)) {
                message.setValue("Категория уже добавлена");
                return;
            }
        }
        item.category = trimmed;
        settings.setValue(new ArrayList<>(editorItems));
        refreshAvailableCategories();
        message.setValue("Категория переименована");
    }

    public void copyFromPreviousMonth() {
        YearMonth currentMonth = month.getValue();
        if (currentMonth == null) {
            return;
        }
        Disposable disposable = cashbackRepository.copyFromPreviousMonth(cardId, currentMonth)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> message.setValue("Настройки прошлого месяца скопированы"),
                        throwable -> message.setValue("Ошибка копирования настроек"));
        disposables.add(disposable);
    }

    private List<CashbackCategory> buildEditorList(List<CashbackCategory> stored, YearMonth currentMonth) {
        return stored == null ? new ArrayList<>() : new ArrayList<>(stored);
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
