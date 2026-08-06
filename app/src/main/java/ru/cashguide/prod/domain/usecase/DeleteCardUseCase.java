package ru.cashguide.prod.domain.usecase;

import io.reactivex.Completable;
import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.data.repository.CardRepository;

public class DeleteCardUseCase {

    private final CardRepository cardRepository;

    public DeleteCardUseCase(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public Completable execute(Card card) {
        return cardRepository.deleteCard(card);
    }
}