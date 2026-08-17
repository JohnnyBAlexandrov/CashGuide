package ru.cashguide.prod.presentation.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import java.util.ArrayList;
import java.util.List;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import ru.cashguide.prod.R;
import ru.cashguide.prod.data.local.db.Bank;
import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.presentation.viewmodel.CardDetailViewModel;
import ru.cashguide.prod.util.Formatting;

public class CardDetailFragment extends Fragment {

    private static final String[] CURRENCIES = {"RUB", "USD", "EUR"};

    private CardDetailViewModel viewModel;
    private long cardId = -1L;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_card_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            cardId = getArguments().getLong("cardId", -1L);
        }

        viewModel = new ViewModelProvider(this).get(CardDetailViewModel.class);
        viewModel.init(cardId);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(cardId > 0 ? R.string.card_edit_title : R.string.card_new_title);
        NavigationUI.setupWithNavController(toolbar, NavHostFragment.findNavController(this));

        TextInputEditText etName = view.findViewById(R.id.etName);
        TextInputEditText etBalance = view.findViewById(R.id.etBalance);
        TextInputEditText etLimit = view.findViewById(R.id.etLimit);
        AutoCompleteTextView etBank = view.findViewById(R.id.etBank);
        etBank.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, new ArrayList<>()));
        Spinner spCurrency = view.findViewById(R.id.spCurrency);
        Button btnSave = view.findViewById(R.id.btnSave);
        Button btnDelete = view.findViewById(R.id.btnDelete);

        spCurrency.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, CURRENCIES));

        viewModel.getCard().observe(getViewLifecycleOwner(), card -> {
            if (card == null || card.id <= 0) {
                return;
            }
            etBank.setText(card.bankName);
            etName.setText(card.cardName);
            etBalance.setText(Formatting.decimal(card.balance));
            if (card.currency != null) {
                selectCurrency(spCurrency, card.currency);
            }
            if (card.monthlyCashbackLimit != null) {
                etLimit.setText(Formatting.decimal(card.monthlyCashbackLimit));
            }
            btnDelete.setVisibility(card.id > 0 ? View.VISIBLE : View.GONE);
        });

        btnSave.setOnClickListener(v -> viewModel.save(
                text(etBank),
                text(etName),
                text(etBalance),
                spCurrency.getSelectedItem() == null
                        ? "RUB"
                        : spCurrency.getSelectedItem().toString(),
                text(etLimit)));

        btnDelete.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_card_title)
                .setMessage(R.string.delete_card_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> viewModel.deleteCard())
                .setNegativeButton(R.string.cancel, null)
                .show());

        viewModel.getBanks().observe(getViewLifecycleOwner(), list -> {
            List<String> bankNames = new ArrayList<>();
            if (list != null) {
                for (Bank bank : list) {
                    bankNames.add(bank.name);
                }
            }
            etBank.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_list_item_1, bankNames));
        });

        viewModel.getMessage().observe(getViewLifecycleOwner(),
                msg -> Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show());
        viewModel.getCloseScreen().observe(getViewLifecycleOwner(), close -> {
            if (close != null && close) {
                NavHostFragment.findNavController(CardDetailFragment.this).popBackStack();
            }
        });
    }

    private void selectCurrency(Spinner spinner, String currency) {
        for (int i = 0; i < CURRENCIES.length; i++) {
            if (CURRENCIES[i].equals(currency)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private String text(android.widget.EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }
}
