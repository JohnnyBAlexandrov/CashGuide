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
import ru.cashguide.prod.data.local.db.Category;
import ru.cashguide.prod.data.repository.CategoryRepository;
import ru.cashguide.prod.di.AppContainer;
import ru.cashguide.prod.presentation.util.SingleLiveEvent;

public class CategoriesViewModel extends AndroidViewModel {

    private final CategoryRepository categoryRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final SingleLiveEvent<String> message = new SingleLiveEvent<>();

    public CategoriesViewModel(@NonNull Application application) {
        super(application);
        categoryRepository = AppContainer.getCategoryRepository(application);
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public void start() {
        Disposable disposable = categoryRepository.observeAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        categories::setValue,
                        throwable -> message.setValue("Ошибка загрузки категорий"));
        disposables.add(disposable);
    }

    public void add(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            message.setValue("Введите название категории");
            return;
        }
        List<Category> current = categories.getValue();
        if (current == null) {
            current = new java.util.ArrayList<>();
        }
        for (Category existing : current) {
            if (existing.name.equalsIgnoreCase(trimmed)) {
                message.setValue("Категория уже существует");
                return;
            }
        }
        Disposable disposable = categoryRepository.addCustom(trimmed)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> message.setValue("Категория добавлена"),
                        throwable -> message.setValue("Не удалось добавить категорию"));
        disposables.add(disposable);
    }

    public void delete(Category category) {
        if (category == null) {
            return;
        }
        Disposable disposable = categoryRepository.delete(category.id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> message.setValue("Категория удалена"),
                        throwable -> message.setValue("Не удалось удалить категорию"));
        disposables.add(disposable);
    }

    public void rename(Category category, String newName) {
        if (category == null) {
            return;
        }
        String trimmed = newName == null ? "" : newName.trim();
        if (trimmed.isEmpty()) {
            message.setValue("Введите название категории");
            return;
        }
        List<Category> current = categories.getValue();
        if (current != null) {
            for (Category existing : current) {
                if (existing.id != category.id
                        && existing.name.equalsIgnoreCase(trimmed)) {
                    message.setValue("Категория с таким названием уже существует");
                    return;
                }
            }
        }
        Disposable disposable = categoryRepository.rename(category.id, trimmed)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> message.setValue("Категория переименована"),
                        throwable -> message.setValue("Не удалось переименовать категорию"));
        disposables.add(disposable);
    }

    public void cleanJunk() {
        Disposable disposable = categoryRepository.cleanJunk()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        count -> message.setValue("Удалено мусорных категорий: " + count),
                        throwable -> message.setValue("Не удалось очистить мусор"));
        disposables.add(disposable);
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}