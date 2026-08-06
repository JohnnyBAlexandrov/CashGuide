package ru.cashguide.prod.domain.usecase;

import io.reactivex.Single;
import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.data.repository.CardRepository;

public class GetCardByIdUseCase {

    private final CardRepository cardRepository;

    public GetCardByIdUseCase(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public Single<Card> execute(long id) {
        return cardRepository.getCard(id);
    }
}