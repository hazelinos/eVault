package id.feezet.efauth.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import androidx.core.graphics.PathParser;

/** Consistent 24-unit outlined vector icons, kept independent of emoji fonts. */
public final class NeoIcons extends Drawable {
    private final Path path;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    public NeoIcons(Context context, String name, int color) {
        String data;
        switch (name) {
            case "switch": data = "M4,7 L20,7 M15,2 L20,7 L15,12 M20,17 L4,17 M9,12 L4,17 L9,22"; break;
            case "add": data = "M12,4 L12,20 M4,12 L20,12"; break;
            case "save": data = "M4,3 L17,3 L21,7 L21,21 L3,21 L3,3 Z M7,3 L7,9 L16,9 L16,3 M7,21 L7,14 L17,14 L17,21"; break;
            case "play": data = "M8,5 L20,12 L8,19 Z"; break;
            case "auto": data = "M20,8 A9,9 0,0 0,4,8 M4,3 L4,8 L9,8 M4,16 A9,9 0,0 0,20,16 M15,16 L20,16 L20,21"; break;
            case "settings": data = "M4,6 L20,6 M4,12 L20,12 M4,18 L20,18 M8,3 L8,9 M16,9 L16,15 M10,15 L10,21"; break;
            case "log": data = "M9,5 L21,5 M9,12 L21,12 M9,19 L21,19 M3,5 L4,5 M3,12 L4,12 M3,19 L4,19"; break;
            case "back": data = "M20,12 L4,12 M11,5 L4,12 L11,19"; break;
            case "info": data = "M12,3 A9,9 0,1 0,12,21 A9,9 0,1 0,12,3 M12,11 L12,17 M12,7 L12,7.2"; break;
            default: data = "M3,7 L12,2 L21,7 L12,12 Z M3,12 L12,17 L21,12 M3,17 L12,22 L21,17";
        }
        path = PathParser.createPathFromPathData(data);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.2f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        int size = Math.round(20 * context.getResources().getDisplayMetrics().density);
        setBounds(0, 0, size, size);
    }
    @Override public void draw(Canvas canvas) {
        canvas.save();
        canvas.translate(getBounds().left, getBounds().top);
        canvas.scale(getBounds().width() / 24f, getBounds().height() / 24f);
        canvas.drawPath(path, paint);
        canvas.restore();
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
