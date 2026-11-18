package id.feezet.efauth.ui;

import android.content.Context;
import id.feezet.efauth.R;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** One native drawable for cards, inputs, dialogs and button state feedback. */
public final class NeoSurface extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;
    private final float strokeWidth;
    private final float radius;
    private final int fill;
    private final int border;
    private final boolean interactive;
    private final float shadow;

    public NeoSurface(Context context, int fill, int border, boolean interactive, int shadowDp) {
        this.density = context.getResources().getDisplayMetrics().density;
        this.strokeWidth = context.getResources().getDimension(R.dimen.neo_border);
        this.radius = context.getResources().getDimension(R.dimen.neo_radius);
        this.fill = fill;
        this.border = border;
        this.interactive = interactive;
        this.shadow = shadowDp == 0 ? 0 : context.getResources().getDimension(R.dimen.neo_shadow);
    }

    @Override public boolean isStateful() { return interactive; }
    @Override protected boolean onStateChange(int[] states) { invalidateSelf(); return true; }
    private boolean hasState(int state) {
        for (int value : getState()) if (value == state) return true;
        return false;
    }
    @Override public void draw(Canvas canvas) {
        float stroke = strokeWidth;
        boolean pressed = interactive && hasState(android.R.attr.state_pressed);
        boolean disabled = interactive && !hasState(android.R.attr.state_enabled);
        float shift = pressed ? shadow : 0;
        rect.set(getBounds().left + stroke / 2, getBounds().top + stroke / 2,
                getBounds().right - shadow - stroke / 2, getBounds().bottom - shadow - stroke / 2);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(border);
        canvas.save();
        canvas.translate(shadow, shadow);
        canvas.drawRoundRect(rect, radius, radius, paint);
        canvas.restore();
        canvas.save();
        canvas.translate(shift, shift);
        paint.setColor(disabled ? 0xFFE2E8F0 : fill);
        canvas.drawRoundRect(rect, radius, radius, paint);
        if (interactive && hasState(android.R.attr.state_focused) && !pressed) {
            paint.setColor(0x302563EB);
            canvas.drawRoundRect(rect, radius, radius, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setColor(border);
        canvas.drawRoundRect(rect, radius, radius, paint);
        canvas.restore();
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
