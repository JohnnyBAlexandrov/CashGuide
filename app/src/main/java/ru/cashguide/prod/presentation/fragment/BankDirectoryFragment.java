package ru.cashguide.prod.presentation.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ru.cashguide.prod.R;
import ru.cashguide.prod.data.local.db.Bank;
import ru.cashguide.prod.presentation.adapter.BankAdapter;
import ru.cashguide.prod.presentation.viewmodel.BankViewModel;
import ru.cashguide.prod.util.DrawerUi;

public class BankDirectoryFragment extends Fragment {

    private BankViewModel viewModel;
    private BankAdapter adapter;
    private final List<Bank> allBanks = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bank_directory, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(BankViewModel.class);
        viewModel.start();

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        DrawerUi.setupWithNavController(toolbar, this);

        RecyclerView recyclerView = view.findViewById(R.id.rvBanks);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BankAdapter(new BankAdapter.Listener() {
            @Override
            public void onEdit(Bank bank) {
                showRenameDialog(bank);
            }

            @Override
            public void onDelete(Bank bank) {
                confirmDelete(bank);
            }
        });
        recyclerView.setAdapter(adapter);

        TextInputEditText etBankName = view.findViewById(R.id.etBankName);
        MaterialButton btnAddBank = view.findViewById(R.id.btnAddBank);
        btnAddBank.setOnClickListener(v -> {
            String name = etBankName.getText() == null ? "" : etBankName.getText().toString();
            viewModel.add(name);
            etBankName.setText("");
        });

        TextInputEditText etSearch = view.findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                if (query.isEmpty()) {
                    adapter.submitList(allBanks);
                    return;
                }
                List<Bank> filtered = new ArrayList<>();
                for (Bank bank : allBanks) {
                    if (bank.name.toLowerCase(Locale.ROOT).contains(query)) {
                        filtered.add(bank);
                    }
                }
                adapter.submitList(filtered);
            }
        });

        viewModel.getBanks().observe(getViewLifecycleOwner(), banks -> {
            allBanks.clear();
            if (banks != null) {
                allBanks.addAll(banks);
            }
            adapter.submitList(allBanks);
        });

        viewModel.getMessage().observe(getViewLifecycleOwner(),
                msg -> Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show());
    }

    private void showRenameDialog(Bank bank) {
        EditText input = new EditText(requireContext());
        input.setText(bank.name);
        input.setSelection(bank.name.length());
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.rename)
                .setView(input)
                .setPositiveButton(R.string.save, (d, w) ->
                        viewModel.rename(bank, input.getText() == null
                                ? "" : input.getText().toString()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmDelete(Bank bank) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete)
                .setMessage(R.string.delete_bank_message)
                .setPositiveButton(R.string.delete, (d, w) -> viewModel.delete(bank))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}