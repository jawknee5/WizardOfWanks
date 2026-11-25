package com.wizardofwanks.receipt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the receipt and handles the receipt gestures:
 *  - pinch to zoom in, pinch closed to zoom out;
 *  - two finger press/hold and drag to move the whole receipt;
 *  - tap an editable field to edit it, tap the barcode to open the item editor.
 */
public class ReceiptView extends View {

    public interface Listener {
        void onEditStoreBlock();

        void onEditStatusLine();

        void onEditItem(int index);

        void onEditItemUpc(int index);

        void onEditTaxRate(int which);

        void onOpenItemEditor();
    }

    private static final float PAGE_W_DP = 372f;
    private static final float ITEM_LINE_DP = 11.9f;

    private static final int K_TEXT = 0;
    private static final int K_ITEM = 1;
    private static final int K_TOTAL = 2;
    private static final int K_SOLD = 3;
    private static final int K_BARCODE = 4;
    private static final int K_LOGO = 5;

    private static class Row {
        int kind;
        final RectF bounds = new RectF();
        String text;
        int align; // 0 left, 1 center, 2 right
        boolean bold;
        float sizeDp;
        List<String> descLines;
        String upc;
        String price;
        String label;
        String rate;
        String value;
        boolean upcOwnLine;
        RectF upcBounds;
        int taxIndex = -1;
    }

    private static class Hit {
        final RectF rect;
        final String field;
        final int index;

        Hit(RectF rect, String field, int index) {
            this.rect = rect;
            this.field = field;
            this.index = index;
        }
    }

    private final Paint mono = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint monoBold = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Row> rows = new ArrayList<Row>();
    private final List<Hit> hits = new ArrayList<Hit>();

    private ReceiptModel receipt;
    private Listener listener;
    private float density;
    private float contentW;
    private float contentH;

    private float scale = 1f;
    private float minScale = 0.5f;
    private float maxScale = 5f;
    private float tx = 0f;
    private float ty = 0f;
    private boolean viewFitted;
    private ScaleGestureDetector scaleDetector;

    private float downX;
    private float downY;
    private float lastX;
    private float lastY;
    private float centroidX;
    private float centroidY;
    private boolean moved;
    private boolean multiTouch;
    private Hit pressed;

    public ReceiptView(Context context) {
        super(context);
        init(context);
    }

