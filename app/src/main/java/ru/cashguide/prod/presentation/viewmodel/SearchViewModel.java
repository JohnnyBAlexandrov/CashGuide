package ru.cashguide.prod.presentation.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.threeten.bp.YearMonth;
import ru.cashguide.prod.data.repository.CategoryRepository;
import ru.cashguide.prod.di.AppContainer;
import ru.cashguide.prod.domain.model.CardCashbackResult;
import ru.cashguide.prod.domain.usecase.GetBestCardForCategoryUseCase;
import ru.cashguide.prod.presentation.util.SingleLiveEvent;
import ru.cashguide.prod.util.MonthPreference;

public class SearchViewModel extends AndroidViewModel {

    private final GetBestCardForCategoryUseCase getBestCardForCategoryUseCase;
    private final CategoryRepository categoryRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<List<CardCashbackResult>> results = new MutableLiveData<>();
    private final MutableLiveData<List<String>> categories = new MutableLiveData<>();
    private final SingleLiveEvent<String> message = new SingleLiveEvent<>();

    public SearchViewModel(@NonNull Application application) {
        super(application);
        getBestCardForCategoryUseCase = new GetBestCardForCategoryUseCase(
                AppContainer.getCardRepository(application),
                AppContainer.getCashbackRepository(application));
        categoryRepository = AppContainer.getCategoryRepository(application);
    }

    public LiveData<List<CardCashbackResult>> getResults() {
        return results;
    }

    public LiveData<List<String>> getCategories() {
        return categories;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public void loadCategories() {
        Disposable disposable = categoryRepository.observeAll()
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
        disposables.add(disposable);
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
