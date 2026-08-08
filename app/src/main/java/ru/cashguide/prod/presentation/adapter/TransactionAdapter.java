package ru.cashguide.prod.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.cashguide.prod.R;
import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.data.local.db.Transaction;
import ru.cashguide.prod.domain.model.TransactionWithCashback;
import ru.cashguide.prod.util.Formatting;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    public interface Listener {
        void onTransactionLongClick(TransactionWithCashback item);
    }

    private final Listener listener;
    private final List<TransactionWithCashback> items = new ArrayList<>();
    private Map<Long, Card> cards = new HashMap<>();

    public TransactionAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<TransactionWithCashback> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void setCards(List<Card> cardList) {
        Map<Long, Card> map = new HashMap<>();
        if (cardList != null) {
            for (Card card : cardList) {
                map.put(card.id, card);
            }
        }
        this.cards = map;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionWithCashback item = items.get(position);
        Transaction transaction = item.transaction;
        boolean isExpense = Transaction.TYPE_EXPENSE.equals(transaction.type);
        Card card = cards.get(transaction.cardId);
        String currency = (card != null && card.currency != null) ? card.currency : "RUB";

        String category = transaction.category;
        holder.tvAvatar.setText(category.isEmpty() ? "?" : category.substring(0, 1));
        holder.tvCategory.setText(category);

        String cardName = (card != null) ? card.cardName : null;
        holder.tvCardAndDate.setText(
                (cardName == null ? "" : cardName + " • ") + Formatting.formatDate(transaction.date));

        if (transaction.note != null && !transaction.note.isEmpty()) {
            holder.tvNote.setText(transaction.note);
            holder.tvNote.setVisibility(View.VISIBLE);
        } else {
            holder.tvNote.setVisibility(View.GONE);
        }

        holder.tvAmount.setText((isExpense ? "-" : "+") + Formatting.formatMoney(transaction.amount, currency));
        holder.tvAmount.setTextColor(ContextCompat.getColor(
                holder.tvAmount.getContext(),
                isExpense ? R.color.expense : R.color.income));

        boolean hasCashback = isExpense && item.earnedCashback > 0.0;
        if (hasCashback) {
            holder.tvCashback.setText(holder.tvCashback.getContext().getString(
                    R.string.history_cashback_earned,
                    Formatting.formatMoney(item.earnedCashback, currency)));
            holder.tvCashback.setVisibility(View.VISIBLE);
        } else {
            holder.tvCashback.setVisibility(View.GONE);
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onTransactionLongClick(item);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView tvAvatar;
        final TextView tvCategory;
        final TextView tvCardAndDate;
        final TextView tvNote;
        final TextView tvAmount;
        final TextView tvCashback;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvCardAndDate = itemView.findViewById(R.id.tvCardAndDate);
            tvNote = itemView.findViewById(R.id.tvNote);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvCashback = itemView.findViewById(R.id.tvCashback);
        }
    }
}
