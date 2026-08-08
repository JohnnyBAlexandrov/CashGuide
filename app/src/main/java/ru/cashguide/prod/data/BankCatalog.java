package ru.cashguide.prod.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Встроенный справочник крупных российских банков.
 * Логотипы подтягиваются из ресурсов drawable/logo_<slug> (webp/png/vector).
 * Если ресурса нет, для отображения используется буква-аватар.
 */
public final class BankCatalog {

    /** Банк из встроенного справочника. */
    public static final class Bank {
        public final String name;
        public final String slug;

        Bank(String name, String slug) {
            this.name = name;
            this.slug = slug;
        }
    }

    private static final String[] NAMES = {
            "СберБанк", "sber",
            "Т-Банк", "tbank",
            "Альфа-Банк", "alfa",
            "ВТБ", "vtb",
            "Газпромбанк", "gazprombank",
            "Россельхозбанк", "rshb",
            "Райффайзенбанк", "raiffeisen",
            "Почта Банк", "pochta",
            "Совкомбанк", "sovcombank",
            "Озон Банк", "ozon",
            "Яндекс Банк", "yandex",
            "Московский кредитный банк", "mkb",
            "Хоум Кредит Банк", "homecredit",
            "АК Барс Банк", "akbars",
            "Банк Санкт-Петербург", "bspb",
            "Уралсиб", "uralsib",
            "УБРиР", "ubrr",
            "МТС Банк", "mts",
            "Ренессанс Кредит", "rencredit",
            "Русский Стандарт", "rsb",
            "Банк Открытие", "otkritie",
            "БКС Банк", "bks",
            "Модульбанк", "modul",
            "Точка", "tochka",
            "Возрождение", "vozrozhdenie",
            "Экспобанк", "expobank",
            "Фора-Банк", "fora",
            "Норвик Банк", "norvik",
            "Авангард", "avangard",
            "Делобанк", "delobank",
    };

    private static final Bank[] BANKS = build();

    private BankCatalog() {
    }

    private static Bank[] build() {
        Bank[] banks = new Bank[NAMES.length / 2];
        for (int i = 0; i < banks.length; i++) {
            banks[i] = new Bank(NAMES[i * 2], NAMES[i * 2 + 1]);
        }
        return banks;
    }

    public static List<Bank> getAll() {
        List<Bank> result = new ArrayList<>();
        for (Bank bank : BANKS) {
            result.add(bank);
        }
        return result;
    }

    /** Возвращает slug по имени банка (без учёта регистра) или null. */
    public static String slugFor(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        for (Bank bank : BANKS) {
            if (bank.name.equalsIgnoreCase(trimmed)) {
                return bank.slug;
            }
        }
        return null;
    }
}