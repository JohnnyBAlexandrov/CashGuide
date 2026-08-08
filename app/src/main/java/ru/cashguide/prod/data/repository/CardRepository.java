package ru.cashguide.prod.data.repository;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import ru.cashguide.prod.data.local.db.AppDatabase;
import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.data.local.db.CardDao;

public class CardRepository {

    private final CardDao cardDao;

    public CardRepository(AppDatabase database) {
        this.cardDao = database.cardDao();
    }

    public Flowable<List<Card>> observeCards() {
        return cardDao.observeAll();
    }

    public List<Card> getCards() {
        return cardDao.getAll();
    }

    public Single<Card> getCard(long id) {
        return cardDao.getById(id);
    }

    public Completable addCard(Card card) {
        return Completable.fromAction(() -> {
            card.sortOrder = cardDao.getNextSortOrder();
            cardDao.insert(card);
        });
    }

    public Completable updateCard(Card card) {
        return Completable.fromAction(() -> cardDao.update(card));
    }

    public Completable deleteCard(Card card) {
        return Completable.fromAction(() -> cardDao.delete(card));
    }

    public Completable reorderCards(List<Card> orderedCards) {
        return Completable.fromAction(() -> {
            for (int i = 0; i < orderedCards.size(); i++) {
                cardDao.updateSortOrder(orderedCards.get(i).id, i);
            }
        });
    }
}
