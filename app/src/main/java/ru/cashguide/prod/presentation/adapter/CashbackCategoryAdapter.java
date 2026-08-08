package ru.cashguide.prod.presentation.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.android.material.textfield.TextInputEditText;
import ru.cashguide.prod.R;
import ru.cashguide.prod.data.local.db.CashbackCategory;
import ru.cashguide.prod.util.Formatting;

public class CashbackCategoryAdapter extends RecyclerView.Adapter<CashbackCategoryAdapter.ViewHolder> {

    public interface Listener {
        void onDelete(CashbackCategory category);

        void onEdit(CashbackCategory category);
    }

    private final Listener listener;
    private final List<CashbackCategory> items = new ArrayList<>();

    public CashbackCategoryAdapter(Listener listener) {
        this.listener = listener;
    }

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

    /**
     * Применяет распознанные со скриншота проценты и лимиты к строкам списка.
     * Возвращает количество категорий, для которых удалось распознать значения.
     */
    public int applyRecognized(Map<String, Double> percentByCategory,
                               Map<String, Double> limitByCategory) {
        int count = 0;
        for (CashbackCategory item : items) {
            Double percent = findValue(percentByCategory, item.category);
            if (percent != null) {
                item.percent = percent.doubleValue();
                count++;
            }
            Double limit = findValue(limitByCategory, item.category);
            if (limit != null) {
                item.monthlyLimit = limit.doubleValue();
                count++;
            }
        }
        if (count > 0) {
            notifyDataSetChanged();
        }
        return count;
    }

    private static Double findValue(Map<String, Double> map, String category) {
        if (map == null || category == null) {
            return null;
        }
        Double direct = map.get(category);
        if (direct != null) {
            return direct;
        }
        String normalized = category.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            if (entry.getKey() != null
                    && entry.getKey().toLowerCase(Locale.ROOT).contains(normalized)) {
                return entry.getValue();
            }
        }
        return null;
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
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(item);
            }
        });
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(item);
            }
        });
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView tvCategoryName;
        final TextInputEditText etPercent;
        final TextInputEditText etLimit;
        final ImageButton btnDelete;
        final ImageButton btnEdit;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            etPercent = itemView.findViewById(R.id.etPercent);
            etLimit = itemView.findViewById(R.id.etLimit);
            btnDelete = itemView.findViewById(R.id.btnDeleteCategory);
            btnEdit = itemView.findViewById(R.id.btnEditCategory);
        }

        void bind(CashbackCategory item) {
            etPercent.removeTextChangedListener(percentWatcher);
            etLimit.removeTextChangedListener(limitWatcher);
            percentWatcher.item = item;
            limitWatcher.item = item;
            tvCategoryName.setText(item.category);
            etPercent.setText(Formatting.decimal(item.percent));
            etLimit.setText(item.monthlyLimit == null
                    ? "" : Formatting.decimal(item.monthlyLimit.doubleValue()));
            etPercent.addTextChangedListener(percentWatcher);
            etLimit.addTextChangedListener(limitWatcher);
        }

        private final ItemWatcher percentWatcher = new ItemWatcher() {
            @Override
            void apply(String text) {
                if (item != null) {
                    item.percent = safeParse(text);
                }
            }
        };

        private final ItemWatcher limitWatcher = new ItemWatcher() {
            @Override
            void apply(String text) {
                if (item == null) {
                    return;
                }
                String value = text == null ? "" : text.trim();
                if (value.isEmpty()) {
                    item.monthlyLimit = null;
                } else {
                    item.monthlyLimit = safeParse(value);
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
