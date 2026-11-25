package com.wizardofwanks.receipt;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure-Java receipt model: the editable receipt data, the total math and the
 * conversion between the item editor text and receipt items.
 *
 * No Android imports live here so the logic can be compiled and tested on a
 * plain JVM (see test/ReceiptModelTest.java).
 */
public class ReceiptModel {

    /** A single receipt line item. */
    public static class Item {
        public String desc;
        public String upc;
        public double price;

        public Item() {
            this("", "", 0d);
        }

        public Item(String desc, String upc, double price) {
            this.desc = desc == null ? "" : desc;
            this.upc = upc == null ? "" : upc;
            this.price = price;
        }

        public Item copy() {
            return new Item(desc, upc, price);
        }
    }

    /** "$21.64" at the very end of an item line. */
    private static final Pattern PRICE_AT = Pattern.compile("\\$\\s*(\\d+(?:\\.\\d{1,2})?)\\s*$");
    /** A bare amount at the end of an item line, e.g. " 21.64". */
    private static final Pattern BARE_PRICE_AT = Pattern.compile("\\s(\\d+\\.\\d{2})\\s*$");

    private static final Set<String> LOWERCASE_WORDS = new HashSet<String>(Arrays.asList(
            "and", "with", "of", "for", "from", "x", "to", "qt", "oz", "fl", "gal", "pk", "ct"));

    public final List<String> storeLines = new ArrayList<String>();
    public String stLine = "";
    public final List<Item> items = new ArrayList<Item>();
    public double tax1Rate = 8.25d;
    public double tax2Rate = 0d;
    public String tcNumber = "";
    public String visaLast4 = "";
    public long timestamp = 0L;

    /** The batch that was last saved from the item editor. */
    public String entryText = "";
    /**
     * Work in progress in the item editor. Autosaved as the user types so a
     * list being built is never lost by navigating away or closing the app.
     */
    public String draftText = "";
    /**
     * Item counts from the last saved batch, so re-opening the editor with that
     * same list and saving again does not add the same items twice.
     */
    public final Map<String, Integer> committedItems = new LinkedHashMap<String, Integer>();

