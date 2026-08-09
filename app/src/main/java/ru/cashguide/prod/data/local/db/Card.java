package ru.cashguide.prod.data.local.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cards")
public class Card {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String bankName = "";

    @NonNull
    public String cardName = "";

    public double balance;

    @NonNull
    public String currency = "RUB";

    /** Месячный лимит выплаты кэшбэка по карте, nullable = нет лимита. */
    public Double monthlyCashbackLimit;

    public int sortOrder;
}
