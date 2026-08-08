package ru.cashguide.prod.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.android.material.button.MaterialButton;
import ru.cashguide.prod.R;
import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.data.local.db.CashbackCategory;
import ru.cashguide.prod.domain.model.CardWithCashback;
import ru.cashguide.prod.util.BankLogo;
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

    public void moveItem(int fromPosition, int toPosition) {
        Collections.swap(items, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    public List<Card> getCardsInOrder() {
        List<Card> result = new ArrayList<>(items.size());
        for (CardWithCashback item : items) {
            result.add(item.card);
        }
        return result;
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
        int logoRes = BankLogo.resFor(holder.tvAvatar.getContext(), bank);
        if (logoRes != 0) {
            holder.ivLogo.setImageResource(logoRes);
            holder.ivLogo.setVisibility(View.VISIBLE);
            holder.tvAvatar.setVisibility(View.GONE);
        } else {
            holder.ivLogo.setVisibility(View.GONE);
            holder.tvAvatar.setVisibility(View.VISIBLE);
            holder.tvAvatar.setText(bank.isEmpty() ? "?" : bank.substring(0, 1));
        }
        holder.tvBank.setText(bank);
        holder.tvName.setText(card.cardName);
        holder.tvBalance.setText(Formatting.formatMoney(card.balance, card.currency));
        holder.tvCashback.setText(Formatting.formatMoney(item.cashbackThisMonth, card.currency));
        String categories = formatCategories(item.categories);
        holder.tvCategories.setText(categories);
        holder.tvCategories.setVisibility(categories.isEmpty() ? View.GONE : View.VISIBLE);

        holder.itemView.setOnClickListener(v -> listener.onCardClick(card));
        holder.btnCashback.setOnClickListener(v -> listener.onCashbackSetup(card));
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(card));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(card));
    }

    private static String formatCategories(List<CashbackCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (CashbackCategory category : categories) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(category.category)
                    .append(' ')
                    .append(Formatting.decimal(category.percent))
                    .append('%');
        }
        return sb.toString();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView tvAvatar;
        final ImageView ivLogo;
        final TextView tvBank;
        final TextView tvName;
        final TextView tvBalance;
        final TextView tvCashback;
        final TextView tvCategories;
        final MaterialButton btnCashback;
        final MaterialButton btnEdit;
        final MaterialButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            ivLogo = itemView.findViewById(R.id.ivLogo);
            tvBank = itemView.findViewById(R.id.tvBank);
            tvName = itemView.findViewById(R.id.tvName);
            tvBalance = itemView.findViewById(R.id.tvBalance);
            tvCashback = itemView.findViewById(R.id.tvCashback);
            tvCategories = itemView.findViewById(R.id.tvCategories);
            btnCashback = itemView.findViewById(R.id.btnCashback);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