    public static ReceiptModel sample() {
        ReceiptModel r = new ReceiptModel();
        r.storeLines.add("WAL*MART");
        r.storeLines.add("WM SUPERCENTER");
        r.storeLines.add("(408)-600-3072 MGR.TBD");
        r.storeLines.add("5095 ALMADEN EXPY");
        r.storeLines.add("SAN JOSE, CA 95118");
        r.stLine = "ST# 05884 OP# 009043 TE# 43 TR# 01871";
        r.tcNumber = "0103 1784 2359 0788 9942";
        r.visaLast4 = "7698";
        r.tax1Rate = 8.25d;
        r.tax2Rate = 0d;

        r.items.add(new Item("MAINSTAYS SUPER SOFT PLUSH BLANKET, DARK BLUE, FULL/QUEEN, ADULT/TEEN",
                "015023260321", 21.64d));
        r.items.add(new Item("OZARK TRAIL 4 PERSON FAMILY INSTANT CABIN TENT, 8' X 7' X 54\", "
                + "MADE WITH RECYCLED MATERIALS, BLUE,", "052107214339", 123.27d));
        r.items.add(new Item("EQ 70% ALCHL", "070797454542", 2.67d));
        r.items.add(new Item("EQ 70% ALCHL", "010973019391", 2.67d));
        r.items.add(new Item("COLEMAN NOW 18-CAN SOFT-SIDED 31.86 QT BACKPACK COOLER, GRAY AND RED",
                "083819659989", 22.66d));
        r.items.add(new Item("COLEMAN NOW 18-CAN SOFT-SIDED 31.86 QT BACKPACK COOLER, GRAY AND RED",
                "083208858547", 22.66d));

        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.NOVEMBER, 25, 5, 17, 26);
        c.set(Calendar.MILLISECOND, 0);
        r.timestamp = c.getTimeInMillis();
        return r;
    }

    /* ---------------------------------------------------------------- math */

    public double subtotal() {
        double sum = 0d;
        for (Item it : items) sum += it.price;
        return round2(sum);
    }

    public double tax1() {
        return round2(subtotal() * tax1Rate / 100d);
    }

    public double tax2() {
        return round2(subtotal() * tax2Rate / 100d);
    }

    public double total() {
        return round2(subtotal() + tax1() + tax2());
    }

    public int itemsSold() {
        return items.size();
    }

    public static double round2(double v) {
        return Math.round(v * 100d) / 100d;
    }

    public static String money(double v) {
        return String.format(Locale.US, "%.2f", v);
    }

    /** "8.25 %", "0 %", "7.5 %" - the rate as printed next to TAX. */
    public static String rateText(double v) {
        String s = String.format(Locale.US, "%.2f", v);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s + " %";
    }

    public String timestampText() {
        if (timestamp <= 0L) return "";
        return new SimpleDateFormat("MM/dd/yy h:mm:ss", Locale.US).format(new Date(timestamp));
    }

    /* ------------------------------------------------------------- editing */

    public String storeBlockText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < storeLines.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(storeLines.get(i));
        }
        return sb.toString();
    }

    public void setStoreBlockText(String text) {
        List<String> parsed = new ArrayList<String>();
        if (text != null) {
            for (String line : text.split("\n")) {
                String t = line.trim();
                if (!t.isEmpty()) parsed.add(t.toUpperCase(Locale.US));
            }
        }
        storeLines.clear();
        if (parsed.isEmpty()) {
            storeLines.addAll(sample().storeLines);
        } else {
            storeLines.addAll(parsed);
        }
    }

    /**
     * The text shown in the item editor. Price is appended directly after the
     * last word of the description with no space, in $x.xx form.
     */
    public String editorText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append('\n');
            Item it = items.get(i);
            sb.append(titleCase(it.desc)).append('$').append(money(it.price));
        }
        return sb.toString();
    }

    /** Identity of an item for duplicate detection (description plus price). */
    public static String signature(Item item) {
        return item.desc.replaceAll("\\s+", " ").trim().toUpperCase(Locale.US)
                + "|" + money(item.price);
    }

    /**
     * Applies an item editor batch to the receipt.
     *
     * Saving adds items to the receipt; it never clears what is already there.
     * Only lines that are new compared with the last saved batch are appended,
     * so opening the editor again with the previous list and saving is a no-op
     * (the timestamp is still refreshed).
     *
     * @return number of items appended, or -1 when the text contains no items
     */
    public int commitEntry(String editorTextValue) {
        List<Item> parsed = parseEditorText(editorTextValue, items);
        if (parsed.isEmpty()) return -1;

        Map<String, Integer> staged = new LinkedHashMap<String, Integer>();
        for (Item item : parsed) {
            String key = signature(item);
            Integer count = staged.get(key);
            staged.put(key, count == null ? 1 : count + 1);
        }

        int appended = 0;
        for (Map.Entry<String, Integer> entry : staged.entrySet()) {
            Integer previous = committedItems.get(entry.getKey());
            int extra = entry.getValue() - (previous == null ? 0 : previous);
            if (extra <= 0) continue;
            for (Item item : parsed) {
                if (extra == 0) break;
                if (signature(item).equals(entry.getKey())) {
                    items.add(item.copy());
                    extra--;
                    appended++;
                }
            }
        }

        committedItems.clear();
        committedItems.putAll(staged);
        entryText = editorTextValue == null ? "" : editorTextValue;
        draftText = entryText;
        timestamp = System.currentTimeMillis();
        return appended;
    }

    public static String titleCase(String s) {
        if (s == null) return "";
        String lower = s.toLowerCase(Locale.US);
        StringBuilder sb = new StringBuilder(lower.length());
        boolean capitalize = true;
        for (int i = 0; i < lower.length(); i++) {
            char ch = lower.charAt(i);
            if (Character.isLetter(ch)) {
                if (capitalize) {
                    sb.append(Character.toUpperCase(ch));
                    capitalize = false;
                } else {
                    sb.append(ch);
                }
            } else {
                sb.append(ch);
                if (ch == ' ' || ch == ',' || ch == '/') capitalize = true;
            }
        }
        String out = sb.toString();
        // keep small joining words and units lower case ("Gray and Red", "31.86 qt")
        StringBuilder fixed = new StringBuilder(out.length());
        int i = 0;
        while (i < out.length()) {
            int start = i;
            while (i < out.length() && (Character.isLetterOrDigit(out.charAt(i)) || out.charAt(i) == '%')) i++;
            if (i == start) {
                fixed.append(out.charAt(i));
                i++;
                continue;
            }
            String word = out.substring(start, i);
            String lw = word.toLowerCase(Locale.US);
            if (LOWERCASE_WORDS.contains(lw) && start > 0) word = lw;
            fixed.append(word);
        }
        return fixed.toString();
    }

    /**
     * Turns the editor text into receipt items.
     *
     * Rules taken from the build notes:
     *  - one item per line, price appended to the item text in $x.xx form;
     *  - lines starting with "##" are notes/guidelines and are ignored;
     *  - blank lines are ignored;
     *  - a line without a price is treated as a wrapped continuation of the
     *    previous item, and a price-only line completes the item above it.
     */
    public static List<Item> parseEditorText(String text, List<Item> previous) {
        List<Item> out = new ArrayList<Item>();
        if (text == null) return out;
        StringBuilder pending = new StringBuilder();
        for (String raw : text.split("\n", -1)) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("##")) continue; // build notes are context, not items

            double price = 0d;
            boolean hasPrice = false;
            Matcher money = PRICE_AT.matcher(line);
            if (money.find()) {
                price = parseDouble(money.group(1));
                line = line.substring(0, money.start()).trim();
                hasPrice = true;
            } else {
                Matcher bare = BARE_PRICE_AT.matcher(line);
                if (bare.find()) {
                    price = parseDouble(bare.group(1));
                    line = line.substring(0, bare.start()).trim();
                    hasPrice = true;
                }
            }
            line = line.replaceAll("\\s+", " ").trim();

            if (!hasPrice) {
                // description line whose price comes later (wrapped description)
                if (pending.length() > 0) pending.append(' ');
                pending.append(line.toUpperCase(Locale.US));
                continue;
            }

            String desc = pending.length() > 0 ? pending + " " + line : line;
            pending.setLength(0);
            desc = desc.replaceAll("\\s+", " ").trim().toUpperCase(Locale.US);
            if (desc.isEmpty()) continue;
            out.add(new Item(desc, lookupUpc(desc, previous), price));
        }

        String tail = pending.toString().trim();
        if (!tail.isEmpty()) {
            // description without a price: keep it visible so it can be fixed
            out.add(new Item(tail, lookupUpc(tail, previous), 0d));
        }
        return out;
    }

    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0d;
        }
    }

    /** Keeps the UPC of an item that already exists on the receipt. */
    private static String lookupUpc(String desc, List<Item> previous) {
        if (previous == null) return "";
        String key = normalize(desc);
        if (key.isEmpty()) return "";
        for (Item it : previous) {
            if (normalize(it.desc).equals(key)) {
                return it.upc == null ? "" : it.upc;
            }
        }
        return "";
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.toUpperCase(Locale.US).replaceAll("[^A-Z0-9]", "");
    }

    /* --------------------------------------------------------- persistence */

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("version=1\n");
        for (String line : storeLines) sb.append("store=").append(enc(line)).append('\n');
        sb.append("st=").append(enc(stLine)).append('\n');
        sb.append("tc=").append(enc(tcNumber)).append('\n');
        sb.append("visa=").append(enc(visaLast4)).append('\n');
        sb.append("tax1=").append(tax1Rate).append('\n');
        sb.append("tax2=").append(tax2Rate).append('\n');
        sb.append("ts=").append(timestamp).append('\n');
        sb.append("entry=").append(enc(entryText)).append('\n');
        sb.append("draft=").append(enc(draftText)).append('\n');
        for (Map.Entry<String, Integer> entry : committedItems.entrySet()) {
            sb.append("committed=").append(enc(entry.getKey()))
                    .append('|').append(entry.getValue()).append('\n');
        }
        for (Item it : items) {
            sb.append("item=").append(enc(it.desc))
                    .append('|').append(enc(it.upc))
                    .append('|').append(it.price).append('\n');
        }
        return sb.toString();
    }

    public static ReceiptModel deserialize(String data) {
        if (data == null || data.trim().isEmpty()) return sample();
        ReceiptModel r = new ReceiptModel();
        boolean sawItem = false;
        for (String line : data.split("\n")) {
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String key = line.substring(0, eq);
            String val = line.substring(eq + 1);
            if ("store".equals(key)) {
                r.storeLines.add(dec(val));
            } else if ("st".equals(key)) {
                r.stLine = dec(val);
            } else if ("tc".equals(key)) {
                r.tcNumber = dec(val);
            } else if ("visa".equals(key)) {
                r.visaLast4 = dec(val);
            } else if ("tax1".equals(key)) {
                r.tax1Rate = parseDouble(val);
            } else if ("tax2".equals(key)) {
                r.tax2Rate = parseDouble(val);
            } else if ("ts".equals(key)) {
                try {
                    r.timestamp = Long.parseLong(val.trim());
                } catch (NumberFormatException e) {
                    r.timestamp = 0L;
                }
            } else if ("entry".equals(key)) {
                r.entryText = dec(val);
            } else if ("draft".equals(key)) {
                r.draftText = dec(val);
            } else if ("committed".equals(key)) {
                int bar = val.lastIndexOf('|');
                if (bar > 0) {
                    try {
                        r.committedItems.put(dec(val.substring(0, bar)),
                                Integer.parseInt(val.substring(bar + 1).trim()));
                    } catch (NumberFormatException ignored) {
                        // skip a malformed counter
                    }
                }
            } else if ("item".equals(key)) {
                String[] parts = val.split("\\|", -1);
                if (parts.length >= 3) {
                    r.items.add(new Item(dec(parts[0]), dec(parts[1]), parseDouble(parts[2])));
                    sawItem = true;
                }
            }
        }
        if (r.storeLines.isEmpty() || (!sawItem && r.items.isEmpty())) {
            ReceiptModel fallback = sample();
            if (r.storeLines.isEmpty()) r.storeLines.addAll(fallback.storeLines);
            if (r.items.isEmpty()) r.items.addAll(fallback.items);
            if (r.stLine.isEmpty()) r.stLine = fallback.stLine;
            if (r.tcNumber.isEmpty()) r.tcNumber = fallback.tcNumber;
            if (r.visaLast4.isEmpty()) r.visaLast4 = fallback.visaLast4;
            if (r.timestamp <= 0L) r.timestamp = fallback.timestamp;
        }
        return r;
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s == null ? "" : s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    private static String dec(String s) {
        try {
            return URLDecoder.decode(s == null ? "" : s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s == null ? "" : s;
        }
    }
}
