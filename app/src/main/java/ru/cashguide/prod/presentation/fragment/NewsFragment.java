package ru.cashguide.prod.presentation.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import ru.cashguide.prod.R;
import ru.cashguide.prod.data.remote.NewsItem;
import ru.cashguide.prod.presentation.adapter.NewsAdapter;
import ru.cashguide.prod.presentation.viewmodel.NewsViewModel;
import ru.cashguide.prod.util.DrawerUi;

public class NewsFragment extends Fragment {

    private NewsViewModel viewModel;
    private NewsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_news, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(NewsViewModel.class);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        DrawerUi.setupWithNavController(toolbar, this);

        RecyclerView recyclerView = view.findViewById(R.id.rvNews);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NewsAdapter(this::openNews);
        recyclerView.setAdapter(adapter);

        MaterialButton btnRetry = view.findViewById(R.id.btnRetry);
        btnRetry.setOnClickListener(v -> viewModel.load());

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                view.findViewById(R.id.progressLoading)
                        .setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));
        viewModel.getError().observe(getViewLifecycleOwner(), error ->
                view.findViewById(R.id.errorView)
                        .setVisibility(Boolean.TRUE.equals(error) ? View.VISIBLE : View.GONE));
        viewModel.getNews().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            view.findViewById(R.id.emptyView)
                    .setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.load();
    }

    private void openNews(NewsItem item) {
        String url = item.getUrl();
        if (url == null || url.isEmpty()) {
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {
        }
    }
}