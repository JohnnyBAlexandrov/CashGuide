package ru.cashguide.prod.presentation.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.threeten.bp.YearMonth;
import ru.cashguide.prod.di.AppContainer;
import ru.cashguide.prod.domain.model.CardCashbackResult;
import ru.cashguide.prod.domain.usecase.GetBestCardForCategoryUseCase;
import ru.cashguide.prod.presentation.util.SingleLiveEvent;
import ru.cashguide.prod.util.MonthPreference;

public class SearchViewModel extends AndroidViewModel {

    private final GetBestCardForCategoryUseCase getBestCardForCategoryUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<List<CardCashbackResult>> results = new MutableLiveData<>();
    private final SingleLiveEvent<String> message = new SingleLiveEvent<>();

    public SearchViewModel(@NonNull Application application) {
        super(application);
        getBestCardForCategoryUseCase = new GetBestCardForCategoryUseCase(
                AppContainer.getCardRepository(application),
                AppContainer.getCashbackRepository(application));
    }

    public LiveData<List<CardCashbackResult>> getResults() {
        return results;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public void search(String category, double amount) {
        YearMonth month = MonthPreference.getCurrentMonth(getApplication());
        Disposable disposable = Single.fromCallable(() -> getBestCardForCategoryUseCase.execute(category, amount, month))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        list -> results.setValue(list),
                        throwable -> message.setValue("Ошибка поиска: " + throwable.getMessage()));
        disposables.add(disposable);
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
