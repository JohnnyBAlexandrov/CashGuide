package ru.cashguide.prod.presentation.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import ru.cashguide.prod.R;
import ru.cashguide.prod.presentation.adapter.CashbackCategoryAdapter;
import ru.cashguide.prod.presentation.viewmodel.CashbackSetupViewModel;
import ru.cashguide.prod.util.Formatting;

public class CashbackSetupFragment extends Fragment {

    private CashbackSetupViewModel viewModel;
    private CashbackCategoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cashback_setup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        long cardId = requireArguments().getLong("cardId");

        viewModel = new ViewModelProvider(this).get(CashbackSetupViewModel.class);
        viewModel.init(cardId);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        NavigationUI.setupWithNavController(toolbar, NavHostFragment.findNavController(this));

        TextView tvCardName = view.findViewById(R.id.tvCardName);
        TextView tvMonth = view.findViewById(R.id.tvMonth);
        MaterialButton btnCopy = view.findViewById(R.id.btnCopy);
        MaterialButton btnSave = view.findViewById(R.id.btnSave);

        RecyclerView recyclerView = view.findViewById(R.id.rvCategories);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CashbackCategoryAdapter();
        recyclerView.setAdapter(adapter);

        btnCopy.setOnClickListener(v -> viewModel.copyFromPreviousMonth());
        btnSave.setOnClickListener(v -> viewModel.saveAll(adapter.getItems()));

        viewModel.getCard().observe(getViewLifecycleOwner(), card -> {
            if (card != null) {
                tvCardName.setText(card.bankName + " • " + card.cardName);
            }
        });
        viewModel.getMonth().observe(getViewLifecycleOwner(),
                month -> tvMonth.setText(
                        tvMonth.getContext().getString(
                                R.string.cashback_for_month, Formatting.formatMonthYear(month))));
        viewModel.getSettings().observe(getViewLifecycleOwner(), adapter::submitList);
        viewModel.getMessage().observe(getViewLifecycleOwner(),
                msg -> Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show());
    }
}
