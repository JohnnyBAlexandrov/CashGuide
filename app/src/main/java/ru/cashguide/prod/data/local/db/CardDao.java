package ru.cashguide.prod.data.local.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Single;

@Dao
public interface CardDao {

    @Insert
    long insert(Card card);

    @Update
    void update(Card card);

    @Delete
    void delete(Card card);

    @Query("SELECT * FROM cards ORDER BY bankName, cardName")
    Flowable<List<Card>> observeAll();

    @Query("SELECT * FROM cards ORDER BY bankName, cardName")
    List<Card> getAll();

    @Query("SELECT * FROM cards WHERE id = :id")
    Single<Card> getById(long id);

    @Query("SELECT * FROM cards WHERE id = :id")
    Card getByIdSync(long id);
}
