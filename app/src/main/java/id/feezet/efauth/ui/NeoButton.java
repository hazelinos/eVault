package id.feezet.efauth.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.content.res.ColorStateList;
import android.view.Gravity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.widget.TextViewCompat;
import android.util.TypedValue;
import id.feezet.efauth.R;

/** Accessible native Button, with a physical press into its hard shadow. */
public final class NeoButton extends AppCompatButton {
    public NeoButton(Context context) {
        super(context);
        setAllCaps(false);
        setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        setGravity(Gravity.CENTER);
        setStateListAnimator(null);
        setMinWidth(0);
        setMinimumWidth(0);
        setMinHeight(px(48));
        setMinimumHeight(px(48));
        setPadding(px(8), px(4), px(12), px(8));
        setMaxLines(2);
        setTextSize(14);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this, 11, 16, 1, TypedValue.COMPLEX_UNIT_SP);
    }
    private int px(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    public void setTone(int background, int foreground) {
        setSupportBackgroundTintList(null);
        setBackground(new NeoSurface(getContext(),
                background, getContext().getColor(R.color.border), true, 4));
        setTextColor(new ColorStateList(new int[][] {new int[] {-android.R.attr.state_enabled}, new int[] {}},
                new int[] {getContext().getColor(R.color.textSecondary), foreground}));
    }
    @Override protected void onDraw(Canvas canvas) {
        canvas.save();
        if (isPressed()) canvas.translate(px(4), px(4));
        super.onDraw(canvas);
        canvas.restore();
    }
}
