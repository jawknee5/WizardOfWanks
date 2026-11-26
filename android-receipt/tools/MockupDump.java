import com.wizardofwanks.receipt.ReceiptModel;

/**
 * Dumps the receipt strings and totals the mockup renderer draws, so the
 * mockups always show exactly what ReceiptModel produces.
 *
 *   javac --release 11 -d /tmp/mockdump \
 *       src/main/java/com/wizardofwanks/receipt/ReceiptModel.java tools/MockupDump.java
 *   java -cp /tmp/mockdump MockupDump > /tmp/mockdata.txt
 */
public class MockupDump {

    public static void main(String[] args) {
        ReceiptModel r = ReceiptModel.sample();
        StringBuilder sb = new StringBuilder();
        for (String line : r.storeLines) sb.append("store|").append(line).append('\n');
        sb.append("st|").append(r.stLine).append('\n');
        for (ReceiptModel.Item item : r.items) {
            sb.append("item|").append(item.desc)
                    .append('|').append(item.upc)
                    .append('|').append(ReceiptModel.money(item.price))
                    .append('\n');
        }
        sb.append("subtotal|").append(ReceiptModel.money(r.subtotal())).append('\n');
        sb.append("tax1|").append(ReceiptModel.money(r.tax1())).append('\n');
        sb.append("rate1|").append(ReceiptModel.rateText(r.tax1Rate)).append('\n');
        sb.append("tax2|").append(ReceiptModel.money(r.tax2())).append('\n');
        sb.append("rate2|").append(ReceiptModel.rateText(r.tax2Rate)).append('\n');
        sb.append("total|").append(ReceiptModel.money(r.total())).append('\n');
        sb.append("visa|").append(r.visaLast4).append('\n');
        sb.append("sold|").append(r.itemsSold()).append('\n');
        sb.append("tc|").append(r.tcNumber).append('\n');
        sb.append("ts|").append(r.timestampText()).append('\n');
        for (String line : r.editorText().split("\n")) {
            sb.append("editor|").append(line).append('\n');
        }
        System.out.print(sb);
    }
}
