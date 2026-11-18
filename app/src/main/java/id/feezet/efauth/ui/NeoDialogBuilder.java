package id.feezet.efauth.ui;

import android.content.Context;
import android.graphics.drawable.InsetDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import id.feezet.efauth.R;

/** Visual decoration only: never replaces existing validation, listeners or dismissal rules. */
public final class NeoDialogBuilder extends AlertDialog.Builder {
    public NeoDialogBuilder(Context context) { super(context, R.style.EVaultDialog); }
    @Override public AlertDialog create() {
        AlertDialog dialog=super.create();
        View decor=dialog.getWindow().getDecorView();
        decor.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) { view.post(() -> decorate(dialog)); }
            @Override public void onViewDetachedFromWindow(View view) {}
        });
        return dialog;
    }
    @Override public AlertDialog show() { AlertDialog d=create(); d.show(); return d; }
    private static int px(Context c,int dp){return Math.round(dp*c.getResources().getDisplayMetrics().density);}
    private static void decorate(AlertDialog dialog) {
        Context c=dialog.getContext();
        if(dialog.getWindow()==null)return;
        dialog.getWindow().setBackgroundDrawable(new InsetDrawable(new NeoSurface(c,c.getColor(R.color.surface),c.getColor(R.color.border),false,4),px(c,8)));
        for(int which:new int[]{AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_NEUTRAL}){
            Button b=dialog.getButton(which);if(b==null)continue;
            b.setAllCaps(false);b.setMinHeight(px(c,48));b.setMinimumHeight(px(c,48));
            b.setTextColor(c.getColor(R.color.primary));
            b.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }
        styleInputs(dialog.getWindow().getDecorView(),c);
    }
    private static void styleInputs(View view,Context c){
        if(view instanceof EditText){
            EditText input=(EditText)view;
            input.setBackground(new NeoSurface(c,
                    c.getColor(R.color.surface),c.getColor(R.color.border),true,0));
            input.setPadding(px(c,16),px(c,12),px(c,16),px(c,12));
            input.setTextColor(c.getColor(R.color.textPrimary));input.setHintTextColor(c.getColor(R.color.textSecondary));
            input.setMinHeight(px(c,48));
        }
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)styleInputs(group.getChildAt(i),c);}
    }
}
