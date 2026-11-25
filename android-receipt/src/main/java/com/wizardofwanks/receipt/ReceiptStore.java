package com.wizardofwanks.receipt;

import android.content.Context;
import android.content.SharedPreferences;

/** Holds the working receipt and keeps it across launches. */
final class ReceiptStore {

    private static final String PREFS = "receipt_detail";
    private static final String KEY = "receipt";

    private static ReceiptStore instance;

    private final SharedPreferences prefs;
    private ReceiptModel receipt;

    static synchronized ReceiptStore get(Context context) {
        if (instance == null) instance = new ReceiptStore(context.getApplicationContext());
        return instance;
    }

    private ReceiptStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        ReceiptModel loaded = null;
        try {
            String raw = prefs.getString(KEY, null);
            if (raw != null) loaded = ReceiptModel.deserialize(raw);
        } catch (RuntimeException ignored) {
            loaded = null;
        }
        receipt = loaded == null ? ReceiptModel.sample() : loaded;
    }

    ReceiptModel getReceipt() {
        return receipt;
    }

    void save() {
        prefs.edit().putString(KEY, receipt.serialize()).apply();
    }
}
