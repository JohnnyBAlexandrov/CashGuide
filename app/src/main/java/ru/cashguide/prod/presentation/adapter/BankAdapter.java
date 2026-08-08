package ru.cashguide.prod.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ru.cashguide.prod.R;
import ru.cashguide.prod.data.local.db.Bank;

public class BankAdapter extends RecyclerView.Adapter<BankAdapter.ViewHolder> {

    public interface Listener {
        void onEdit(Bank bank);

        void onDelete(Bank bank);
    }

    private final Listener listener;
    private final List<Bank> items = new ArrayList<>();

    public BankAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<Bank> newItems) {
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
                .inflate(R.layout.item_bank, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Bank bank = items.get(position);
        holder.tvBank.setText(bank.name);
        int logo = holder.itemView.getContext().getResources().getIdentifier(
                "logo_" + bank.slug,
                "drawable",
                holder.itemView.getContext().getPackageName());
        if (logo != 0) {
            holder.ivLogo.setImageResource(logo);
            holder.ivLogo.setVisibility(View.VISIBLE);
            holder.tvAvatar.setVisibility(View.GONE);
        } else {
            holder.ivLogo.setVisibility(View.GONE);
            holder.tvAvatar.setVisibility(View.VISIBLE);
            holder.tvAvatar.setText(bank.name.isEmpty() ? "?" : bank.name.substring(0, 1));
        }
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(bank);
            }
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(bank);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final ImageView ivLogo;
        final TextView tvAvatar;
        final TextView tvBank;
        final ImageButton btnEdit;
        final ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivLogo = itemView.findViewById(R.id.ivLogo);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvBank = itemView.findViewById(R.id.tvBank);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}