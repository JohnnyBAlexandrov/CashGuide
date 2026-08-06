package ru.cashguide.prod.data.local.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "transactions",
        foreignKeys = @ForeignKey(
                entity = Card.class,
                parentColumns = "id",
                childColumns = "cardId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("cardId")})
public class Transaction {

    public static final String TYPE_EXPENSE = "EXPENSE";
    public static final String TYPE_INCOME = "INCOME";

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long cardId;

    public double amount;

    @NonNull
    public String type = TYPE_EXPENSE;

    @NonNull
    public String category = "";

    public long date;

    @NonNull
    public String note = "";
}
