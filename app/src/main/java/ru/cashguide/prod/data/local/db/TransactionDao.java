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
public interface TransactionDao {

    @Insert
    long insert(Transaction transaction);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    Flowable<List<Transaction>> observeAll();

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    List<Transaction> getAll();

    @Query("SELECT * FROM transactions WHERE id = :id")
    Single<Transaction> getById(long id);

    @Query("SELECT * FROM transactions WHERE id = :id")
    Transaction getByIdSync(long id);
}
