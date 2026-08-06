package ru.cashguide.prod.domain.usecase;

import io.reactivex.Completable;
import ru.cashguide.prod.data.local.db.Transaction;
import ru.cashguide.prod.data.repository.TransactionRepository;

public class AddTransactionUseCase {

    private final TransactionRepository transactionRepository;

    public AddTransactionUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Completable execute(Transaction transaction) {
        return transactionRepository.addTransaction(transaction);
    }
}