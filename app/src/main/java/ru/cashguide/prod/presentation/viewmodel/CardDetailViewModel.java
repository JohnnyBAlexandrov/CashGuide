package ru.cashguide.prod.presentation.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.util.List;

import ru.cashguide.prod.data.local.db.Bank;
import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.data.repository.BankRepository;
import ru.cashguide.prod.data.repository.CardRepository;
import ru.cashguide.prod.di.AppContainer;
import ru.cashguide.prod.presentation.util.SingleLiveEvent;
import ru.cashguide.prod.util.Formatting;

public class CardDetailViewModel extends AndroidViewModel {

    private final CardRepository cardRepository;
    private final BankRepository bankRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<Card> card = new MutableLiveData<>();
    private final MutableLiveData<List<Bank>> banks = new MutableLiveData<>();
    private final SingleLiveEvent<String> message = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> closeScreen = new SingleLiveEvent<>();

    private long cardId = -1L;

    public CardDetailViewModel(@NonNull Application application) {
        super(application);
        cardRepository = AppContainer.getCardRepository(application);
        bankRepository = AppContainer.getBankRepository(application);
    }

    public LiveData<Card> getCard() {
        return card;
    }

    public LiveData<List<Bank>> getBanks() {
        return banks;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public LiveData<Boolean> getCloseScreen() {
        return closeScreen;
    }

    public void init(long id) {
        this.cardId = id;
        Disposable banksDisposable = bankRepository.observeAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        banks::setValue,
                        throwable -> {
                        });
        disposables.add(banksDisposable);
        if (id > 0) {
            Disposable disposable = cardRepository.getCard(id)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            loaded -> card.setValue(loaded),
                            throwable -> message.setValue("Ошибка загрузки карты"));
            disposables.add(disposable);
        } else {
            card.setValue(new Card());
        }
    }

    public void save(String bankName, String cardName, String balanceText, String currency, String limitText) {
        String bank = bankName == null ? "" : bankName.trim();
        String name = cardName == null ? "" : cardName.trim();
        if (bank.isEmpty()) {
            message.setValue("Укажите банк");
            return;
        }
        if (name.isEmpty()) {
            message.setValue("Укажите название карты");
            return;
        }
        double balance;
        try {
            balance = Formatting.parseNumber(balanceText);
        } catch (Exception e) {
            message.setValue("Некорректный баланс");
            return;
        }
        Double limit = null;
        String limitTrimmed = limitText == null ? "" : limitText.trim();
        if (!limitTrimmed.isEmpty()) {
            try {
                limit = Formatting.parseNumber(limitTrimmed);
                if (limit < 0) {
                    message.setValue("Лимит не может быть отрицательным");
                    return;
                }
            } catch (Exception e) {
                message.setValue("Некорректный лимит кэшбэка");
                return;
            }
        }

        Card value = card.getValue();
        if (value == null) {
            value = new Card();
        }
        value.bankName = bank;
        value.cardName = name;
        value.balance = balance;
        value.currency = currency;
        value.monthlyCashbackLimit = limit;

        Completable operation = (cardId > 0)
                ? cardRepository.updateCard(value)
                : cardRepository.addCard(value);
        Disposable disposable = operation
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> closeScreen.setValue(true),
                        throwable -> message.setValue("Ошибка сохранения карты"));
        disposables.add(disposable);
    }

    public void deleteCard() {
        Card value = card.getValue();
        if (cardId <= 0 || value == null) {
            return;
        }
        Disposable disposable = cardRepository.deleteCard(value)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> closeScreen.setValue(true),
                        throwable -> message.setValue("Ошибка удаления карты"));
        disposables.add(disposable);
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
