package ru.cashguide.prod.presentation.fragment;

import android.os.Bundle;
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

import ru.cashguide.prod.R;
import ru.cashguide.prod.data.local.db.Category;
import ru.cashguide.prod.presentation.adapter.CategoryAdapter;
import ru.cashguide.prod.presentation.viewmodel.CategoriesViewModel;
import ru.cashguide.prod.util.DrawerUi;

public class CategoriesFragment extends Fragment {

    private CategoriesViewModel viewModel;
    private CategoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_categories, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CategoriesViewModel.class);
        viewModel.start();

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        DrawerUi.setupWithNavController(toolbar, this);

        TextInputEditText etCategoryName = view.findViewById(R.id.etCategoryName);
        MaterialButton btnAdd = view.findViewById(R.id.btnAdd);

        RecyclerView recyclerView = view.findViewById(R.id.rvCategories);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CategoryAdapter(new CategoryAdapter.Listener() {
            @Override
            public void onEdit(Category category) {
                showRenameDialog(category);
            }

            @Override
            public void onDelete(Category category) {
                confirmDelete(category);
            }
        });
        recyclerView.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> {
            String name = etCategoryName.getText() == null
                    ? "" : etCategoryName.getText().toString();
            viewModel.add(name);
            etCategoryName.setText("");
        });

        viewModel.getCategories().observe(getViewLifecycleOwner(), adapter::submitList);
        viewModel.getMessage().observe(getViewLifecycleOwner(),
                msg -> Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show());
    }

    private void showRenameDialog(Category category) {
        EditText input = new EditText(requireContext());
        input.setText(category.name);
        input.setSelection(category.name.length());
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.rename)
                .setView(input)
                .setPositiveButton(R.string.save, (d, w) ->
                        viewModel.rename(category, input.getText() == null
                                ? "" : input.getText().toString()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmDelete(Category category) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete)
                .setMessage(R.string.delete_category_message)
                .setPositiveButton(R.string.delete, (d, w) -> viewModel.delete(category))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}