package id.feezet.efauth;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import id.feezet.efauth.ui.HomeLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class NeoUiTest {
    @Test public void homeFitsSmallPortraitAndLandscapeWithoutScroll() throws Exception {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                HomeLayout home=activity.findViewById(android.R.id.content).findViewWithTag("home");
                assertNotNull(home);
                assertNoScroll(home);
                float density=activity.getResources().getDisplayMetrics().density;
                for(int[] size:new int[][]{{320,480},{360,640},{412,800},{640,320}}){
                    int w=Math.round(size[0]*density),h=Math.round(size[1]*density);
                    home.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));
                    home.layout(0,0,w,h);
                    assertEquals(8,countButtons(home));
                    assertButtonsFit(home,Math.round(48*density));
                    for(int i=0;i<home.getChildCount();i++){
                        View child=home.getChildAt(i);
                        assertTrue(child.getTop()>=0);
                        assertTrue(child.getBottom()<=h);
                    }
                }
                home.requestLayout();
            });
            screenshot("home");
        }
    }
    @Test public void setupCanBeReopenedAndBackReturnsHome() throws Exception {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> click(activity,"Pengaturan"));
            screenshot("settings");
            for(int i=0;i<2;i++){
                scenario.onActivity(activity -> click(activity,"Setup  ›"));
                scenario.onActivity(activity -> {
                    View setup=activity.findViewById(android.R.id.content).findViewWithTag("setup");
                    assertTrue(setup.isShown());
                    assertNotNull(findButton(setup,"Pilih AUTH Library"));
                    assertNotNull(findButton(setup,"Pilih folder AUTH"));
                    assertNotNull(findButton(setup,"Scan & pilih clone"));
                });
                screenshot("setup");
                scenario.onActivity(activity -> click(activity,"← Pengaturan"));
            }
            scenario.onActivity(activity -> click(activity,"Kembali"));
            scenario.onActivity(activity -> assertTrue(activity.findViewById(android.R.id.content).findViewWithTag("home").isShown()));
        }
    }
    private static void click(MainActivity activity,String text){
        Button button=findButton(activity.findViewById(android.R.id.content),text);
        assertNotNull("Missing visible action: "+text,button);button.performClick();
    }
    private static Button findButton(View view,String text){
        if(!view.isShown())return null;
        if(view instanceof Button && ((Button)view).getText().toString().startsWith(text))return (Button)view;
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++){Button b=findButton(g.getChildAt(i),text);if(b!=null)return b;}}
        return null;
    }
    private static void assertNoScroll(View v){
        assertFalse(v instanceof ScrollView);
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)assertNoScroll(g.getChildAt(i));}
    }
    private static int countButtons(View v){int n=v instanceof Button?1:0;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)n+=countButtons(g.getChildAt(i));}return n;}
    private static void assertButtonsFit(ViewGroup group,int minimum){
        for(int i=0;i<group.getChildCount();i++){
            View v=group.getChildAt(i);
            if(v instanceof Button){
                assertTrue("Short touch target: "+((Button)v).getText(),v.getHeight()>=minimum);
                assertTrue("Action clipped vertically: "+((Button)v).getText(),v.getTop()>=0 && v.getBottom()<=group.getHeight());
                assertTrue("Action clipped horizontally",v.getLeft()>=0 && v.getRight()<=group.getWidth());
            }
            if(v instanceof ViewGroup)assertButtonsFit((ViewGroup)v,minimum);
        }
    }
    private static void screenshot(String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        // Wait for the brief navigation fade before recording visual evidence.
        android.os.SystemClock.sleep(200);
        Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        File folder=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),"ui-screenshots");
        assertTrue(folder.exists() || folder.mkdirs());
        try(FileOutputStream out=new FileOutputStream(new File(folder,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}
        bitmap.recycle();
    }
}
