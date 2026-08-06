package ru.cashguide.prod.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import com.google.android.material.button.MaterialButton;
import ru.cashguide.prod.R;
import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.domain.model.CardWithCashback;
import ru.cashguide.prod.util.Formatting;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {

    public interface Listener {
        void onCardClick(Card card);

        void onCashbackSetup(Card card);

        void onEdit(Card card);

        void onDelete(Card card);
    }

    private final Listener listener;
    private final List<CardWithCashback> items = new ArrayList<>();

    public CardAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<CardWithCashback> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CardWithCashback item = items.get(position);
        Card card = item.card;

        String bank = card.bankName;
        holder.tvAvatar.setText(bank.isEmpty() ? "?" : bank.substring(0, 1));
        holder.tvBank.setText(bank);
        holder.tvName.setText(card.cardName);
        holder.tvBalance.setText(Formatting.formatMoney(card.balance, card.currency));
        holder.tvCashback.setText(
                holder.tvCashback.getContext().getString(
                        R.string.cashback_this_month,
                        Formatting.formatMoney(item.cashbackThisMonth, card.currency)));

        holder.itemView.setOnClickListener(v -> listener.onCardClick(card));
        holder.btnCashback.setOnClickListener(v -> listener.onCashbackSetup(card));
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(card));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(card));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView tvAvatar;
        final TextView tvBank;
        final TextView tvName;
        final TextView tvBalance;
        final TextView tvCashback;
        final MaterialButton btnCashback;
        final MaterialButton btnEdit;
        final MaterialButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvBank = itemView.findViewById(R.id.tvBank);
            tvName = itemView.findViewById(R.id.tvName);
            tvBalance = itemView.findViewById(R.id.tvBalance);
            tvCashback = itemView.findViewById(R.id.tvCashback);
            btnCashback = itemView.findViewById(R.id.btnCashback);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
