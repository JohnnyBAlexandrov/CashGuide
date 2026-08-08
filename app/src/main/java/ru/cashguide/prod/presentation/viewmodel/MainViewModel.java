package ru.cashguide.prod.presentation.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.threeten.bp.YearMonth;
import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.data.local.db.CashbackCategory;
import ru.cashguide.prod.data.repository.CardRepository;
import ru.cashguide.prod.data.repository.CashbackRepository;
import ru.cashguide.prod.di.AppContainer;
import ru.cashguide.prod.domain.model.CardWithCashback;
import ru.cashguide.prod.presentation.util.SingleLiveEvent;
import ru.cashguide.prod.util.MonthPreference;

public class MainViewModel extends AndroidViewModel {

    private final CardRepository cardRepository;
    private final CashbackRepository cashbackRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<YearMonth> currentMonth = new MutableLiveData<>();
    private final MutableLiveData<List<CardWithCashback>> cards = new MutableLiveData<>();
    private final SingleLiveEvent<String> message = new SingleLiveEvent<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        cardRepository = AppContainer.getCardRepository(application);
        cashbackRepository = AppContainer.getCashbackRepository(application);
        currentMonth.setValue(MonthPreference.getCurrentMonth(application));
        subscribeMonth();
    }

    public LiveData<YearMonth> getCurrentMonth() {
        return currentMonth;
    }

    public LiveData<List<CardWithCashback>> getCards() {
        return cards;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public void prevMonth() {
        shiftMonth(-1);
    }

    public void nextMonth() {
        shiftMonth(1);
    }

    private void shiftMonth(int delta) {
        YearMonth newMonth = currentMonth.getValue().plusMonths(delta);
        MonthPreference.setCurrentMonth(getApplication(), newMonth);
        currentMonth.setValue(newMonth);
        subscribeMonth();
    }

    private void subscribeMonth() {
        disposables.clear();
        YearMonth month = currentMonth.getValue();
        Disposable disposable = Observable.combineLatest(
                cardRepository.observeCards().toObservable(),
                cashbackRepository.observeForMonth(month.getMonthValue(), month.getYear()).toObservable(),
                (cardList, settings) -> buildList(cardList, settings))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        list -> cards.setValue(list),
                        throwable -> message.setValue("Ошибка загрузки данных: " + throwable.getMessage()));
        disposables.add(disposable);
    }

    public void deleteCard(Card card) {
        Disposable disposable = cardRepository.deleteCard(card)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> message.setValue("Карта удалена"),
                        throwable -> message.setValue("Ошибка удаления карты"));
        disposables.add(disposable);
    }

    public void reorderCards(List<Card> cards) {
        Disposable disposable = cardRepository.reorderCards(cards)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                        },
                        throwable -> message.setValue("Ошибка сохранения порядка карт"));
        disposables.add(disposable);
    }

    private List<CardWithCashback> buildList(List<Card> cards, List<CashbackCategory> settings) {
        Map<Long, List<CashbackCategory>> byCard = new HashMap<>();
        for (CashbackCategory setting : settings) {
            byCard.computeIfAbsent(setting.cardId, k -> new ArrayList<>()).add(setting);
        }
        List<CardWithCashback> result = new ArrayList<>();
        for (Card card : cards) {
            double total = 0.0;
            List<CashbackCategory> cardSettings =
                    byCard.getOrDefault(card.id, Collections.emptyList());
            for (CashbackCategory setting : cardSettings) {
                total += setting.spentThisMonth * setting.percent / 100.0;
            }
            result.add(new CardWithCashback(card, total, cardSettings));
        }
        return result;
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
