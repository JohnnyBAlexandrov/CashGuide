package ru.cashguide.prod.util;

import java.util.Arrays;
import java.util.List;

/** Справочник категорий покупок, используемых во всём приложении. */
public final class CategoryCatalog {

    public static final List<String> ALL = Arrays.asList(
            "Супермаркеты",
            "АЗС",
            "Кафе и рестораны",
            "Аптеки",
            "Транспорт",
            "Такси",
            "Одежда и обувь",
            "Электроника",
            "Дом и ремонт",
            "Развлечения",
            "Путешествия",
            "Связь и интернет",
            "Красота и здоровье",
            "Прочее");

    private CategoryCatalog() {
    }
}
