import com.wizardofwanks.receipt.ReceiptModel;

import java.util.List;

/** Plain-JVM checks for the receipt math, parsing, saving and persistence. */
public class ReceiptModelTest {

    private static int checks = 0;

    public static void main(String[] args) {
        totalsFromPhotos();
        editorStartsBlank();
        editorBatchRoundTrip();
        notesAndBlankLines();
        wrappedLinesStayWithTheirItem();
        commitAppendsAndRecalculates();
        draftSurvivesLeavingWithoutSaving();
        duplicateLineInBatchAddsOneCopy();
        persistenceRoundTrip();
        rateFormatting();
        System.out.println("OK - " + checks + " checks passed");
    }

    private static void totalsFromPhotos() {
        ReceiptModel receipt = ReceiptModel.sample();
        eq("subtotal", 195.57, receipt.subtotal());
        eq("items sold", 6, receipt.itemsSold());
        eq("tax 1 (8.25 %)", 16.13, receipt.tax1());
        eq("tax 12 (0 %)", 0.0, receipt.tax2());
        eq("total", 211.70, receipt.total());
        eq("timestamp", "11/25/26 5:17:26", receipt.timestampText());
    }

    private static void editorStartsBlank() {
        ReceiptModel receipt = ReceiptModel.sample();
        check("editor text blank on first use", receipt.entryText.isEmpty());
        check("nothing committed yet", receipt.committedItems.isEmpty());
    }

    private static void editorBatchRoundTrip() {
        ReceiptModel receipt = ReceiptModel.sample();
        String text = receipt.editorText();
        check("editor text has six lines", text.split("\n").length == 6);
        check("price glued to last word", text.contains("Adult/Teen$21.64"));
        List<ReceiptModel.Item> parsed = ReceiptModel.parseEditorText(text, receipt.items);
        eq("parsed items", 6, parsed.size());
        eq("first price", 21.64, parsed.get(0).price);
        eq("second price", 123.27, parsed.get(1).price);
        check("upc kept for matching item", "052107214339".equals(parsed.get(1).upc));
        check("descriptions stored upper case", parsed.get(0).desc.startsWith("MAINSTAYS"));
    }

    private static void notesAndBlankLines() {
        ReceiptModel receipt = ReceiptModel.sample();
        String text = "##User enters Item price tag description.\n\n"
                + "Great Value Milk$3.48\n\n"
                + "##Append items listed price with no space between last word and price\n"
                + "Ozark Trail Chair$24.00\n";
        List<ReceiptModel.Item> parsed = ReceiptModel.parseEditorText(text, receipt.items);
        eq("notes ignored", 2, parsed.size());
        check("note text never becomes an item",
                !parsed.get(0).desc.contains("#") && !parsed.get(1).desc.contains("#"));
        eq("first parsed price", 3.48, parsed.get(0).price);
    }

    private static void wrappedLinesStayWithTheirItem() {
        ReceiptModel receipt = ReceiptModel.sample();
        String text = "Ozark Trail 4 Person Family\nCabin Tent, Blue$123.27\n\n"
                + "Great Value Bread\n$2.98";
        List<ReceiptModel.Item> parsed = ReceiptModel.parseEditorText(text, receipt.items);
        eq("wrapped item count", 2, parsed.size());
        check("wrapped description kept together",
                parsed.get(0).desc.contains("FAMILY CABIN TENT"));
        eq("wrapped item price", 123.27, parsed.get(0).price);
        eq("price-only line completes the item", 2.98, parsed.get(1).price);
    }

    private static void commitAppendsAndRecalculates() {
        ReceiptModel receipt = ReceiptModel.sample();

        int appended = receipt.commitEntry("Great Value Whole Milk 1 gal$3.48");
        eq("appended one item", 1, appended);
        eq("items sold after add", 7, receipt.itemsSold());
        eq("subtotal after add", 199.05, receipt.subtotal());
        eq("tax after add", 16.42, receipt.tax1());
        eq("total after add", 215.47, receipt.total());
        check("entry text remembered for next open", receipt.entryText.contains("3.48"));
        check("draft matches the saved batch", receipt.draftText.equals(receipt.entryText));
        check("existing items kept", receipt.items.get(0).desc.startsWith("MAINSTAYS"));

        // Opening the editor again with that same batch must not duplicate it.
        int again = receipt.commitEntry(receipt.entryText);
        eq("re-saving the same batch appends nothing", 0, again);
        eq("item count unchanged", 7, receipt.itemsSold());

        // An extra line is appended while the already-saved lines stay untouched.
        int extraLine = receipt.commitEntry(receipt.entryText + "\nGreat Value Bread$2.98");
        eq("only the new line is appended", 1, extraLine);
        eq("item count after extra line", 8, receipt.itemsSold());
        check("last item is the bread", receipt.items.get(7).desc.contains("BREAD"));
    }