    public ReceiptView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        density = getResources().getDisplayMetrics().density;
        mono.setTypeface(Typeface.MONOSPACE);
        monoBold.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                float next = Math.max(minScale, Math.min(maxScale, scale * factor));
                float applied = next / scale;
                tx = detector.getFocusX() - (detector.getFocusX() - tx) * applied;
                ty = detector.getFocusY() - (detector.getFocusY() - ty) * applied;
                scale = next;
                clampTranslation();
                invalidate();
                return true;
            }
        });
        setBackgroundColor(0xFFFFFFFF);
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void setReceipt(ReceiptModel receipt) {
        this.receipt = receipt;
        pressed = null;
        invalidate();
    }

    private float dp(float value) {
        return value * density;
    }

    /* -------------------------------------------------------------- layout */

    private void buildLayout() {
        rows.clear();
        hits.clear();
        if (receipt == null) {
            contentW = dp(PAGE_W_DP);
            contentH = dp(600);
            return;
        }

        contentW = dp(PAGE_W_DP);
        float w = contentW;
        float lh = dp(ITEM_LINE_DP);
        float y = dp(4);

        Row logo = new Row();
        logo.kind = K_LOGO;
        logo.bounds.set(0, y, w, y + dp(102));
        rows.add(logo);
        y += dp(102);

        float storeTop = y;
        for (String line : receipt.storeLines) {
            addTextRow(K_TEXT, y, lh, w, line, 1, false, 9.6f);
            y += lh;
        }
        if (y > storeTop) {
            hits.add(new Hit(new RectF(dp(4), storeTop, w - dp(4), y), "store", -1));
        }

        Row st = addTextRow(K_TEXT, y, lh, w, receipt.stLine, 1, false, 9.6f);
        y += lh;
        hits.add(new Hit(new RectF(dp(4), st.bounds.top, w - dp(4), st.bounds.bottom), "st", -1));

        y += dp(14);

        for (int i = 0; i < receipt.items.size(); i++) {
            y = addItemRow(i, y, w, lh);
        }

        y += dp(16);

        y = addTotalRow(y, w, lh, "SUBTOTAL", null, -1, ReceiptModel.money(receipt.subtotal()));
        y = addTotalRow(y, w, lh, "TAX  1", ReceiptModel.rateText(receipt.tax1Rate), 0,
                ReceiptModel.money(receipt.tax1()));
        y = addTotalRow(y, w, lh, "TAX 12", ReceiptModel.rateText(receipt.tax2Rate), 1,
                ReceiptModel.money(receipt.tax2()));
        y = addTotalRow(y, w, lh, "TOTAL", null, -1, ReceiptModel.money(receipt.total()));
        y = addTotalRow(y, w, lh, "VISA CREDIT TEND", null, -1, ReceiptModel.money(receipt.total()));
        y = addTotalRow(y, w, lh, "VISA  **** **** **** " + receipt.visaLast4, null, -1, "");
        y = addTotalRow(y, w, lh, "CHANGE DUE", null, -1, "0.00");

        y += dp(12);
        addTextRow(K_SOLD, y, dp(28), w, "# ITEMS SOLD " + receipt.itemsSold(), 1, true, 16f);
        y += dp(28);

        y += dp(6);
        Row tc = addTextRow(K_TEXT, y, lh, w, "TC# " + receipt.tcNumber, 1, false, 9.6f);
        y += lh;

        Row barcode = new Row();
        barcode.kind = K_BARCODE;
        barcode.bounds.set(w * 0.04f, y + dp(3), w * 0.96f, y + dp(3) + dp(62));
        rows.add(barcode);
        hits.add(new Hit(new RectF(barcode.bounds.left - dp(8), tc.bounds.top,
                barcode.bounds.right + dp(8), barcode.bounds.bottom + dp(4)), "barcode", -1));
        y = barcode.bounds.bottom;

        y += dp(10);
        addTextRow(K_TEXT, y, lh, w, receipt.timestampText(), 1, false, 9.6f);
        y += lh;

        contentH = y + dp(24);
    }

    private Row addTextRow(int kind, float y, float h, float w, String text, int align,
                           boolean bold, float sizeDp) {
        Row r = new Row();
        r.kind = kind;
        r.bounds.set(0, y, w, y + h);
        r.text = text == null ? "" : text;
        r.align = align;
        r.bold = bold;
        r.sizeDp = sizeDp;
        rows.add(r);
        return r;
    }

    private float addItemRow(int index, float y, float w, float lh) {
        ReceiptModel.Item item = receipt.items.get(index);
        mono.setTextSize(dp(9.6f));
        List<String> lines = wrap(item.desc, mono, w * 0.68f);

        boolean upcOwnLine = false;
        float upcX = 0f;
        if (item.upc != null && !item.upc.isEmpty()) {
            float upcWidth = mono.measureText(item.upc);
            upcX = w * 0.80f - upcWidth;
            float firstLine = mono.measureText(lines.get(0));
            if (upcX < firstLine + dp(5)) upcOwnLine = true;
        }

        int lineCount = lines.size() + (upcOwnLine ? 1 : 0);
        Row r = new Row();
        r.kind = K_ITEM;
        r.bounds.set(0, y, w, y + lineCount * lh);
        r.descLines = lines;
        r.upc = item.upc;
        r.price = ReceiptModel.money(item.price);
        r.upcOwnLine = upcOwnLine;
        r.sizeDp = 9.6f;
        rows.add(r);

        if (item.upc != null && !item.upc.isEmpty()) {
            float upcTop = upcOwnLine ? y + lines.size() * lh : y;
            float upcWidth = mono.measureText(item.upc);
            r.upcBounds = new RectF(upcX - dp(10), upcTop - dp(3), upcX + upcWidth + dp(8), upcTop + lh + dp(3));
            hits.add(new Hit(r.upcBounds, "upc", index));
        }
        hits.add(new Hit(new RectF(dp(3), y, w - dp(3), y + lineCount * lh), "item", index));

        return y + lineCount * lh;
    }

    private float addTotalRow(float y, float w, float lh, String label, String rate, int taxIndex,
                              String value) {
        Row r = new Row();
        r.kind = K_TOTAL;
        r.bounds.set(0, y, w, y + lh);
        r.label = label;
        r.rate = rate;
        r.value = value;
        r.taxIndex = taxIndex;
        r.sizeDp = 9.6f;
        rows.add(r);
        if (rate != null) {
            mono.setTextSize(dp(9.6f));
            // generous tap target for the small rate text
            float rateX = w * 0.605f;
            float rateWidth = mono.measureText(rate);
            hits.add(new Hit(new RectF(rateX - dp(18), y - dp(4),
                    rateX + rateWidth + dp(18), y + lh + dp(4)), "tax", taxIndex));
        }
        return y + lh;
    }

    private List<String> wrap(String text, Paint paint, float maxWidth) {
        List<String> out = new ArrayList<String>();
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) {
            out.add("");
            return out;
        }
        StringBuilder line = new StringBuilder();
        for (String word : value.split(" ")) {
            if (word.isEmpty()) continue;
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (line.length() == 0 || paint.measureText(candidate) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
            } else {
                out.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (line.length() > 0) out.add(line.toString());
        if (out.isEmpty()) out.add("");
        return out;
    }

    /* --------------------------------------------------------------- paint */

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xFFFFFFFF);
        if (receipt == null) return;
        buildLayout();
        canvas.save();
        canvas.translate(tx, ty);
        canvas.scale(scale, scale);
        for (Row row : rows) {
            drawRow(canvas, row);
        }
        if (pressed != null) {
            drawPressed(canvas, pressed.rect);
        }
        canvas.restore();
    }

    private void drawRow(Canvas canvas, Row row) {
        switch (row.kind) {
            case K_LOGO:
                drawLogo(canvas, row.bounds);
                break;
            case K_TEXT:
                drawText(canvas, row, 0xFF151515);
                break;
            case K_SOLD:
                drawText(canvas, row, 0xFF101010);
                break;
            case K_ITEM:
                drawItem(canvas, row);
                break;
            case K_TOTAL:
                drawTotal(canvas, row);
                break;
            case K_BARCODE:
                drawBarcode(canvas, row.bounds);
                break;
            default:
                break;
        }
    }

    private void drawText(Canvas canvas, Row row, int color) {
        Paint p = row.bold ? monoBold : mono;
        p.setTextSize(dp(row.sizeDp));
        p.setLetterSpacing(row.kind == K_SOLD ? 0.08f : 0f);
        p.setColor(color);
        float baseline = row.bounds.centerY() - (p.ascent() + p.descent()) / 2f;
        float x;
        if (row.align == 1) {
            p.setTextAlign(Paint.Align.CENTER);
            x = row.bounds.centerX();
        } else if (row.align == 2) {
            p.setTextAlign(Paint.Align.RIGHT);
            x = row.bounds.right;
        } else {
            p.setTextAlign(Paint.Align.LEFT);
            x = row.bounds.left;
        }
        canvas.drawText(row.text, x, baseline, p);
    }

    private void drawItem(Canvas canvas, Row row) {
        Paint p = mono;
        p.setTextSize(dp(row.sizeDp));
        p.setLetterSpacing(0f);
        p.setColor(0xFF141414);
        p.setTextAlign(Paint.Align.LEFT);
        float baseline = row.bounds.top - p.ascent() + dp(1);
        float lineStep = dp(ITEM_LINE_DP);
        for (int i = 0; i < row.descLines.size(); i++) {
            canvas.drawText(row.descLines.get(i), dp(2), baseline + i * lineStep, p);
        }
        if (row.upc != null && !row.upc.isEmpty()) {
            float line = row.upcOwnLine ? row.descLines.size() : 0;
            float x = contentW * 0.80f - p.measureText(row.upc);
            canvas.drawText(row.upc, x, baseline + line * lineStep, p);
        }
        if (row.price != null && row.price.length() > 0) {
            p.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(row.price, contentW - dp(2), baseline, p);
            p.setTextAlign(Paint.Align.LEFT);
        }
    }

    private void drawTotal(Canvas canvas, Row row) {
        Paint p = mono;
        p.setTextSize(dp(row.sizeDp));
        p.setLetterSpacing(0f);
        p.setColor(0xFF141414);
        float baseline = row.bounds.centerY() - (p.ascent() + p.descent()) / 2f;
        p.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(row.label, row.rate == null ? contentW * 0.80f : contentW * 0.60f, baseline, p);
        if (row.rate != null) {
            p.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(row.rate, contentW * 0.605f, baseline, p);
        }
        if (row.value != null && row.value.length() > 0) {
            p.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(row.value, contentW - dp(2), baseline, p);
        }
    }

    /** Draws a real Code 128 symbol: bars at even widths, spaces at odd ones. */
    private void drawBarcode(Canvas canvas, RectF bounds) {
        int[] widths = Code128.encode(receipt.tcNumber);
        if (widths.length == 0) return;

        float total = Code128.modules(widths);
        float module = bounds.width() / total;
        float x = bounds.left;
        Paint p = new Paint();
        p.setColor(0xFF111111);
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < widths.length; i++) {
            float w = widths[i] * module;
            if (i % 2 == 0) {
                canvas.drawRect(x, bounds.top, x + w, bounds.bottom, p);
            }
            x += w;
        }
    }

    private void drawLogo(Canvas canvas, RectF bounds) {
        Paint word = new Paint(Paint.ANTI_ALIAS_FLAG);
        word.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        word.setTextSize(dp(42f));
        word.setColor(0xFF0C0C0C);
        word.setTextAlign(Paint.Align.LEFT);
        word.setLetterSpacing(-0.02f);
        String text = "Walmart";
        float wordWidth = word.measureText(text);
        float sparkRadius = dp(15f);
        float gap = dp(7f);
        float totalWidth = wordWidth + gap + sparkRadius * 2f;
        float left = bounds.centerX() - totalWidth / 2f;
        float baseline = bounds.top + dp(60f);
        canvas.drawText(text, left, baseline, word);
        drawSpark(canvas, left + wordWidth + gap + sparkRadius, baseline - dp(15f), sparkRadius);

        Paint slogan = new Paint(Paint.ANTI_ALIAS_FLAG);
        slogan.setTypeface(Typeface.create("sans-serif", Typeface.BOLD_ITALIC));
        slogan.setTextSize(dp(11.5f));
        slogan.setColor(0xFF101010);
        slogan.setTextAlign(Paint.Align.CENTER);
        slogan.setLetterSpacing(0.01f);
        // Slogan sits fully inside the logo block, so it is never clipped.
        canvas.drawText("Save more. Live better.", bounds.centerX(), baseline + dp(23f), slogan);
    }

    private void drawSpark(Canvas canvas, float cx, float cy, float radius) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF0C0C0C);
        p.setStrokeWidth(dp(4.6f));
        p.setStrokeCap(Paint.Cap.ROUND);
        float inner = radius * 0.26f;
        float outer = radius * 0.95f;
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(-90 + i * 60);
            canvas.drawLine(
                    cx + (float) Math.cos(angle) * inner,
                    cy + (float) Math.sin(angle) * inner,
                    cx + (float) Math.cos(angle) * outer,
                    cy + (float) Math.sin(angle) * outer,
                    p);
        }
    }

    private void drawPressed(Canvas canvas, RectF rect) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(1.6f));
        p.setColor(0xFFF0A226);
        p.setPathEffect(new DashPathEffect(new float[]{dp(4f), dp(3f)}, 0f));
        RectF r = new RectF(rect);
        r.inset(-dp(2), -dp(2));
        canvas.drawRect(r, p);
    }

    /* -------------------------------------------------------------- gestures */

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (receipt == null) return false;
        scaleDetector.onTouchEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                lastX = downX;
                lastY = downY;
                moved = false;
                multiTouch = false;
                pressed = findHit(toContentX(downX), toContentY(downY));
                invalidate();
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (e.getPointerCount() >= 2) {
                    multiTouch = true;
                    centroidX = pointerAverage(e, true);
                    centroidY = pointerAverage(e, false);
                    pressed = null;
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (e.getPointerCount() >= 2) {
                    float cx = pointerAverage(e, true);
                    float cy = pointerAverage(e, false);
                    if (!scaleDetector.isInProgress()) {
                        tx += cx - centroidX;
                        ty += cy - centroidY;
                        clampTranslation();
                        invalidate();
                    }
                    centroidX = cx;
                    centroidY = cy;
                    moved = true;
                    pressed = null;
                } else if (!multiTouch && !scaleDetector.isInProgress()) {
                    float dx = e.getX() - lastX;
                    float dy = e.getY() - lastY;
                    if (moved || Math.hypot(e.getX() - downX, e.getY() - downY) > dp(7f)) {
                        moved = true;
                        pressed = null;
                        tx += dx;
                        ty += dy;
                        clampTranslation();
                        invalidate();
                    }
                    lastX = e.getX();
                    lastY = e.getY();
                }
                break;

            case MotionEvent.ACTION_UP:
                if (!moved && !multiTouch && pressed != null) {
                    Hit hit = pressed;
                    pressed = null;
                    invalidate();
                    dispatch(hit);
                } else {
                    pressed = null;
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                pressed = null;
                invalidate();
                break;

            default:
                break;
        }
        return true;
    }

    private float pointerAverage(MotionEvent e, boolean xAxis) {
        float sum = 0f;
        for (int i = 0; i < e.getPointerCount(); i++) {
            sum += xAxis ? e.getX(i) : e.getY(i);
        }
        return sum / Math.max(1, e.getPointerCount());
    }

    private float toContentX(float x) {
        return (x - tx) / scale;
    }

    private float toContentY(float y) {
        return (y - ty) / scale;
    }

    private Hit findHit(float x, float y) {
        Hit best = null;
        float bestArea = Float.MAX_VALUE;
        for (Hit hit : hits) {
            if (hit.rect.contains(x, y)) {
                float area = hit.rect.width() * hit.rect.height();
                if (area < bestArea) {
                    bestArea = area;
                    best = hit;
                }
            }
        }
        return best;
    }

    private void dispatch(Hit hit) {
        if (listener == null) return;
        if ("store".equals(hit.field)) {
            listener.onEditStoreBlock();
        } else if ("st".equals(hit.field)) {
            listener.onEditStatusLine();
        } else if ("upc".equals(hit.field)) {
            listener.onEditItemUpc(hit.index);
        } else if ("item".equals(hit.field)) {
            listener.onEditItem(hit.index);
        } else if ("tax".equals(hit.field)) {
            listener.onEditTaxRate(hit.index);
        } else if ("barcode".equals(hit.field)) {
            listener.onOpenItemEditor();
        }
    }

    private void clampTranslation() {
        float scaledW = contentW * scale;
        float scaledH = contentH * scale;
        if (scaledW <= getWidth()) {
            tx = (getWidth() - scaledW) / 2f;
        } else {
            tx = Math.min(dp(48), Math.max(getWidth() - scaledW - dp(48), tx));
        }
        if (scaledH <= getHeight()) {
            ty = (getHeight() - scaledH) / 2f;
        } else {
            ty = Math.min(dp(48), Math.max(getHeight() - scaledH - dp(48), ty));
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        buildLayout();
        if (!viewFitted && w > 0) {
            viewFitted = true;
            float fit = (w - dp(6)) / contentW;
            scale = Math.min(fit, 1.6f);
            minScale = Math.max(0.35f, scale * 0.7f);
            maxScale = Math.max(4f, scale * 4f);
            tx = (w - contentW * scale) / 2f;
            ty = 0f;
        }
        clampTranslation();
    }
}
