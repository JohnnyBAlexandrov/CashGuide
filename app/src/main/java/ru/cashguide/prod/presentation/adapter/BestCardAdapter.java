package ru.cashguide.prod.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ru.cashguide.prod.R;
import ru.cashguide.prod.domain.model.CardCashbackResult;
import ru.cashguide.prod.util.Formatting;

public class BestCardAdapter extends RecyclerView.Adapter<BestCardAdapter.ViewHolder> {

    private final List<CardCashbackResult> items = new ArrayList<>();

    public void submitList(List<CardCashbackResult> newItems) {
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
                .inflate(R.layout.item_best_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CardCashbackResult item = items.get(position);
        holder.tvBank.setText(item.card.bankName);
        holder.tvCardName.setText(item.card.cardName);
        holder.tvPercent.setText(
                holder.tvPercent.getContext().getString(R.string.cashback_rate, Formatting.decimal(item.percent)));
        holder.tvCashback.setText(
                holder.tvCashback.getContext().getString(
                        R.string.you_get, Formatting.formatMoney(item.cashbackAmount, item.card.currency)));
        holder.tvRank.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView tvRank;
        final TextView tvBank;
        final TextView tvCardName;
        final TextView tvPercent;
        final TextView tvCashback;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvBank = itemView.findViewById(R.id.tvBank);
            tvCardName = itemView.findViewById(R.id.tvCardName);
            tvPercent = itemView.findViewById(R.id.tvPercent);
            tvCashback = itemView.findViewById(R.id.tvCashback);
        }
    }
}
