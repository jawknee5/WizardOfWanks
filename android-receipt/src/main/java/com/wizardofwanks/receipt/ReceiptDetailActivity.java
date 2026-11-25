package com.wizardofwanks.receipt;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Main screen: the receipt image with editable fields and the barcode. */
public class ReceiptDetailActivity extends Activity implements ReceiptView.Listener {

    private static final int REQ_ITEM_EDITOR = 1001;
    private static final int BLUE_BAR = 0xFF0B6BC1;
    private static final int BLUE_DARK = 0xFF0A4E8C;
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private ReceiptModel receipt;
    private ReceiptView receiptView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(BLUE_DARK);
        window.setNavigationBarColor(0xFF000000);
        receipt = ReceiptStore.get(this).getReceipt();
        setContentView(buildUi());
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(BLUE_BAR);
        bar.setPadding(dp(10), 0, dp(14), 0);

        TextView back = new TextView(this);
        back.setText("\u2039");
        back.setTextColor(Color.WHITE);
        back.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34);
        back.setGravity(Gravity.CENTER);
        back.setPadding(dp(6), 0, dp(10), dp(6));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        bar.addView(back, new LinearLayout.LayoutParams(WRAP, MATCH));

        TextView title = new TextView(this);
        title.setText("Receipt Detail");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(title, new LinearLayout.LayoutParams(WRAP, WRAP));

        root.addView(bar, new LinearLayout.LayoutParams(MATCH, dp(58)));

        receiptView = new ReceiptView(this);
        receiptView.setListener(this);
        receiptView.setReceipt(receipt);
        root.addView(receiptView, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        return root;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void persist() {
        ReceiptStore.get(this).save();
        if (receiptView != null) receiptView.setReceipt(receipt);
    }

    /* ------------------------------------------------------ editable fields */

    @Override
    public void onEditStoreBlock() {
        final EditText input = input(receipt.storeBlockText(), true);
        showDialog("Store details", wrap(input), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                receipt.setStoreBlockText(input.getText().toString());
                persist();
            }
        });
    }

    @Override
    public void onEditStatusLine() {
        final EditText input = input(receipt.stLine, false);
        showDialog("Transaction line", wrap(input), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                receipt.stLine = input.getText().toString().trim().toUpperCase();
                persist();
            }
        });
    }

    @Override
    public void onEditItemUpc(final int index) {
        if (index < 0 || index >= receipt.items.size()) return;
        final ReceiptModel.Item item = receipt.items.get(index);
        final EditText input = input(item.upc, false);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        showDialog("Item UPC", wrap(input), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                item.upc = input.getText().toString().replaceAll("[^0-9]", "");
                persist();
            }
        });
    }

    @Override
    public void onEditItem(final int index) {
        if (index < 0 || index >= receipt.items.size()) return;

        final ReceiptModel.Item item = receipt.items.get(index);
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);

        TextView descLabel = fieldLabel("Description");
        final EditText desc = input(item.desc, true);
        TextView priceLabel = fieldLabel("Price");
        final EditText price = input(ReceiptModel.money(item.price), false);
        price.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        TextView upcLabel = fieldLabel("UPC");
        final EditText upc = input(item.upc, false);
        upc.setInputType(InputType.TYPE_CLASS_NUMBER);

        column.addView(descLabel);
        column.addView(desc, new LinearLayout.LayoutParams(MATCH, WRAP));
        column.addView(upcLabel);
        column.addView(upc, new LinearLayout.LayoutParams(MATCH, WRAP));
        column.addView(priceLabel);
        column.addView(price, new LinearLayout.LayoutParams(MATCH, WRAP));

        showDialog("Edit item", wrap(column), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text = desc.getText().toString().trim().toUpperCase();
                if (text.isEmpty()) {
                    Toast.makeText(ReceiptDetailActivity.this, "Description cannot be empty",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                item.desc = text;
                item.upc = upc.getText().toString().replaceAll("[^0-9]", "");
                item.price = parsePrice(price.getText().toString());
                persist();
            }
        });
    }

    @Override
    public void onEditTaxRate(final int which) {
        final boolean first = which == 0;
        double current = first ? receipt.tax1Rate : receipt.tax2Rate;
        final EditText input = input(ReceiptModel.money(current), false);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        showDialog(first ? "Tax 1 rate (%)" : "Tax 12 rate (%)", wrap(input),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        double rate = parsePrice(input.getText().toString());
                        rate = Math.max(0d, Math.min(100d, rate));
                        if (first) {
                            receipt.tax1Rate = rate;
                        } else {
                            receipt.tax2Rate = rate;
                        }
                        persist();
                    }
                });
    }

    @Override
    public void onOpenItemEditor() {
        startActivityForResult(new Intent(this, ItemDetailsActivity.class), REQ_ITEM_EDITOR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_ITEM_EDITOR && resultCode == RESULT_OK) {
            receipt = ReceiptStore.get(this).getReceipt();
            if (receiptView != null) receiptView.setReceipt(receipt);
        }
    }

    /* --------------------------------------------------------------- dialog */

    private TextView fieldLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        label.setTextColor(0xFF666666);
        label.setPadding(dp(6), dp(8), 0, 0);
        return label;
    }

    private EditText input(String value, boolean multiline) {
        EditText edit = new EditText(this);
        edit.setText(value == null ? "" : value);
        edit.setTypeface(Typeface.MONOSPACE);
        edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        edit.setGravity(Gravity.TOP | Gravity.START);
        edit.setMinLines(multiline ? 4 : 1);
        edit.setInputType(multiline
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                : InputType.TYPE_CLASS_TEXT);
        if (edit.getText().length() > 0) edit.setSelection(edit.getText().length());
        return edit;
    }

    private View wrap(View content) {
        FrameLayout frame = new FrameLayout(this);
        frame.setPadding(dp(20), dp(4), dp(20), 0);
        frame.addView(content, new FrameLayout.LayoutParams(MATCH, WRAP));
        return frame;
    }

    private void showDialog(String title, View content, DialogInterface.OnClickListener onSave) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(content)
                .setPositiveButton("Save", onSave)
                .setNegativeButton("Cancel", null)
                .create();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        dialog.show();
    }

    private static double parsePrice(String raw) {
        if (raw == null) return 0d;
        String cleaned = raw.replace("$", "").replace(",", "").trim();
        try {
            return ReceiptModel.round2(Double.parseDouble(cleaned));
        } catch (NumberFormatException e) {
            return 0d;
        }
    }
}
