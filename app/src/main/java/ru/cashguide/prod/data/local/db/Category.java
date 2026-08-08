package ru.cashguide.prod.data.local.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Категория покупок (системная или пользовательская).
 */
@Entity(
        tableName = "categories",
        indices = {@Index(value = "name", unique = true)})
public class Category {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name = "";

    public int sortOrder;

    public boolean isCustom;
}