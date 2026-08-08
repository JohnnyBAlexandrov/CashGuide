package ru.cashguide.prod.presentation.fragment;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import ru.cashguide.prod.R;
import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.data.local.db.Transaction;
import ru.cashguide.prod.presentation.adapter.TransactionAdapter;
import ru.cashguide.prod.presentation.viewmodel.HistoryViewModel;
import ru.cashguide.prod.util.Formatting;

public class HistoryFragment extends Fragment {

    private HistoryViewModel viewModel;
    private TransactionAdapter adapter;
    private final List<Card> cardHolder = new ArrayList<>();

    private Long fromMillis = null;
    private Long toMillis = null;
    private MaterialButton btnFrom;
    private MaterialButton btnTo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        viewModel.start();

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        NavigationUI.setupWithNavController(toolbar, NavHostFragment.findNavController(this));

        RecyclerView recyclerView = view.findViewById(R.id.rvTransactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TransactionAdapter(transaction -> confirmDelete(transaction));
        recyclerView.setAdapter(adapter);

        Spinner spCardFilter = view.findViewById(R.id.spCardFilter);
        AutoCompleteTextView actCategory = view.findViewById(R.id.actCategoryFilter);
        actCategory.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, new ArrayList<>()));

        btnFrom = view.findViewById(R.id.btnFrom);
        btnTo = view.findViewById(R.id.btnTo);
        btnFrom.setText(R.string.filter_from);
        btnTo.setText(R.string.filter_to);
        btnFrom.setOnClickListener(v -> pickDate(false));
        btnTo.setOnClickListener(v -> pickDate(true));

        view.findViewById(R.id.btnApplyFilters).setOnClickListener(v -> {
            Long cardId = readCardFilter(spCardFilter);
            String category = actCategory.getText() == null
                    ? "" : actCategory.getText().toString().trim();
            viewModel.setFilters(
                    cardId,
                    category.isEmpty() ? null : category,
                    fromMillis,
                    toMillis);
        });

        view.findViewById(R.id.btnReset).setOnClickListener(v -> {
            spCardFilter.setSelection(0);
            actCategory.setText("", false);
            fromMillis = null;
            toMillis = null;
            btnFrom.setText(R.string.filter_from);
            btnTo.setText(R.string.filter_to);
            viewModel.clearFilters();
        });

        view.findViewById(R.id.fabAddTransaction).setOnClickListener(v ->
                NavHostFragment.findNavController(HistoryFragment.this)
                        .navigate(R.id.transactionEditFragment));

        viewModel.getCards().observe(getViewLifecycleOwner(), cards -> {
            cardHolder.clear();
            if (cards != null) {
                cardHolder.addAll(cards);
            }
            List<String> names = new ArrayList<>();
            names.add(getString(R.string.all_cards));
            for (Card card : cardHolder) {
                names.add(card.bankName + " • " + card.cardName);
            }
            spCardFilter.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_dropdown_item, names));
            adapter.setCards(cardHolder);
        });

        viewModel.getTransactions().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            boolean empty = list == null || list.isEmpty();
            view.findViewById(R.id.emptyView).setVisibility(empty ? View.VISIBLE : View.GONE);
        });
        viewModel.getCategories().observe(getViewLifecycleOwner(), names -> {
            if (names == null) {
                return;
            }
            actCategory.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_list_item_1, names));
        });
        viewModel.getMessage().observe(getViewLifecycleOwner(),
                msg -> Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show());
    }

    private Long readCardFilter(Spinner spinner) {
        int position = spinner.getSelectedItemPosition();
        if (position <= 0) {
            return null;
        }
        int index = position - 1;
        if (index < cardHolder.size()) {
            return cardHolder.get(index).id;
        }
        return null;
    }

    private void pickDate(boolean isTo) {
        long base = isTo
                ? (toMillis != null ? toMillis : System.currentTimeMillis())
                : (fromMillis != null ? fromMillis : System.currentTimeMillis());
        LocalDate initial = Instant.ofEpochMilli(base)
                .atZone(ZoneId.systemDefault()).toLocalDate();
        new DatePickerDialog(requireContext(),
                (picker, year, month, dayOfMonth) -> {
                    long millis = LocalDate.of(year, month + 1, dayOfMonth)
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant().toEpochMilli();
                    if (isTo) {
                        toMillis = millis;
                        btnTo.setText(getString(R.string.filter_to) + ": " + Formatting.formatDate(millis));
                    } else {
                        fromMillis = millis;
                        btnFrom.setText(getString(R.string.filter_from) + ": " + Formatting.formatDate(millis));
                    }
                },
                initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    private void confirmDelete(Transaction transaction) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete)
                .setMessage("Удалить операцию?")
                .setPositiveButton(R.string.delete, (dialog, which) -> viewModel.deleteTransaction(transaction))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}