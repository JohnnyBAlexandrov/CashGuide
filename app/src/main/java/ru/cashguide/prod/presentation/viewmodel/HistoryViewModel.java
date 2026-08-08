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
import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.data.local.db.CashbackCategory;
import ru.cashguide.prod.data.local.db.Transaction;
import ru.cashguide.prod.data.repository.CardRepository;
import ru.cashguide.prod.data.repository.CashbackRepository;
import ru.cashguide.prod.data.repository.CategoryRepository;
import ru.cashguide.prod.data.repository.TransactionRepository;
import ru.cashguide.prod.di.AppContainer;
import ru.cashguide.prod.domain.model.TransactionCashbackCalculator;
import ru.cashguide.prod.domain.model.TransactionWithCashback;
import ru.cashguide.prod.presentation.util.SingleLiveEvent;

public class HistoryViewModel extends AndroidViewModel {

    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final CashbackRepository cashbackRepository;
    private final CategoryRepository categoryRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<List<TransactionWithCashback>> transactions = new MutableLiveData<>();
    private final MutableLiveData<List<Card>> cards = new MutableLiveData<>();
    private final MutableLiveData<List<String>> categories = new MutableLiveData<>();
    private final SingleLiveEvent<String> message = new SingleLiveEvent<>();

    private List<Transaction> cachedAll = new ArrayList<>();
    private final Map<Long, Card> cachedCards = new HashMap<>();
    private List<CashbackCategory> cachedSettings = new ArrayList<>();
    private Long cardFilter = null;
    private String categoryFilter = null;
    private Long fromMillis = null;
    private Long toMillis = null;

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        transactionRepository = AppContainer.getTransactionRepository(application);
        cardRepository = AppContainer.getCardRepository(application);
        cashbackRepository = AppContainer.getCashbackRepository(application);
        categoryRepository = AppContainer.getCategoryRepository(application);
    }

    public LiveData<List<TransactionWithCashback>> getTransactions() {
        return transactions;
    }

    public LiveData<List<Card>> getCards() {
        return cards;
    }

    public LiveData<List<String>> getCategories() {
        return categories;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public void start() {
        Disposable txDisposable = transactionRepository.observeTransactions()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        list -> {
                            cachedAll = list;
                            recomputeCashback();
                        },
                        throwable -> message.setValue("Ошибка загрузки операций"));
        disposables.add(txDisposable);

        Disposable cardDisposable = cardRepository.observeCards()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        list -> {
                            cachedCards.clear();
                            if (list != null) {
                                for (Card card : list) {
                                    cachedCards.put(card.id, card);
                                }
                            }
                            cards.setValue(list);
                            recomputeCashback();
                        },
                        throwable -> {
                        });
        disposables.add(cardDisposable);

        Disposable settingsDisposable = cashbackRepository.observeAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        list -> {
                            cachedSettings = list;
                            recomputeCashback();
                        },
                        throwable -> {
                        });
        disposables.add(settingsDisposable);

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
    }

    public void setFilters(Long cardId, String category, Long from, Long to) {
        this.cardFilter = cardId;
        this.categoryFilter = category;
        this.fromMillis = from;
        this.toMillis = to;
        pushFiltered();
    }

    public void clearFilters() {
        setFilters(null, null, null, null);
    }

    private void recomputeCashback() {
        pushFiltered();
    }

    private void pushFiltered() {
        Map<Long, Double> cashbackByTransaction =
                TransactionCashbackCalculator.calculate(cachedAll, cachedSettings, cachedCards);
        List<TransactionWithCashback> result = new ArrayList<>();
        for (Transaction transaction : cachedAll) {
            if (cardFilter != null && transaction.cardId != cardFilter) {
                continue;
            }
            if (categoryFilter != null && !categoryFilter.isEmpty() && !categoryFilter.equals(transaction.category)) {
                continue;
            }
            if (fromMillis != null && transaction.date < fromMillis) {
                continue;
            }
            if (toMillis != null && transaction.date > toMillis) {
                continue;
            }
            Double earned = cashbackByTransaction.get(transaction.id);
            result.add(new TransactionWithCashback(
                    transaction, earned == null ? 0.0 : earned.doubleValue()));
        }
        transactions.setValue(result);
    }

    public void deleteTransaction(Transaction transaction) {
        Disposable disposable = transactionRepository.deleteTransaction(transaction)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> message.setValue("Операция удалена"),
                        throwable -> message.setValue("Ошибка удаления операции"));
        disposables.add(disposable);
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
