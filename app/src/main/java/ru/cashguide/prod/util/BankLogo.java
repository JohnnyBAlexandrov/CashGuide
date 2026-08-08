package ru.cashguide.prod.util;

import android.content.Context;

import ru.cashguide.prod.data.BankCatalog;

/**
 * Поиск ресурса-логотипа банка по имени.
 * Ищет drawable с именем logo_<slug>; если его нет, возвращает 0.
 */
public final class BankLogo {

    private BankLogo() {
    }

    public static int resFor(Context context, String bankName) {
        String slug = BankCatalog.slugFor(bankName);
        if (slug == null) {
            return 0;
        }
        return context.getResources().getIdentifier(
                "logo_" + slug,
                "drawable",
                context.getPackageName());
    }
}