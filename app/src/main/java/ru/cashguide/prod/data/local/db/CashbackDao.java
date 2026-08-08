package ru.cashguide.prod.data.local.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.reactivex.Flowable;

@Dao
public interface CashbackDao {

    @Insert
    long insert(CashbackCategory category);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CashbackCategory> categories);

    @Update
    void update(CashbackCategory category);

    @Query("DELETE FROM cashback_categories WHERE cardId = :cardId AND month = :month AND year = :year")
    void deleteForCardAndMonth(long cardId, int month, int year);

    @Query("DELETE FROM cashback_categories WHERE category IN (:names)")
    int deleteByCategory(List<String> names);

    @Query("SELECT * FROM cashback_categories WHERE cardId = :cardId AND month = :month AND year = :year ORDER BY category")
    Flowable<List<CashbackCategory>> observeForCardAndMonth(long cardId, int month, int year);

    @Query("SELECT * FROM cashback_categories WHERE month = :month AND year = :year")
    Flowable<List<CashbackCategory>> observeForMonth(int month, int year);

    @Query("SELECT * FROM cashback_categories WHERE month = :month AND year = :year")
    List<CashbackCategory> getAllForMonth(int month, int year);

    @Query("SELECT * FROM cashback_categories ORDER BY year, month")
    Flowable<List<CashbackCategory>> observeAll();

    @Query("SELECT * FROM cashback_categories")
    List<CashbackCategory> getAll();

    @Query("SELECT * FROM cashback_categories WHERE cardId = :cardId AND month = :month AND year = :year ORDER BY category")
    List<CashbackCategory> getForCardAndMonthSync(long cardId, int month, int year);
}
