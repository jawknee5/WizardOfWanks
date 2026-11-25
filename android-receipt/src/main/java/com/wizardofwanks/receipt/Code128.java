package com.wizardofwanks.receipt;

import java.util.ArrayList;
import java.util.List;

/**
 * Code 128 encoder for the receipt barcode.
 *
 * Symbol patterns are the standard Code 128 table (values 0-106, taken from the
 * ZXing reference table); the trailing checksum is the symbology's modulo-103
 * weighted sum, so the result is a real, scannable Code 128 symbol.
 *
 * Numeric data with an even number of digits is encoded in code set C (two
 * digits per symbol, the densest form); anything else falls back to code set B.
 */
public final class Code128 {

    private static final String[] PATTERNS = {
            "212222", "222122", "222221", "121223", "121322", // 0-4
            "131222", "122213", "122312", "132212", "221213", // 5-9
            "221312", "231212", "112232", "122132", "122231", // 10-14
            "113222", "123122", "123221", "223211", "221132", // 15-19
            "221231", "213212", "223112", "312131", "311222", // 20-24
            "321122", "321221", "312212", "322112", "322211", // 25-29
            "212123", "212321", "232121", "111323", "131123", // 30-34
            "131321", "112313", "132113", "132311", "211313", // 35-39
            "231113", "231311", "112133", "112331", "132131", // 40-44
            "113123", "113321", "133121", "313121", "211331", // 45-49
            "231131", "213113", "213311", "213131", "311123", // 50-54
            "311321", "331121", "312113", "312311", "332111", // 55-59
            "314111", "221411", "431111", "111224", "111422", // 60-64
            "121124", "121421", "141122", "141221", "112214", // 65-69
            "112412", "122114", "122411", "142112", "142211", // 70-74
            "241211", "221114", "413111", "241112", "134111", // 75-79
            "111242", "121142", "121241", "114212", "124112", // 80-84
            "124211", "411212", "421112", "421211", "212141", // 85-89
            "214121", "412121", "111143", "111341", "131141", // 90-94
            "114113", "114311", "411113", "411311", "113141", // 95-99
            "114131", "311141", "411131", "211412", "211214", // 100-104
            "211232",                                          // 105 start C
            "2331112"                                          // 106 stop
    };

    private static final int START_B = 104;
    private static final int START_C = 105;
    private static final int STOP = 106;

    private Code128() {
    }

    /** Digits only, as printed under TC# on the receipt. */
    public static String digitsOf(String raw) {
        return raw == null ? "" : raw.replaceAll("[^0-9]", "");
    }

    /**
     * Encodes the digits of {@code raw}.
     *
     * @return alternating bar/space widths in modules, starting with a bar, or an
     * empty array when there are no digits
     */
    public static int[] encode(String raw) {
        String digits = digitsOf(raw);
        if (digits.isEmpty()) return new int[0];
        if (digits.length() % 2 == 0) return buildCodeC(digits);
        return buildCodeB(digits);
    }

    /** Total width of an encoded barcode in modules. */
    public static int modules(int[] widths) {
        int total = 0;
        for (int width : widths) total += width;
        return total;
    }

    private static int[] buildCodeC(String digits) {
        List<Integer> values = new ArrayList<Integer>();
        values.add(START_C);
        for (int i = 0; i < digits.length(); i += 2) {
            values.add(Integer.parseInt(digits.substring(i, i + 2)));
        }
        return toWidths(values);
    }

    private static int[] buildCodeB(String digits) {
        List<Integer> values = new ArrayList<Integer>();
        values.add(START_B);
        for (int i = 0; i < digits.length(); i++) {
            values.add(digits.charAt(i) - 32);
        }
        return toWidths(values);
    }

    private static int[] toWidths(List<Integer> values) {
        int checksum = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            checksum += values.get(i) * i;
        }
        values.add(checksum % 103);
        values.add(STOP);

        List<Integer> widths = new ArrayList<Integer>();
        for (int value : values) {
            String pattern = PATTERNS[value];
            for (int i = 0; i < pattern.length(); i++) {
                widths.add(pattern.charAt(i) - '0');
            }
        }
        int[] out = new int[widths.size()];
        for (int i = 0; i < out.length; i++) out[i] = widths.get(i);
        return out;
    }
}