    private static void draftSurvivesLeavingWithoutSaving() {
        ReceiptModel receipt = ReceiptModel.sample();
        // user types a list, then leaves the editor without pressing Save
        receipt.draftText = "Great Value Milk$3.48\nBread$2.98";

        ReceiptModel reopened = ReceiptModel.deserialize(receipt.serialize());
        check("unfinished list is restored", reopened.draftText.contains("Bread$2.98"));
        eq("draft alone does not touch items sold", 6, reopened.itemsSold());
        eq("draft alone commits nothing", 0, reopened.committedItems.size());
        eq("draft is not counted as saved", 0, reopened.entryText.length());

        // saving that draft afterwards appends its two items
        eq("draft can be saved later", 2, reopened.commitEntry(reopened.draftText));
        eq("items sold after saving the draft", 8, reopened.itemsSold());
    }

    private static void duplicateLineInBatchAddsOneCopy() {
        ReceiptModel receipt = ReceiptModel.sample();
        eq("first batch appended", 2, receipt.commitEntry("Ozark Trail Chair$24.00\nBread$2.98"));
        // The Duplicate button copies a line; saving should add just that copy.
        int appended = receipt.commitEntry("Ozark Trail Chair$24.00\nOzark Trail Chair$24.00\nBread$2.98");
        eq("duplicated line appended once", 1, appended);
        eq("item count after duplicate", 9, receipt.itemsSold());
        eq("duplicate carries the price", 24.00, receipt.items.get(8).price);
    }

    private static void persistenceRoundTrip() {
        ReceiptModel receipt = ReceiptModel.sample();
        receipt.setStoreBlockText("WAL*MART\nWM SUPERCENTER\n408-555-0100\n1 MAIN ST\nSAN JOSE, CA 95118");
        receipt.tax1Rate = 7.5;
        receipt.stLine = "ST# 00001 OP# 1 TE# 1 TR# 1";
        eq("batch appended", 1, receipt.commitEntry("Test Item, Blue$9.99"));

        ReceiptModel loaded = ReceiptModel.deserialize(receipt.serialize());
        eq("store lines", 5, loaded.storeLines.size());
        check("store line value", "408-555-0100".equals(loaded.storeLines.get(2)));
        check("status line", receipt.stLine.equals(loaded.stLine));
        eq("tax rate", 7.5, loaded.tax1Rate);
        eq("item count", 7, loaded.items.size());
        check("last item desc", "TEST ITEM, BLUE".equals(loaded.items.get(6).desc));
        eq("last item price", 9.99, loaded.items.get(6).price);
        eq("loaded total", receipt.total(), loaded.total());
        check("editor batch restored", receipt.entryText.equals(loaded.entryText));
        eq("committed count restored", 1, loaded.committedItems.size());
        eq("re-saving a restored batch appends nothing", 0, loaded.commitEntry(loaded.entryText));
        eq("item count after reopen", 7, loaded.itemsSold());
    }

    private static void rateFormatting() {
        check("8.25 rate", "8.25 %".equals(ReceiptModel.rateText(8.25)));
        check("zero rate", "0 %".equals(ReceiptModel.rateText(0)));
        check("7.5 rate", "7.5 %".equals(ReceiptModel.rateText(7.5)));
    }

    private static void check(String label, boolean condition) {
        checks++;
        if (!condition) {
            throw new AssertionError("FAILED: " + label);
        }
    }

    private static void eq(String label, double expected, double actual) {
        checks++;
        if (Math.abs(expected - actual) > 0.0001) {
            throw new AssertionError("FAILED: " + label + " expected " + expected + " but was " + actual);
        }
    }

    private static void eq(String label, int expected, int actual) {
        checks++;
        if (expected != actual) {
            throw new AssertionError("FAILED: " + label + " expected " + expected + " but was " + actual);
        }
    }

    private static void eq(String label, String expected, String actual) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError("FAILED: " + label + " expected '" + expected + "' but was '" + actual + "'");
        }
    }
}
