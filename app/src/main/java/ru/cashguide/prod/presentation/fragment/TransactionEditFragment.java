package ru.cashguide.prod.presentation.fragment;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import java.util.ArrayList;
import java.util.List;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import ru.cashguide.prod.R;
import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.data.local.db.Transaction;
import ru.cashguide.prod.presentation.viewmodel.TransactionEditViewModel;
import ru.cashguide.prod.util.Formatting;

public class TransactionEditFragment extends Fragment {

    private TransactionEditViewModel viewModel;
    private final List<Card> cardHolder = new ArrayList<>();
    private final List<String> knownCategories = new ArrayList<>();
    private long selectedDateMillis = 0L;

    private Spinner spCard;
    private AutoCompleteTextView actCategory;
    private MaterialButton btnDate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        long transactionId = -1L;
        long preselectedCardId = -1L;
        if (getArguments() != null) {
            transactionId = getArguments().getLong("transactionId", -1L);
            preselectedCardId = getArguments().getLong("cardId", -1L);
        }

        viewModel = new ViewModelProvider(this).get(TransactionEditViewModel.class);
        viewModel.init(transactionId, preselectedCardId);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(transactionId > 0
                ? R.string.transaction_edit_title
                : R.string.transaction_new_title);
        NavigationUI.setupWithNavController(toolbar, NavHostFragment.findNavController(this));

        spCard = view.findViewById(R.id.spCard);
        actCategory = view.findViewById(R.id.actCategory);
        actCategory.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, new ArrayList<>()));

        RadioGroup rgType = view.findViewById(R.id.rgType);
        TextInputEditText etAmount = view.findViewById(R.id.etAmount);
        TextInputEditText etNote = view.findViewById(R.id.etNote);
        btnDate = view.findViewById(R.id.btnDate);
        MaterialButton btnSave = view.findViewById(R.id.btnSave);

        selectedDateMillis = System.currentTimeMillis();
        updateDateLabel();

        btnDate.setOnClickListener(v -> pickDate());
        btnSave.setOnClickListener(v -> {
            int position = spCard.getSelectedItemPosition();
            Card selectedCard = (position >= 0 && position < cardHolder.size())
                    ? cardHolder.get(position)
                    : null;
            boolean isExpense = rgType.getCheckedRadioButtonId() == R.id.rbExpense;
            double amount;
            try {
                amount = Formatting.parseNumber(
                        etAmount.getText() == null ? "" : etAmount.getText().toString());
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Укажите корректную сумму", Toast.LENGTH_SHORT).show();
                return;
            }
            String category = actCategory.getText() == null
                    ? "" : actCategory.getText().toString().trim();
            String note = etNote.getText() == null ? "" : etNote.getText().toString();
            viewModel.save(selectedCard, isExpense, amount, category, selectedDateMillis, note);
        });

        viewModel.getCards().observe(getViewLifecycleOwner(), cards -> {
            cardHolder.clear();
            if (cards != null) {
                cardHolder.addAll(cards);
            }
            List<String> names = new ArrayList<>();
            for (Card card : cardHolder) {
                names.add(card.bankName + " • " + card.cardName);
            }
            spCard.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_dropdown_item, names));
        });

        viewModel.getCategories().observe(getViewLifecycleOwner(), names -> {
            if (names == null) {
                return;
            }
            knownCategories.clear();
            knownCategories.addAll(names);
            actCategory.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_list_item_1, names));
        });

        viewModel.getTransaction().observe(getViewLifecycleOwner(), transaction -> {
            if (transaction == null || transaction.id <= 0) {
                return;
            }
            selectCard(transaction.cardId);
            rgType.check(Transaction.TYPE_INCOME.equals(transaction.type)
                    ? R.id.rbIncome : R.id.rbExpense);
            etAmount.setText(Formatting.decimal(transaction.amount));
            actCategory.setText(transaction.category, false);
            if (transaction.date > 0) {
                selectedDateMillis = transaction.date;
                updateDateLabel();
            }
            etNote.setText(transaction.note);
        });

        viewModel.getCard().observe(getViewLifecycleOwner(), card -> {
            if (card != null) {
                selectCard(card.id);
            }
        });

        viewModel.getMessage().observe(getViewLifecycleOwner(),
                msg -> Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show());
        viewModel.getCloseScreen().observe(getViewLifecycleOwner(), close -> {
            if (close != null && close) {
                NavHostFragment.findNavController(TransactionEditFragment.this).popBackStack();
            }
        });
    }

    private void selectCard(long cardId) {
        for (int i = 0; i < cardHolder.size(); i++) {
            if (cardHolder.get(i).id == cardId) {
                spCard.setSelection(i);
                return;
            }
        }
    }

    private void pickDate() {
        LocalDate initial = Instant.ofEpochMilli(selectedDateMillis)
                .atZone(ZoneId.systemDefault()).toLocalDate();
        new DatePickerDialog(requireContext(),
                (picker, year, month, dayOfMonth) -> {
                    selectedDateMillis = LocalDate.of(year, month + 1, dayOfMonth)
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant().toEpochMilli();
                    updateDateLabel();
                },
                initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    private void updateDateLabel() {
        btnDate.setText(getString(R.string.tx_date, Formatting.formatDate(selectedDateMillis)));
    }
}
