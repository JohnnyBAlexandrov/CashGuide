package ru.cashguide.prod.data.local.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "cashback_categories",
        foreignKeys = @ForeignKey(
                entity = Card.class,
                parentColumns = "id",
                childColumns = "cardId",
                onDelete = ForeignKey.CASCADE),
        indices = {
                @Index("cardId"),
                @Index(value = {"cardId", "month", "year", "category"}, unique = true)
        })
public class CashbackCategory {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long cardId;

    @NonNull
    public String category = "";

    public double percent;

    public double spentThisMonth;

    /** Месячный лимит начисляемого кэшбэка для этой категории (nullable = нет лимита). */
    public Double monthlyLimit;

    public int month;

    public int year;
}
