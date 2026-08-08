package ru.cashguide.prod.presentation.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.data.local.db.Transaction;
import ru.cashguide.prod.data.repository.CardRepository;
import ru.cashguide.prod.data.repository.CategoryRepository;
import ru.cashguide.prod.data.repository.TransactionRepository;
import ru.cashguide.prod.di.AppContainer;
import ru.cashguide.prod.presentation.util.SingleLiveEvent;

public class TransactionEditViewModel extends AndroidViewModel {

    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final CategoryRepository categoryRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<Transaction> transaction = new MutableLiveData<>();
    private final MutableLiveData<Card> card = new MutableLiveData<>();
    private final MutableLiveData<java.util.List<Card>> cards = new MutableLiveData<>();
    private final MutableLiveData<List<String>> categories = new MutableLiveData<>();
    private final SingleLiveEvent<String> message = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> closeScreen = new SingleLiveEvent<>();

    private long transactionId = -1L;

    public TransactionEditViewModel(@NonNull Application application) {
        super(application);
        transactionRepository = AppContainer.getTransactionRepository(application);
        cardRepository = AppContainer.getCardRepository(application);
        categoryRepository = AppContainer.getCategoryRepository(application);
    }

    public LiveData<Transaction> getTransaction() {
        return transaction;
    }

    public LiveData<Card> getCard() {
        return card;
    }

    public LiveData<java.util.List<Card>> getCards() {
        return cards;
    }

    public LiveData<List<String>> getCategories() {
        return categories;
    }

    /** Результат распознавания скриншота, который нужно подставить в форму. */
    public static final class ScanResult {
        public double amount;
        public String category;
        public long dateMillis;
    }

    private final MutableLiveData<ScanResult> scanResult = new MutableLiveData<>();

    public LiveData<ScanResult> getScanResult() {
        return scanResult;
    }

    public void reportScan(double amount, String category, long dateMillis) {
        ScanResult result = new ScanResult();
        result.amount = amount;
        result.category = category;
        result.dateMillis = dateMillis;
        scanResult.setValue(result);
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public LiveData<Boolean> getCloseScreen() {
        return closeScreen;
    }

    public void init(long txId, long preselectedCardId) {
        this.transactionId = txId;

        Disposable cardsDisposable = cardRepository.observeCards()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        list -> cards.setValue(list),
                        throwable -> {
                        });
        disposables.add(cardsDisposable);

        Disposable catDisposable = categoryRepository.observeAll()
                .map(list -> {
                    List<String> names = new ArrayList<>();
                    for (ru.cashguide.prod.data.local.db.Category category : list) {
                        names.add(category.name);
                    }
                    return names;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        categories::setValue,
                        throwable -> {
                        });
        disposables.add(catDisposable);

        if (txId > 0) {
            Disposable txDisposable = transactionRepository.getTransaction(txId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            loaded -> transaction.setValue(loaded),
                            throwable -> message.setValue("Ошибка загрузки операции"));
            disposables.add(txDisposable);
        } else {
            Transaction fresh = new Transaction();
            transaction.setValue(fresh);
            if (preselectedCardId > 0) {
                cardRepository.getCard(preselectedCardId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                loaded -> card.setValue(loaded),
                                throwable -> {
                                });
            }
        }
    }

    public void save(Card selectedCard, boolean isExpense, double amount, String category, long dateMillis, String note) {
        if (selectedCard == null) {
            message.setValue("Выберите карту");
            return;
        }
        if (amount <= 0) {
            message.setValue("Сумма должна быть больше нуля");
            return;
        }
        if (category == null || category.trim().isEmpty()) {
            message.setValue("Укажите категорию");
            return;
        }

        Transaction value = transaction.getValue();
        if (value == null) {
            value = new Transaction();
        }
        value.cardId = selectedCard.id;
        value.type = isExpense ? Transaction.TYPE_EXPENSE : Transaction.TYPE_INCOME;
        value.amount = amount;
        value.category = category.trim();
        value.date = dateMillis;
        value.note = note == null ? "" : note.trim();

        Completable operation = (transactionId > 0)
                ? transactionRepository.updateTransaction(value)
                : transactionRepository.addTransaction(value);
        Disposable disposable = operation
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> closeScreen.setValue(true),
                        throwable -> message.setValue("Ошибка сохранения операции"));
        disposables.add(disposable);
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
