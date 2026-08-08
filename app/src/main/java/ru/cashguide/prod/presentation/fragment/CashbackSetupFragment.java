package ru.cashguide.prod.presentation.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import ru.cashguide.prod.R;
import ru.cashguide.prod.data.local.db.CashbackCategory;
import ru.cashguide.prod.data.ocr.TextRecognizerWrapper;
import ru.cashguide.prod.domain.parse.CashbackScreenshotParser;
import ru.cashguide.prod.presentation.adapter.CashbackCategoryAdapter;
import ru.cashguide.prod.presentation.viewmodel.CashbackSetupViewModel;
import ru.cashguide.prod.util.Formatting;

public class CashbackSetupFragment extends Fragment {

    private CashbackSetupViewModel viewModel;
    private CashbackCategoryAdapter adapter;
    private final List<String> knownCategories = new ArrayList<>();
    private final List<String> availableCategories = new ArrayList<>();
    private final List<String> allCategoryNames = new ArrayList<>();

    private final ActivityResultLauncher<PickVisualMediaRequest> pickScreenshot =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), this::onScreenshotPicked);

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
        MaterialButton btnAddCategory = view.findViewById(R.id.btnAddCategory);
        MaterialButton btnScan = view.findViewById(R.id.btnScan);
        MaterialButton btnCopy = view.findViewById(R.id.btnCopy);
        MaterialButton btnSave = view.findViewById(R.id.btnSave);

        RecyclerView recyclerView = view.findViewById(R.id.rvCategories);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CashbackCategoryAdapter(new CashbackCategoryAdapter.Listener() {
            @Override
            public void onDelete(CashbackCategory category) {
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.delete)
                        .setMessage(R.string.delete_cashback_category_message)
                        .setPositiveButton(R.string.delete, (d, w) -> viewModel.removeCategory(category))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }

            @Override
            public void onEdit(CashbackCategory category) {
                showEditCategoryDialog(category);
            }
        });
        recyclerView.setAdapter(adapter);

        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());
        btnScan.setOnClickListener(v -> pickScreenshot.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
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
        viewModel.getSettings().observe(getViewLifecycleOwner(), list -> {
            knownCategories.clear();
            if (list != null) {
                for (CashbackCategory item : list) {
                    knownCategories.add(item.category);
                }
            }
            adapter.submitList(list);
        });
        viewModel.getAvailableCategories().observe(getViewLifecycleOwner(), list -> {
            availableCategories.clear();
            if (list != null) {
                availableCategories.addAll(list);
            }
        });
        viewModel.getAllCategoryNames().observe(getViewLifecycleOwner(), list -> {
            allCategoryNames.clear();
            if (list != null) {
                allCategoryNames.addAll(list);
            }
        });
        viewModel.getMessage().observe(getViewLifecycleOwner(),
                msg -> Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show());
    }

    private void showAddCategoryDialog() {
        if (availableCategories.isEmpty()) {
            Toast.makeText(requireContext(), R.string.all_categories_added,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        final AutoCompleteTextView etName = new AutoCompleteTextView(requireContext());
        etName.setSingleLine(true);
        etName.setHint(R.string.tx_category);
        etName.setThreshold(0);
        etName.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, availableCategories));

        final EditText etPercent = new EditText(requireContext());
        etPercent.setSingleLine(true);
        etPercent.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etPercent.setHint(R.string.cashback_percent);

        final EditText etLimit = new EditText(requireContext());
        etLimit.setSingleLine(true);
        etLimit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etLimit.setHint(R.string.cashback_limit);

        layout.addView(etName, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        layout.addView(etPercent, params);
        LinearLayout.LayoutParams limitParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        limitParams.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        layout.addView(etLimit, limitParams);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_category)
                .setView(layout)
                .setPositiveButton(R.string.add_category, (d, w) -> {
                    String name = etName.getText() == null ? "" : etName.getText().toString();
                    double percent;
                    try {
                        percent = Formatting.parseNumber(etPercent.getText() == null
                                ? "" : etPercent.getText().toString());
                    } catch (Exception e) {
                        percent = 0.0;
                    }
                    Double limit = null;
                    String limitText = etLimit.getText() == null ? "" : etLimit.getText().toString();
                    if (!limitText.trim().isEmpty()) {
                        try {
                            limit = Formatting.parseNumber(limitText);
                        } catch (Exception e) {
                            limit = null;
                        }
                    }
                    viewModel.addCategory(name, percent, limit);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showEditCategoryDialog(CashbackCategory category) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        final AutoCompleteTextView etName = new AutoCompleteTextView(requireContext());
        etName.setSingleLine(true);
        etName.setHint(R.string.tx_category);
        etName.setThreshold(0);
        etName.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, availableCategories));

        final EditText etPercent = new EditText(requireContext());
        etPercent.setSingleLine(true);
        etPercent.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etPercent.setHint(R.string.cashback_percent);

        final EditText etLimit = new EditText(requireContext());
        etLimit.setSingleLine(true);
        etLimit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etLimit.setHint(R.string.cashback_limit);

        etName.setText(category.category);
        etPercent.setText(Formatting.decimal(category.percent));
        etLimit.setText(category.monthlyLimit == null
                ? "" : Formatting.decimal(category.monthlyLimit.doubleValue()));

        layout.addView(etName, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        layout.addView(etPercent, params);
        LinearLayout.LayoutParams limitParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        limitParams.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        layout.addView(etLimit, limitParams);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.rename)
                .setView(layout)
                .setPositiveButton(R.string.save, (d, w) -> {
                    String name = etName.getText() == null ? "" : etName.getText().toString();
                    double percent;
                    try {
                        percent = Formatting.parseNumber(etPercent.getText() == null
                                ? "" : etPercent.getText().toString());
                    } catch (Exception e) {
                        percent = 0.0;
                    }
                    Double limit = null;
                    String limitText = etLimit.getText() == null ? "" : etLimit.getText().toString();
                    if (!limitText.trim().isEmpty()) {
                        try {
                            limit = Formatting.parseNumber(limitText);
                        } catch (Exception e) {
                            limit = null;
                        }
                    }
                    category.category = name;
                    category.percent = percent;
                    category.monthlyLimit = limit;
                    viewModel.updateCategory(category);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void onScreenshotPicked(Uri uri) {
        if (uri == null) {
            return;
        }
        new TextRecognizerWrapper().recognize(requireContext(), uri)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        text -> {
                            List<String> candidates = new ArrayList<>();
                            candidates.addAll(allCategoryNames);
                            candidates.addAll(knownCategories);
                            CashbackScreenshotParser.SettingsResult result =
                                    new CashbackScreenshotParser(candidates).parseSettings(text);
                            int count = viewModel.applyRecognized(
                                    result.percentByCategory, result.limitByCategory);
                            if (count > 0) {
                                Toast.makeText(requireContext(),
                                        getString(
                                                R.string.scan_applied_categories, count),
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(requireContext(),
                                        R.string.scan_nothing_found,
                                        Toast.LENGTH_LONG).show();
                            }
                        },
                        throwable -> Toast.makeText(requireContext(),
                                "Не удалось распознать текст", Toast.LENGTH_SHORT).show());
    }
}
