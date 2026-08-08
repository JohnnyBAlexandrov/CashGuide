package ru.cashguide.prod.util;

import java.util.Locale;

/**
 * Нормализация названий категорий для нечёткого сопоставления,
 * устойчивая к искажениям OCR: латиница вместо кириллицы и наоборот.
 */
public final class CategoryNormalizer {

    private CategoryNormalizer() {
    }

    /**
     * Заменяет часто путаемые OCR латинские буквы на их кириллические
     * соответствия по начертанию, затем приводит к нижнему регистру
     * и вычищает всё, кроме букв и цифр.
     */
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            char replaced = lookup(c);
            if ((replaced >= '0' && replaced <= '9')
                    || (replaced >= 'a' && replaced <= 'z')
                    || (replaced >= 'а' && replaced <= 'я')
                    || replaced == 'ё') {
                sb.append(replaced);
            }
        }
        return sb.toString();
    }

    public static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = (lookup(a.charAt(i - 1)) == lookup(b.charAt(j - 1))) ? 0 : 1;
                curr[j] = Math.min(
                        Math.min(curr[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + cost);
            }
            int[] swap = prev;
            prev = curr;
            curr = swap;
        }
        return prev[b.length()];
    }

    private static char lookup(char c) {
        switch (c) {
            case 'a': return 'а';
            case 'b': return 'б';
            case 'e': return 'е';
            case 'h': return 'н';
            case 'i': return 'и';
            case 'k': return 'к';
            case 'm': return 'м';
            case 'o': return 'о';
            case 'p': return 'р';
            case 't': return 'т';
            case 'u': return 'у';
            case 'x': return 'х';
            default: return c;
        }
    }
}