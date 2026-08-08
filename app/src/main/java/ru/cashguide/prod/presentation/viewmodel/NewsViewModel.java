package ru.cashguide.prod.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import ru.cashguide.prod.data.remote.NewsFetcher;
import ru.cashguide.prod.data.remote.NewsItem;

public class NewsViewModel extends ViewModel {

    private final NewsFetcher fetcher = new NewsFetcher();
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<List<NewsItem>> news = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>();
    private final MutableLiveData<Boolean> error = new MutableLiveData<>();

    public LiveData<List<NewsItem>> getNews() {
        return news;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getError() {
        return error;
    }

    public void load() {
        loading.setValue(true);
        error.setValue(false);
        Disposable disposable = Single.fromCallable(fetcher::fetch)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        list -> {
                            loading.setValue(false);
                            news.setValue(list);
                        },
                        throwable -> {
                            loading.setValue(false);
                            error.setValue(true);
                        });
        disposables.add(disposable);
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}