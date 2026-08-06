package ru.cashguide.prod.presentation.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.threeten.bp.YearMonth;
import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.data.local.db.CashbackCategory;
import ru.cashguide.prod.data.repository.CardRepository;
import ru.cashguide.prod.data.repository.CashbackRepository;
import ru.cashguide.prod.di.AppContainer;
import ru.cashguide.prod.presentation.util.SingleLiveEvent;
import ru.cashguide.prod.util.CategoryCatalog;
import ru.cashguide.prod.util.MonthPreference;

public class CashbackSetupViewModel extends AndroidViewModel {

    private final CardRepository cardRepository;
    private final CashbackRepository cashbackRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<Card> card = new MutableLiveData<>();
    private final MutableLiveData<List<CashbackCategory>> settings = new MutableLiveData<>();
    private final MutableLiveData<YearMonth> month = new MutableLiveData<>();
    private final SingleLiveEvent<String> message = new SingleLiveEvent<>();

    private long cardId = -1L;

    public CashbackSetupViewModel(@NonNull Application application) {
        super(application);
        cardRepository = AppContainer.getCardRepository(application);
        cashbackRepository = AppContainer.getCashbackRepository(application);
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

    public LiveData<String> getMessage() {
        return message;
    }

    public void init(long id) {
        this.cardId = id;
        YearMonth currentMonth = MonthPreference.getCurrentMonth(getApplication());
        month.setValue(currentMonth);

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

    private void subscribeSettings() {
        YearMonth currentMonth = month.getValue();
        Disposable disposable = cashbackRepository
                .observeForCardAndMonth(cardId, currentMonth.getMonthValue(), currentMonth.getYear())
                .map(stored -> buildEditorList(stored, currentMonth))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        list -> settings.setValue(list),
                        throwable -> message.setValue("Ошибка загрузки настроек"));
        disposables.add(disposable);
    }

    public void saveAll(List<CashbackCategory> editedItems) {
        if (editedItems == null || editedItems.isEmpty()) {
            message.setValue("Нет данных для сохранения");
            return;
        }
        Disposable disposable = cashbackRepository.saveSettings(editedItems)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> message.setValue("Настройки сохранены"),
                        throwable -> message.setValue("Ошибка сохранения настроек"));
        disposables.add(disposable);
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
        Map<String, CashbackCategory> byCategory = new HashMap<>();
        for (CashbackCategory item : stored) {
            byCategory.put(item.category, item);
        }
        List<CashbackCategory> result = new ArrayList<>();
        for (String categoryName : CategoryCatalog.ALL) {
            CashbackCategory item = byCategory.get(categoryName);
            if (item == null) {
                item = new CashbackCategory();
                item.cardId = cardId;
                item.category = categoryName;
                item.percent = 0.0;
                item.spentThisMonth = 0.0;
                item.month = currentMonth.getMonthValue();
                item.year = currentMonth.getYear();
            }
            result.add(item);
        }
        return result;
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
