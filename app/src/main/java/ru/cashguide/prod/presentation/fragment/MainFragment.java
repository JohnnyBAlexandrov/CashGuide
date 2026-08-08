package ru.cashguide.prod.presentation.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import ru.cashguide.prod.R;
import ru.cashguide.prod.data.local.db.Card;
import ru.cashguide.prod.presentation.adapter.CardAdapter;
import ru.cashguide.prod.presentation.viewmodel.MainViewModel;
import ru.cashguide.prod.util.DrawerUi;
import ru.cashguide.prod.util.Formatting;

public class MainFragment extends Fragment {

    private MainViewModel viewModel;
    private CardAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        RecyclerView recyclerView = view.findViewById(R.id.rvCards);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CardAdapter(new CardAdapter.Listener() {
            @Override
            public void onCardClick(Card card) {
                navigateCashbackSetup(card.id);
            }

            @Override
            public void onCashbackSetup(Card card) {
                navigateCashbackSetup(card.id);
            }

            @Override
            public void onEdit(Card card) {
                Bundle args = new Bundle();
                args.putLong("cardId", card.id);
                NavHostFragment.findNavController(MainFragment.this)
                        .navigate(R.id.cardDetailFragment, args);
            }

            @Override
            public void onDelete(Card card) {
                confirmDelete(card);
            }
        });
        recyclerView.setAdapter(adapter);
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                if (from < 0 || to < 0) {
                    return false;
                }
                adapter.moveItem(from, to);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }

            @Override
            public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder,
                                          int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                    viewHolder.itemView.setAlpha(0.7f);
                    viewHolder.itemView.setElevation(8f);
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setAlpha(1f);
                viewHolder.itemView.setElevation(0f);
                viewModel.reorderCards(adapter.getCardsInOrder());
            }
        }).attachToRecyclerView(recyclerView);

        TextView monthLabel = view.findViewById(R.id.monthLabel);
        view.findViewById(R.id.btnPrev).setOnClickListener(v -> viewModel.prevMonth());
        view.findViewById(R.id.btnNext).setOnClickListener(v -> viewModel.nextMonth());
        view.findViewById(R.id.fabAddCard).setOnClickListener(v ->
                NavHostFragment.findNavController(MainFragment.this).navigate(R.id.cardDetailFragment));

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        DrawerUi.setupWithNavController(toolbar, this);
        toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_search) {
                NavHostFragment.findNavController(MainFragment.this).navigate(R.id.searchFragment);
                return true;
            }
            return false;
        });

        viewModel.getCurrentMonth().observe(getViewLifecycleOwner(),
                month -> monthLabel.setText(Formatting.formatMonthYear(month)));

        viewModel.getCards().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            view.findViewById(R.id.emptyView)
                    .setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.getMessage().observe(getViewLifecycleOwner(),
                msg -> Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show());
    }

    private void navigateCashbackSetup(long cardId) {
        Bundle args = new Bundle();
        args.putLong("cardId", cardId);
        NavHostFragment.findNavController(MainFragment.this)
                .navigate(R.id.cashbackSetupFragment, args);
    }

    private void confirmDelete(Card card) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_card_title)
                .setMessage(R.string.delete_card_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> viewModel.deleteCard(card))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}