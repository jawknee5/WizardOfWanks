package com.wizardofwanks.receipt;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Item details screen: black text editor with Save / Duplicate / Undo / Redo.
 * Saving applies the edited items to the receipt, recalculates totals and
 * stamps the receipt with the current date and time.
 */
public class ItemDetailsActivity extends Activity {

    private static final int ORANGE = 0xFFE9A13B;
    private static final int MAGENTA = 0xFFFF00CC;
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final Deque<String> undoStack = new ArrayDeque<String>();
    private final Deque<String> redoStack = new ArrayDeque<String>();

    private ReceiptModel receipt;
    private EditText editor;
    private boolean programmatic;

    private final Handler draftHandler = new Handler(Looper.getMainLooper());
    private final Runnable persistDraft = new Runnable() {
        @Override
        public void run() {
            ReceiptStore.get(ItemDetailsActivity.this).save();
        }
    };

    private final TextWatcher watcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (!programmatic) {
                undoStack.push(s.toString());
                while (undoStack.size() > 200) undoStack.removeLast();
                redoStack.clear();
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (receipt == null) return;
            receipt.draftText = s.toString();
            scheduleDraftSave();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(0xFF9A6A16);
        window.setNavigationBarColor(Color.BLACK);
        receipt = ReceiptStore.get(this).getReceipt();
        setContentView(buildUi());
        // Restores whatever was being typed, even if the app was closed before
        // saving; blank only when there is nothing in progress.
        String initial = receipt.draftText != null && !receipt.draftText.isEmpty()
                ? receipt.draftText : receipt.entryText;
        setEditorText(initial);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFE9E9E9);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(ORANGE);
        bar.setPadding(dp(4), dp(8), dp(4), dp(8));

        String[] labels = {"Save", "Duplicate", "Undo", "Redo"};
        View.OnClickListener[] actions = {
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        save();
                    }
                },
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        duplicateLine();
                    }
                },
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        undo();
                    }
                },
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        redo();
                    }
                }
        };

        for (int i = 0; i < labels.length; i++) {
            TextView button = new TextView(this);
            button.setText(labels[i]);
            button.setTextColor(Color.WHITE);
            button.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            button.setGravity(Gravity.CENTER);
            GradientDrawable background = new GradientDrawable();
            background.setColor(MAGENTA);
            background.setStroke(dp(1), 0xFF2A2A2A);
            background.setCornerRadius(dp(2));
            button.setBackground(background);
            button.setOnClickListener(actions[i]);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
            lp.setMargins(dp(4), 0, dp(4), 0);
            bar.addView(button, lp);
        }
        root.addView(bar, new LinearLayout.LayoutParams(MATCH, WRAP));

        LinearLayout editorColumn = new LinearLayout(this);
        editorColumn.setOrientation(LinearLayout.VERTICAL);

        FrameLayout holder = new FrameLayout(this);
        holder.setBackgroundColor(Color.BLACK);

        View stripe = new View(this);
        stripe.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF3E310F, 0xFF8A6D1F, 0xFFB8912A, 0xFF4A3A12}));
        holder.addView(stripe, new FrameLayout.LayoutParams(dp(20), MATCH));

        editor = new EditText(this);
        editor.setTextColor(Color.WHITE);
        editor.setHintTextColor(0xFF9A9A9A);
        editor.setHint("Item description$price");
        editor.setTypeface(Typeface.MONOSPACE);
        editor.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setBackgroundColor(Color.TRANSPARENT);
        editor.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editor.setPadding(dp(12), dp(12), dp(10), dp(12));
        editor.setVerticalScrollBarEnabled(true);
        FrameLayout.LayoutParams editorParams = new FrameLayout.LayoutParams(MATCH, MATCH);
        editorParams.leftMargin = dp(20);
        holder.addView(editor, editorParams);
        editor.addTextChangedListener(watcher);

        editorColumn.addView(holder, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        View bottomBorder = new View(this);
        bottomBorder.setBackgroundColor(ORANGE);
        editorColumn.addView(bottomBorder, new LinearLayout.LayoutParams(MATCH, dp(3)));

        root.addView(editorColumn, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        TextView hint = new TextView(this);
        hint.setText("One item per line as description$price. Lines starting with ## are "
                + "ignored. Save adds the items to the receipt; your list is kept as a draft "
                + "until then.");
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        hint.setTextColor(0xFF6B6B6B);
        hint.setPadding(dp(14), dp(8), dp(14), dp(8));
        root.addView(hint, new LinearLayout.LayoutParams(MATCH, WRAP));

        return root;
    }

    private void setEditorText(String text) {
        programmatic = true;
        editor.setText(text);
        if (editor.getText() != null) editor.setSelection(editor.getText().length());
        programmatic = false;
        if (receipt != null) receipt.draftText = editor.getText().toString();
    }

    private void scheduleDraftSave() {
        draftHandler.removeCallbacks(persistDraft);
        draftHandler.postDelayed(persistDraft, 400);
    }

    @Override
    protected void onPause() {
        super.onPause();
        draftHandler.removeCallbacks(persistDraft);
        if (receipt != null && editor != null) {
            receipt.draftText = editor.getText().toString();
            ReceiptStore.get(this).save();
        }
    }

    @Override
    protected void onDestroy() {
        draftHandler.removeCallbacks(persistDraft);
        super.onDestroy();
    }

    private void undo() {
        if (undoStack.isEmpty()) {
            toast("Nothing to undo");
            return;
        }
        String previous = undoStack.pop();
        redoStack.push(editor.getText().toString());
        setEditorText(previous);
    }

    private void redo() {
        if (redoStack.isEmpty()) {
            toast("Nothing to redo");
            return;
        }
        String next = redoStack.pop();
        undoStack.push(editor.getText().toString());
        setEditorText(next);
    }

    private void duplicateLine() {
        Editable editable = editor.getText();
        if (editable == null) return;
        String text = editable.toString();

        int cursor = Math.max(0, Math.min(editor.getSelectionStart(), text.length()));
        int lineStart = text.lastIndexOf("\n", Math.max(0, cursor - 1)) + 1;
        int lineEnd = text.indexOf("\n", cursor);
        if (lineEnd < 0) lineEnd = text.length();
        String line = text.subSequence(lineStart, lineEnd).toString();

        if (line.trim().isEmpty()) {
            int search = lineStart;
            while (search > 0) {
                int end = search - 1;
                int start = text.lastIndexOf("\n", Math.max(0, end - 1)) + 1;
                String candidate = text.subSequence(start, end).toString();
                if (!candidate.trim().isEmpty()) {
                    line = candidate;
                    lineEnd = end;
                    break;
                }
                if (start == 0) break;
                search = start;
            }
        }

        if (line.trim().isEmpty()) {
            toast("Nothing to duplicate");
            return;
        }

        String insert = "\n" + line;
        editable.insert(lineEnd, insert);
        editor.setSelection(lineEnd + insert.length());
    }

    private void save() {
        String raw = editor.getText().toString();
        int appended = receipt.commitEntry(raw);
        if (appended < 0) {
            toast("Add at least one item, e.g. Great Value Milk 1 gal$3.48");
            return;
        }
        ReceiptStore.get(this).save();
        setResult(RESULT_OK);
        finish();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
