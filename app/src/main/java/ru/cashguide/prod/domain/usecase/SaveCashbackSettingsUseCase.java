package ru.cashguide.prod.domain.usecase;

import java.util.List;

import io.reactivex.Completable;
import ru.cashguide.prod.data.local.db.CashbackCategory;
import ru.cashguide.prod.data.repository.CashbackRepository;

public class SaveCashbackSettingsUseCase {

    private final CashbackRepository cashbackRepository;

    public SaveCashbackSettingsUseCase(CashbackRepository cashbackRepository) {
        this.cashbackRepository = cashbackRepository;
    }

    public Completable execute(List<CashbackCategory> items) {
        return cashbackRepository.saveSettings(items);
    }
}