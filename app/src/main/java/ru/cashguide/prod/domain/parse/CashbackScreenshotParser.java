package ru.cashguide.prod.domain.parse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.threeten.bp.LocalDate;
import ru.cashguide.prod.util.CategoryNormalizer;

/**
 * Извлекает из распознанного текста скриншота банковского приложения:
 * сумму операции, название магазина, категорию покупки и дату.
 * Результат эвристический — пользователь проверяет значения перед сохранением.
 */
public final class CashbackScreenshotParser {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?<![0-9])([0-9]{1,3}(?:[\\u00A0 ][0-9]{3})*|[0-9]+)(?:[,.]\\s?([0-9]{1,2}))?\\s*(?:₽|руб\\.?|р\\.|рублей|рубля|РУБ|RUB|RUR)?");

    private static final Pattern PERCENT_PATTERN = Pattern.compile(
            "([0-9]{1,3}(?:[\\u00A0 ][0-9]{3})*|[0-9]+)(?:[,.]\\s?([0-9]{1,2}))?\\s*%");

    private static final Pattern LEADING_PERCENT_PATTERN = Pattern.compile(
            "^\\s*([0-9]{1,3}(?:[\\u00A0 ][0-9]{3})*|[0-9]+)(?:[,.]\\s?([0-9]{1,2}))?\\s*%");

    private static final Pattern PERCENT_TOKEN_PATTERN =
            Pattern.compile("[0-9]+(?:[,.][0-9]{1,2})?\\s*%");

    private static final Pattern LETTER_PATTERN = Pattern.compile("[A-Za-zА-Яа-яЁё]");

    private static final Pattern DATE_PATTERN = Pattern.compile("([0-3]?[0-9])[./-]([0-1]?[0-9])[./-](20[0-9]{2})");

    private static final String[] KEYWORDS = {
            "супермаркеты",
            "азс",
            "кафе и рестораны",
            "аптеки",
            "транспорт",
            "такси",
            "одежда и обувь",
            "электроника",
            "дом и ремонт",
            "развлечения",
            "путешествия",
            "связь и интернет",
            "красота и здоровье",
            "продукты",
            "прочее",
    };

    private static final String[][] MERCHANT_KEYWORDS = {
            {"пятёрочка", "пятерочка", "магнит", "ашан", "лента", "дикайси", "перекресток", "перекрёсток", "вкусвилл", "карусель", "окей", "супермаркет", "продукт", "глобус"},
            {"азс", "заправка", "нефть", "газпромнефть", "лукойл", "роснефть"},
            {"кафе", "ресторан", "кофе", "бургер", "макдональдс", "кфс", "суши", "пицца", "вкусно"},
            {"аптека", "аптечный"},
            {"метро", "метрополитен", "трамвай", "автобус", "троллейбус", "электричк", "билет"},
            {"такси", "яндекс такси", "ситимобил"},
            {"одежда", "обувь", "adidas", "nike", "универмаг"},
            {"электроник", "мвидео", "эльдорадо", "ситилинк", "днс", "мегафон"},
            {"дом", "ремонт", "строительн", "мебель", "ikea", "леруа"},
            {"кино", "развлечен", "steam", "playstation", "xbox", "билеты"},
            {"отель", "гостиниц", "авиа", "авиакомпан", "путешеств", "билеты на"},
            {"связь", "интернет", "мтс", "билайн", "мегафон", "теле2"},
            {"салон", "красоты", "фитнес", "здоров", "клиник"},
            {},
    };

    private final List<String> knownCategories;
    private final List<String> categoryCandidates;

    public CashbackScreenshotParser(List<String> knownCategories) {
        this.knownCategories = knownCategories;
        this.categoryCandidates = new ArrayList<>();
        if (knownCategories != null) {
            for (String category : knownCategories) {
                if (category != null && !category.isEmpty()) {
                    categoryCandidates.add(category);
                }
            }
        }
        for (String keyword : KEYWORDS) {
            categoryCandidates.add(keyword);
        }
        categoryCandidates.sort((a, b) -> Integer.compare(b.length(), a.length()));
    }

    public ParsedScreenshot parse(String text) {
        if (text == null || text.isEmpty()) {
            return ParsedScreenshot.empty();
        }
        String normalized = text.replace('\u00A0', ' ');
        double amount = findAmount(normalized);
        LocalDate date = findDate(normalized);
        String category = findCategory(normalized);

        ParsedScreenshot result = new ParsedScreenshot();
        result.amount = amount;
        result.date = date;
        result.category = category;
        return result;
    }

    /**
     * Разбирает скриншот кэшбэка для экрана «Настройка кэшбэка»:
     * по строкам определяет категорию и процент кэшбэка (и лимит, если указан).
     */
    public SettingsResult parseSettings(String text) {
        SettingsResult result = new SettingsResult();
        if (text == null || text.isEmpty()) {
            return result;
        }
        String[] lines = text.replace('\u00A0', ' ').split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            Matcher leading = LEADING_PERCENT_PATTERN.matcher(line);
            if (leading.find()) {
                Double percent = parsePercentFromGroup(line.substring(leading.start(), leading.end()));
                String name = line.substring(leading.end()).trim();
                if (percent == null || name.isEmpty() || !LETTER_PATTERN.matcher(name).find()) {
                    continue;
                }
                String category = matchCategoryName(name);
                if (category == null) {
                    category = findClosestCategory(name);
                }
                if (category == null) {
                    category = sanitizeNewCategoryName(name);
                }
                if (category != null) {
                    result.percentByCategory.put(category, percent);
                    Double limit = findLimit(line);
                    if (limit != null) {
                        result.limitByCategory.put(category, limit);
                    }
                } else {
                    result.unrecognizedLines.add(line);
                }
                continue;
            }
            String category = matchCategoryName(line);
            if (category == null) {
                category = findClosestCategory(line);
            }
            if (category != null) {
                Double percent = findPercent(line);
                if (percent == null) {
                    percent = findPercentNearby(lines, i + 1);
                }
                if (percent != null) {
                    result.percentByCategory.put(category, percent);
                }
                Double limit = findLimit(line);
                if (limit == null) {
                    limit = findLimitNearby(lines, i + 1);
                }
                if (limit != null) {
                    result.limitByCategory.put(category, limit);
                }
            } else if (findPercent(line) != null || findPercentNearby(lines, i + 1) != null) {
                result.unrecognizedLines.add(line);
            }
        }
        return result;
    }

    private static final Pattern LIMIT_WORD_PATTERN = Pattern.compile(
            "(?i)(лимит|максимальн|до\\s)");

    private static final Pattern SUM_TOKEN_PATTERN = Pattern.compile(
            "(?i)[0-9][0-9\\s.,]*(?:₽|рублей|рубля|руб\\.?|р\\.|rub|rur)?");

    /**
     * Принимает распознанное имя новой категории (не входящей в каталог),
     * если строка похожа на название: обрезает служебные слова и суммы,
     * требует не менее трёх букв. Искажённые OCR имена с латиницей
     * (например {@code MeTCKIM MMp} вместо «Детский мир») отбрасываются.
     * Иначе возвращает {@code null}.
     */
    private static String sanitizeNewCategoryName(String name) {
        if (name == null) {
            return null;
        }
        String value = name.trim();
        Matcher matcher = LIMIT_WORD_PATTERN.matcher(value);
        if (matcher.find()) {
            value = value.substring(0, matcher.start()).trim();
        }
        value = SUM_TOKEN_PATTERN.matcher(value).replaceAll("").replaceAll("\\s+", " ").trim();
        value = value.replaceAll("[₽,|»«'’\\-–—/]", " ").replaceAll("\\s+", " ").trim();
        value = value.replaceFirst("^р\\.?\\s+", "").trim();
        if (value.length() < 3) {
            return null;
        }
        if (value.replaceAll("[^A-Za-zА-Яа-яЁё]", "").length() < 3) {
            return null;
        }
        boolean hasLatin = value.matches(".*[A-Za-z].*");
        if (hasLatin) {
            return null;
        }
        return value;
    }

    private static Double parsePercentFromGroup(String group) {
        String digits = group == null ? "" : group.replaceAll("[^0-9,.]", "");
        try {
            double value = Double.parseDouble(digits.replace(',', '.'));
            if (value > 0 && value <= 100) {
                return value;
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    /**
     * Нечёткий поиск ближайшей категории каталога по названию,
     * искажённому распознаванием (латиница вместо кириллицы и т.п.).
     */
    private String findClosestCategory(String name) {
        String normalized = CategoryNormalizer.normalize(name);
        if (normalized.length() < 3) {
            return null;
        }
        String best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (String candidate : categoryCandidates) {
            String c = CategoryNormalizer.normalize(candidate);
            if (c.length() < 3) {
                continue;
            }
            int distance = CategoryNormalizer.levenshtein(normalized, c);
            int threshold = CategoryNormalizer.fuzzyThreshold(normalized);
            if (distance <= threshold && (best == null || distance < bestDistance)) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private Double findPercentNearby(String[] lines, int from) {
        for (int j = from; j < Math.min(from + 2, lines.length); j++) {
            Double percent = findPercent(lines[j]);
            if (percent != null) {
                return percent;
            }
        }
        return null;
    }

    private Double findLimitNearby(String[] lines, int from) {
        for (int j = from; j < Math.min(from + 2, lines.length); j++) {
            Double limit = findLimit(lines[j]);
            if (limit != null) {
                return limit;
            }
        }
        return null;
    }

    private String matchCategoryName(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        for (String candidate : categoryCandidates) {
            String c = candidate.toLowerCase(Locale.ROOT);
            if (c.length() < 3) {
                continue;
            }
            if (lower.contains(c) || (lower.length() >= 4 && c.contains(lower))) {
                return candidate;
            }
        }
        String normalizedLine = CategoryNormalizer.normalize(line);
        for (String candidate : categoryCandidates) {
            String normalizedCandidate = CategoryNormalizer.normalize(candidate);
            if (normalizedCandidate.length() < 3) {
                continue;
            }
            if (normalizedLine.contains(normalizedCandidate)
                    || normalizedCandidate.contains(normalizedLine)) {
                return candidate;
            }
        }
        return null;
    }

    private Double findPercent(String line) {
        Matcher matcher = PERCENT_PATTERN.matcher(line);
        if (matcher.find()) {
            String digits = matcher.group().replaceAll("[^0-9,.]", "");
            try {
                double value = Double.parseDouble(digits.replace(',', '.'));
                if (value > 0 && value <= 100) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private Double findLimit(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        if (!lower.contains("лимит") && !lower.contains("максимальн")
                && !lower.contains("max") && !lower.contains("до ")) {
            return null;
        }
        String withoutPercents = PERCENT_TOKEN_PATTERN.matcher(line).replaceAll("");
        double amount = findAmount(withoutPercents);
        if (amount > 0 && amount < 100_000_000) {
            return amount;
        }
        return null;
    }

    private double findAmount(String text) {
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        while (matcher.find()) {
            String group = matcher.group();
            if (group == null || group.isEmpty()) {
                continue;
            }
            String digits = group.replaceAll("[^0-9,.]", "");
            String cleaned = digits.replace(" ", "").replaceAll("\\.(?=[0-9]{3})", "").replace(',', '.');
            try {
                double value = Double.parseDouble(cleaned);
                if (value > 0 && value < 100_000_000) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return 0.0;
    }

    private LocalDate findDate(String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                int day = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int year = Integer.parseInt(matcher.group(3));
                if (day >= 1 && day <= 31 && month >= 1 && month <= 12) {
                    return LocalDate.of(year, month, day);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String findCategory(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (int i = 0; i < KEYWORDS.length; i++) {
            for (String keyword : MERCHANT_KEYWORDS[i]) {
                if (lower.contains(keyword)) {
                    return KEYWORDS[i];
                }
            }
        }
        if (knownCategories != null) {
            for (String category : knownCategories) {
                if (category != null && lower.contains(category.toLowerCase(Locale.ROOT))) {
                    return category;
                }
            }
        }
        return null;
    }

    public static final class ParsedScreenshot {
        public double amount;
        public LocalDate date;
        public String category;

        static ParsedScreenshot empty() {
            return new ParsedScreenshot();
        }
    }

    /** Результат разбора скриншота для настроек кэшбэка. */
    public static final class SettingsResult {
        public final Map<String, Double> percentByCategory = new HashMap<>();
        public final Map<String, Double> limitByCategory = new HashMap<>();
        /** Строки с процентом, по которым не удалось определить категорию. */
        public final List<String> unrecognizedLines = new ArrayList<>();

        public int matchedCount() {
            return percentByCategory.size() + limitByCategory.size();
        }
    }
}