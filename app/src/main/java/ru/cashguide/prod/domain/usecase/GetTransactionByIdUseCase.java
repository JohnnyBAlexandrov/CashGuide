package ru.cashguide.prod.domain.usecase;

import io.reactivex.Single;
import ru.cashguide.prod.data.local.db.Transaction;
import ru.cashguide.prod.data.repository.TransactionRepository;

public class GetTransactionByIdUseCase {

    private final TransactionRepository transactionRepository;

    public GetTransactionByIdUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Single<Transaction> execute(long id) {
        return transactionRepository.getTransaction(id);
    }
}