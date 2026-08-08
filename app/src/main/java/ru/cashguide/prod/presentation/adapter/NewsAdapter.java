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
import ru.cashguide.prod.data.remote.NewsItem;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.ViewHolder> {

    public interface Listener {
        void onNewsClick(NewsItem item);
    }

    private final Listener listener;
    private final List<NewsItem> items = new ArrayList<>();

    public NewsAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<NewsItem> newItems) {
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
                .inflate(R.layout.item_news, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final NewsItem item = items.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvDate.setText(item.getDate());
        holder.tvSummary.setText(item.getSummary());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNewsClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView tvTitle;
        final TextView tvDate;
        final TextView tvSummary;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvSummary = itemView.findViewById(R.id.tvSummary);
        }
    }
}