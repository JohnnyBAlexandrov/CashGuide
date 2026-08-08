package ru.cashguide.prod.data.local.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import io.reactivex.Flowable;

@Dao
public interface BankDao {

    @Insert
    long insert(Bank bank);

    @Query("SELECT * FROM banks ORDER BY sortOrder, name")
    Flowable<List<Bank>> observeAll();

    @Query("SELECT * FROM banks ORDER BY sortOrder, name")
    List<Bank> getAll();

    @Query("SELECT COUNT(*) FROM banks")
    int count();

    @Query("UPDATE banks SET name = :name WHERE id = :id")
    int rename(long id, String name);

    @Query("DELETE FROM banks WHERE id = :id")
    int delete(long id);
}