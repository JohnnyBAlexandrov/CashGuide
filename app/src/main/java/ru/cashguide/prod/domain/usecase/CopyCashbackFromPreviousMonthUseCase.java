package ru.cashguide.prod.domain.usecase;

import io.reactivex.Completable;
import org.threeten.bp.YearMonth;
import ru.cashguide.prod.data.repository.CashbackRepository;

public class CopyCashbackFromPreviousMonthUseCase {

    private final CashbackRepository cashbackRepository;

    public CopyCashbackFromPreviousMonthUseCase(CashbackRepository cashbackRepository) {
        this.cashbackRepository = cashbackRepository;
    }

    public Completable execute(long cardId, YearMonth targetMonth) {
        return cashbackRepository.copyFromPreviousMonth(cardId, targetMonth);
    }
}