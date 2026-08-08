package ru.cashguide.prod.data.local.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import io.reactivex.Flowable;

@Dao
public interface CategoryDao {

    @Insert
    long insert(Category category);

    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    Flowable<List<Category>> observeAll();

    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    List<Category> getAll();

    @Query("SELECT COUNT(*) FROM categories")
    int count();

    @Query("DELETE FROM categories WHERE id = :id")
    int delete(long id);

    @Query("UPDATE categories SET name = :name WHERE id = :id")
    int rename(long id, String name);
}