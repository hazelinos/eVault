package id.feezet.efauth.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/** Home has no ScrollView. Two columns on short/wide windows; compact portrait otherwise. */
public final class HomeLayout extends ViewGroup {
    public HomeLayout(Context context) { super(context); setClipToPadding(false); }
    private int px(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }
    private boolean wide;
    private int pad, gap, header, hero, label, auth, action, status;
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec), h = MeasureSpec.getSize(heightSpec);
        wide = w > h * 1.25f;
        boolean compact = h < px(600);
        pad = px(compact ? 8 : 16);
        gap = px(compact ? 4 : 8);
        header = px(compact ? 32 : 40);
        label = compact ? 0 : px(24);
        auth = px(compact ? 64 : 80);
        action = px(compact ? 48 : 56);
        status = px(24);
        int innerW = w - 2 * pad;
        if (wide) {
            int col = (innerW - px(16)) / 2;
            hero = h - 2 * pad - header - status - 2 * gap;
            int available = h - 2 * pad;
            label = px(24);
            auth = Math.max(px(56), available - label - 3 * action - 4 * gap);
            for (int i=0;i<8;i++) {
                int height = i==0 ? header : i==1 ? hero : i==2 ? label : i==3 ? auth : i==7 ? status : action;
                getChildAt(i).measure(exact(col), exact(Math.max(0,height)));
            }
        } else {
            // Seven gaps; collapsed section label does not consume a gap.
            int gaps = label == 0 ? 6 : 7;
            hero = h - 2 * pad - header - label - auth - 3 * action - status - gaps * gap;
            for (int i=0;i<8;i++) {
                int height = i==0 ? header : i==1 ? hero : i==2 ? label : i==3 ? auth : i==7 ? status : action;
                getChildAt(i).measure(exact(innerW), exact(Math.max(0,height)));
            }
        }
        setMeasuredDimension(w,h);
    }
    private int exact(int size) { return MeasureSpec.makeMeasureSpec(Math.max(0,size), MeasureSpec.EXACTLY); }
    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (wide) {
            int col = getChildAt(0).getMeasuredWidth();
            place(0,pad,pad); place(1,pad,pad+header+gap); place(7,pad,pad+header+hero+2*gap);
            int x=pad+col+px(16), y=pad;
            for(int i=2;i<=6;i++){ place(i,x,y); y+=getChildAt(i).getMeasuredHeight()+gap; }
        } else {
            int y=pad;
            for(int i=0;i<8;i++) {
                place(i,pad,y);
                y+=getChildAt(i).getMeasuredHeight();
                if(i<7 && (i!=2 || label>0)) y+=gap;
            }
        }
    }
    private void place(int index,int x,int y) {
        View v=getChildAt(index);v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());
    }
    @Override protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(-1,-1); }
}
