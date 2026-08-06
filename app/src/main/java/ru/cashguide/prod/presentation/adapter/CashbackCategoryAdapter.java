package ru.cashguide.prod.presentation.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import com.google.android.material.textfield.TextInputEditText;
import ru.cashguide.prod.R;
import ru.cashguide.prod.data.local.db.CashbackCategory;
import ru.cashguide.prod.util.Formatting;

public class CashbackCategoryAdapter extends RecyclerView.Adapter<CashbackCategoryAdapter.ViewHolder> {

    private final List<CashbackCategory> items = new ArrayList<>();

    public void submitList(List<CashbackCategory> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public List<CashbackCategory> getItems() {
        return new ArrayList<>(items);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cashback_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CashbackCategory item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView tvCategoryName;
        final TextInputEditText etPercent;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            etPercent = itemView.findViewById(R.id.etPercent);
        }

        void bind(CashbackCategory item) {
            etPercent.removeTextChangedListener(percentWatcher);
            percentWatcher.item = item;
            tvCategoryName.setText(item.category);
            etPercent.setText(Formatting.decimal(item.percent));
            etPercent.addTextChangedListener(percentWatcher);
        }

        private final ItemWatcher percentWatcher = new ItemWatcher() {
            @Override
            void apply(String text) {
                if (item != null) {
                    item.percent = safeParse(text);
                }
            }
        };

        private abstract static class ItemWatcher implements TextWatcher {
            CashbackCategory item;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                apply(s == null ? "" : s.toString());
            }

            abstract void apply(String text);
        }

        private static double safeParse(String text) {
            try {
                return Formatting.parseNumber(text);
            } catch (Exception e) {
                return 0.0;
            }
        }
    }
}
