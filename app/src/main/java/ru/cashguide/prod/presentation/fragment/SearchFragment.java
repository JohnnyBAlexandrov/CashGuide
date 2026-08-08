package ru.cashguide.prod.presentation.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

import ru.cashguide.prod.R;
import ru.cashguide.prod.presentation.adapter.BestCardAdapter;
import ru.cashguide.prod.presentation.viewmodel.SearchViewModel;
import ru.cashguide.prod.util.DrawerUi;
import ru.cashguide.prod.util.Formatting;

public class SearchFragment extends Fragment {

    private SearchViewModel viewModel;
    private BestCardAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        viewModel.loadCategories();

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        DrawerUi.setupWithNavController(toolbar, this);

        AutoCompleteTextView actCategory = view.findViewById(R.id.actCategory);
        actCategory.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, new ArrayList<>()));

        TextInputEditText etAmount = view.findViewById(R.id.etAmount);
        Button btnSearch = view.findViewById(R.id.btnSearch);
        RecyclerView recyclerView = view.findViewById(R.id.rvResults);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BestCardAdapter();
        recyclerView.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> {
            String category = actCategory.getText() == null
                    ? "" : actCategory.getText().toString().trim();
            String amountText = etAmount.getText() == null
                    ? "" : etAmount.getText().toString().trim();
            double amount;
            try {
                amount = Formatting.parseNumber(amountText);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Укажите корректную сумму", Toast.LENGTH_SHORT).show();
                return;
            }
            if (amount <= 0) {
                Toast.makeText(requireContext(), "Сумма должна быть больше нуля", Toast.LENGTH_SHORT).show();
                return;
            }
            if (category.isEmpty()) {
                Toast.makeText(requireContext(), "Укажите категорию", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.search(category, amount);
        });

        viewModel.getResults().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            boolean empty = list == null || list.isEmpty();
            view.findViewById(R.id.emptyView).setVisibility(empty ? View.VISIBLE : View.GONE);
        });
        viewModel.getCategories().observe(getViewLifecycleOwner(), names -> {
            if (names == null || names.isEmpty()) {
                return;
            }
            actCategory.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_list_item_1, names));
        });
        viewModel.getMessage().observe(getViewLifecycleOwner(),
                msg -> Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show());
    }
}
