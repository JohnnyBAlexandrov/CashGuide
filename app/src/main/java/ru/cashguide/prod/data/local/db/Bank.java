package ru.cashguide.prod.data.local.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Банк из справочника (системный или пользовательский).
 */
@Entity(
        tableName = "banks",
        indices = {@Index(value = "name", unique = true)})
public class Bank {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name = "";

    @NonNull
    public String slug = "";

    public boolean isCustom;

    public int sortOrder;
}