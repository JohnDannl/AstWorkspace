package me.toptas.fancyshowcasesample;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.OnClick;
import me.toptas.fancyshowcase.FancyShowCaseView;
import me.toptas.fancyshowcase.FocusShape;
import me.toptas.fancyshowcase.OnViewInflateListener;

public class MainActivity extends BaseActivity {

    FancyShowCaseView mFancyShowCaseView;
    Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        addViewToWindow();
        final View view = findViewById(R.id.btn_focus);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new FancyShowCaseView.Builder(MainActivity.this)
                        .focusOn(view)
                        .title("Focus on View")
                        .build()
                        .show();
            }
        }, 1000);

    }

    private void addViewToWindow() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        View view = new View(this);
        view.setBackgroundColor(0x33ff0000);
        view.setLayoutParams(new ViewGroup.LayoutParams(dm.widthPixels, dm.heightPixels));
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "click", Toast.LENGTH_SHORT).show();
            }
        });
        WindowManager mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        final WindowManager.LayoutParams lp = new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_APPLICATION, WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.RGBA_8888);
        mWindowManager.addView(view, lp);
    }

    /**
     * Shows a simple FancyShowCaseView
     */
    @OnClick(R.id.btn_simple)
    public void simple() {
        new FancyShowCaseView.Builder(this)
                .title("No Focus")
                .build()
                .show();
    }

    /**
     * Shows a FancyShowCaseView that focus on a view
     *
     * @param view View to focus
     */
    @OnClick(R.id.btn_focus)
    public void focusView(View view) {
        new FancyShowCaseView.Builder(this)
                .focusOn(view)
                .title("Focus on View")
                .build()
                .show();
    }

    /**
     * Shows a FancyShowCaseView with rounded rect focus shape
     *
     * @param view View to focus
     */
    @OnClick(R.id.btn_rounded_rect)
    public void focusRoundedRect(View view) {
        new FancyShowCaseView.Builder(this)
                .focusOn(view)
                .focusShape(FocusShape.ROUNDED_RECTANGLE)
                .title("Focus on View")
                .build()
                .show();
    }

    /**
     * Shows FancyShowCaseView with focusCircleRadiusFactor 1.5 and title gravity
     *
     * @param view View to focus
     */
    @OnClick(R.id.btn_focus2)
    public void focusWithLargerCircle(View view) {
        new FancyShowCaseView.Builder(this)
                .focusOn(view)
                .focusCircleRadiusFactor(1.5)
                .title("Focus on View with larger circle")
                .focusBorderColor(Color.GREEN)
                .titleStyle(0, Gravity.BOTTOM | Gravity.CENTER)
                .build()
                .show();
    }

    /**
     * Shows a FancyShowCaseView that focuses on a larger view
     *
     * @param view View to focus
     */
    @OnClick(R.id.btn_focus_rect_color)
    public void focusRectWithBorderColor(View view) {
        new FancyShowCaseView.Builder(this)
                .focusOn(view)
                .title("Focus on larger view")
                .focusShape(FocusShape.ROUNDED_RECTANGLE)
                .roundRectRadius(50)
                .focusBorderSize(5)
                .focusBorderColor(Color.RED)
                .titleStyle(0, Gravity.TOP)
                .build()
                .show();
    }

    /**
     * Shows a FancyShowCaseView with background color and title style
     *
     * @param view View to focus
     */
    @OnClick(R.id.btn_background_color)
    public void focusWithBackgroundColor(View view) {
        new FancyShowCaseView.Builder(this)
                .focusOn(view)
                .backgroundColor(Color.parseColor("#AAff0000"))
                .title("Background color and title style can be changed")
                .titleStyle(R.style.MyTitleStyle, Gravity.TOP | Gravity.CENTER)
                .build()
                .show();
    }

    /**
     * Shows a FancyShowCaseView with border color
     *
     * @param view View to focus
     */
    @OnClick(R.id.btn_border_color)
    public void focusWithBorderColor(View view) {
        new FancyShowCaseView.Builder(this)
                .focusOn(view)
                .title("Focus border color can be changed")
                .titleStyle(R.style.MyTitleStyle, Gravity.TOP | Gravity.CENTER)
                .focusBorderColor(Color.GREEN)
                .focusBorderSize(10)
                .build()
                .show();
    }

    /**
     * Shows a FancyShowCaseView with custom enter, exit animations
     *
     * @param view View to focus
     */
    @OnClick(R.id.btn_anim)
    public void focusWithCustomAnimation(View view) {
        Animation enterAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_top);
        Animation exitAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_out_bottom);

        final FancyShowCaseView fancyShowCaseView = new FancyShowCaseView.Builder(this)
                .focusOn(view)
                .title("Custom enter and exit animations.")
                .enterAnimation(enterAnimation)
                .exitAnimation(exitAnimation)
                .build();
        fancyShowCaseView.show();
        exitAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                fancyShowCaseView.removeView();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }


    /**
     * Shows a FancyShowCaseView view custom view inflation
     *
     * @param view View to focus
     */
    @OnClick(R.id.btn_custom_view)
    public void focusWithCustomView(View view) {
        mFancyShowCaseView = new FancyShowCaseView.Builder(this)
                .focusOn(view)
                .customView(R.layout.layout_my_custom_view, new OnViewInflateListener() {
                    @Override
                    public void onViewInflated(View view) {
                        view.findViewById(R.id.btn_action_1).setOnClickListener(mClickListener);
                    }
                })
                .closeOnTouch(false)
                .build();
        mFancyShowCaseView.show();


    }

    @OnClick(R.id.btn_queue)
    public void queueMultipleInstances() {
        startActivity(new Intent(this, QueueActivity.class));
    }

    View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mFancyShowCaseView.hide();
        }
    };

    @OnClick(R.id.btn_another_activity)
    public void anotherActivity() {
        startActivity(new Intent(this, SecondActivity.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }


    /**
     * Shows a FancyShowCaseView that focuses to ActionBar items
     *
     * @param item actionbar item to focus
     * @return true
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        new FancyShowCaseView.Builder(this)
                .focusOn(findViewById(item.getItemId()))
                .title("Focus on Actionbar items")
                .build()
                .show();
        return true;
    }
}
