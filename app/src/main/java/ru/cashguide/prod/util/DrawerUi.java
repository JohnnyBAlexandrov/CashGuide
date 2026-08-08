package ru.cashguide.prod.util;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.MaterialToolbar;

import ru.cashguide.prod.R;

/**
 * Подключает тулбар фрагмента к навигации с выдвижным меню:
 * на экранах «вершины меню» показывает гамбургер, на вложенных — стрелку назад.
 */
public final class DrawerUi {

    private DrawerUi() {
    }

    public static void setupWithNavController(MaterialToolbar toolbar, Fragment fragment) {
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.mainFragment, R.id.searchFragment, R.id.historyFragment,
                R.id.categoriesFragment, R.id.bankDirectoryFragment, R.id.newsFragment)
                .setDrawerLayout(fragment.requireActivity().findViewById(R.id.drawer_layout))
                .build();
        NavigationUI.setupWithNavController(toolbar,
                NavHostFragment.findNavController(fragment), appBarConfiguration);
    }
}