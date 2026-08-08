package ru.cashguide.prod.presentation.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import ru.cashguide.prod.data.local.db.Bank;
import ru.cashguide.prod.data.repository.BankRepository;
import ru.cashguide.prod.di.AppContainer;
import ru.cashguide.prod.presentation.util.SingleLiveEvent;

public class BankViewModel extends AndroidViewModel {

    private final BankRepository bankRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<List<Bank>> banks = new MutableLiveData<>();
    private final SingleLiveEvent<String> message = new SingleLiveEvent<>();

    public BankViewModel(@NonNull Application application) {
        super(application);
        bankRepository = AppContainer.getBankRepository(application);
    }

    public LiveData<List<Bank>> getBanks() {
        return banks;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public void start() {
        Disposable disposable = bankRepository.observeAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        banks::setValue,
                        throwable -> message.setValue("Ошибка загрузки банков"));
        disposables.add(disposable);
    }

    public void add(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            message.setValue("Введите название банка");
            return;
        }
        Disposable disposable = bankRepository.addCustom(trimmed)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> message.setValue("Банк добавлен"),
                        throwable -> message.setValue("Не удалось добавить банк"));
        disposables.add(disposable);
    }

    public void rename(Bank bank, String newName) {
        if (bank == null) {
            return;
        }
        String trimmed = newName == null ? "" : newName.trim();
        if (trimmed.isEmpty()) {
            message.setValue("Введите название банка");
            return;
        }
        List<Bank> current = banks.getValue();
        if (current != null) {
            for (Bank existing : current) {
                if (existing.id != bank.id && existing.name.equalsIgnoreCase(trimmed)) {
                    message.setValue("Банк с таким названием уже существует");
                    return;
                }
            }
        }
        Disposable disposable = bankRepository.rename(bank.id, trimmed)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> message.setValue("Банк переименован"),
                        throwable -> message.setValue("Не удалось переименовать банк"));
        disposables.add(disposable);
    }

    public void delete(Bank bank) {
        if (bank == null) {
            return;
        }
        Disposable disposable = bankRepository.delete(bank.id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> message.setValue("Банк удалён"),
                        throwable -> message.setValue("Не удалось удалить банк"));
        disposables.add(disposable);
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}