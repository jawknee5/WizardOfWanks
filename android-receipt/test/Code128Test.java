import com.wizardofwanks.receipt.Code128;

/** Structure checks for the Code 128 encoder; prints the module bit string on request. */
public class Code128Test {

    private static final String TC = "0103 1784 2359 0788 9942";

    public static void main(String[] args) {
        int[] widths = Code128.encode(TC);

        // start C (211232) + ten digit pairs + checksum = 12 symbols * 11 modules, + 13 for stop
        check("module count", Code128.modules(widths) == 12 * 11 + 13);
        check("starts with start C", patternAt(widths, 0).equals("211232"));
        // checksum for these digits is symbol 45 (113123)
        check("checksum symbol", patternAt(widths, 6 + 10 * 6).equals("113123"));
        check("ends with stop pattern", endsWith(widths, new int[]{2, 3, 3, 1, 1, 1, 2}));

        int[] odd = Code128.encode("123");
        check("odd digits use code B", patternAt(odd, 0).equals("211214"));
        // '1','2','3' are code B values 17,18,19
        // 104 + 1*17 + 2*18 + 3*19 = 214 -> 214 % 103 = 8 -> pattern 132212
        check("code B checksum symbol", patternAt(odd, 6 + 3 * 6).equals("132212"));

        check("non digits are ignored", Code128.digitsOf("TC# 01-03").equals("0103"));
        check("empty input", Code128.encode("").length == 0);
        // start C + one pair + checksum = 3 symbols (18 widths), plus 7 for stop
        check("two digits", Code128.encode("12").length == 25);

        if (args.length > 0 && "--bits".equals(args[0])) {
            System.out.println(toBits(widths));
        }
        System.out.println("OK - code128 checks passed");
    }

    private static String patternAt(int[] widths, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < start + 6 && i < widths.length; i++) sb.append(widths[i]);
        return sb.toString();
    }

    private static boolean endsWith(int[] widths, int[] tail) {
        if (widths.length < tail.length) return false;
        for (int i = 0; i < tail.length; i++) {
            if (widths[widths.length - tail.length + i] != tail[i]) return false;
        }
        return true;
    }

    private static String toBits(int[] widths) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < widths.length; i++) {
            char bit = i % 2 == 0 ? '1' : '0';
            for (int w = 0; w < widths[i]; w++) sb.append(bit);
        }
        return sb.toString();
    }

    private static void check(String label, boolean condition) {
        if (!condition) throw new AssertionError("FAILED: " + label);
    }
}
