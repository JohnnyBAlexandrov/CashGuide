package ru.cashguide.prod;

import android.app.Application;

import com.jakewharton.threetenabp.AndroidThreeTen;

import io.reactivex.schedulers.Schedulers;
import ru.cashguide.prod.di.AppContainer;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);
        seedCategories();
        seedBanks();
    }

    private void seedCategories() {
        AppContainer.getCategoryRepository(this)
                .ensureSeeded()
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    private void seedBanks() {
        AppContainer.getBankRepository(this)
                .ensureSeeded()
                .subscribeOn(Schedulers.io())
                .subscribe();
    }
}
