package ru.cashguide.prod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ru.cashguide.prod.domain.parse.CashbackScreenshotParser;
import ru.cashguide.prod.util.CategoryCatalog;

public class CashbackScreenshotParserTest {

    private static CashbackScreenshotParser.SettingsResult parse(String text) {
        return new CashbackScreenshotParser(CategoryCatalog.ALL).parseSettings(text);
    }

    private static CashbackScreenshotParser.SettingsResult parseWithKnown(String text, String... extra) {
        List<String> known = new ArrayList<>(CategoryCatalog.ALL);
        for (String name : extra) {
            known.add(name);
        }
        return new CashbackScreenshotParser(known).parseSettings(text);
    }

    @Test
    public void ocrGarbledCategoryMapsToCatalog() {
        CashbackScreenshotParser.SettingsResult result = parse("2% IoДукты");
        assertEquals(Double.valueOf(2.0), result.percentByCategory.get("продукты"));
    }

    @Test
    public void clearCategoryMatchesCatalog() {
        CashbackScreenshotParser.SettingsResult result = parse("2% Продукты");
        assertEquals(Double.valueOf(2.0), result.percentByCategory.get("продукты"));
    }

    @Test
    public void unknownCleanCategoryIsRecognizedAsNew() {
        CashbackScreenshotParser.SettingsResult result = parse("2% Автоуслуги");
        assertEquals(Double.valueOf(2.0), result.percentByCategory.get("Автоуслуги"));
    }

    @Test
    public void mixedLatinCyrillicNoiseIsRejected() {
        CashbackScreenshotParser.SettingsResult result = parse("2% Моbильный");
        assertTrue(result.percentByCategory.isEmpty());
        assertNull(result.percentByCategory.get("Моbильный"));
    }

    @Test
    public void pureLatinOcrGarbageIsRejected() {
        CashbackScreenshotParser.SettingsResult result = parse("2% MeTCKIM MMp");
        assertTrue(result.percentByCategory.isEmpty());
        assertNull(result.percentByCategory.get("MeTCKIM MMp"));
        assertTrue(result.unrecognizedLines.contains("2% MeTCKIM MMp"));
    }

    @Test
    public void childWorldGarbageMapsToKnownCategory() {
        CashbackScreenshotParser.SettingsResult result = parseWithKnown("2% MeTCKIM MMp", "Детский мир");
        assertEquals(Double.valueOf(2.0), result.percentByCategory.get("Детский мир"));
        assertNull(result.percentByCategory.get("MeTCKIM MMp"));
    }

    @Test
    public void limitIsRecognizedAlongsideCategory() {
        CashbackScreenshotParser.SettingsResult result = parse("2% Автоуслуги лимит 1 000 ₽");
        assertEquals(Double.valueOf(2.0), result.percentByCategory.get("Автоуслуги"));
        assertEquals(Double.valueOf(1000.0), result.limitByCategory.get("Автоуслуги"));
    }
}