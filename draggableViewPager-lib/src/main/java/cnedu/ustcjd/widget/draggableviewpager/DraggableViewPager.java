package cnedu.ustcjd.widget.draggableviewpager;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;

import cnedu.ustcjd.widget.draggableviewpager.callbacks.OnPageChangedListener;
import cnedu.ustcjd.widget.draggableviewpager.utils.SystemUtils;


public class DraggableViewPager extends HorizontalScrollView implements ViewPagerContainer, OnGestureListener {

    private static final int FLING_VELOCITY = 500;
    private int PAGE_SCROLL_SPEED = 500;
    private boolean isPageScrollAnimationEnabled = true;
    private boolean isScrollEnalbed = true;
    private int activePage = 0;
    private boolean activePageRestored = false;

    private DragDropGrid grid;
    private DraggableViewPagerAdapter adapter;
    private GestureDetector gestureScanner;

    private OnPageChangedListener pageChangedListener;
    private int bgXmlRes;
    private Context mContext;

    public DraggableViewPager(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        setBackground(attrs);

        initPagedScroll();
        initGrid();
    }

    public DraggableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);

        setBackground(attrs);

        initPagedScroll();
        initGrid();
    }

    public DraggableViewPager(Context context) {
        super(context);


        initPagedScroll();
        initGrid();
    }

    public DraggableViewPager(Context context, AttributeSet attrs, int defStyle, DraggableViewPagerAdapter adapter) {
        super(context, attrs, defStyle);

        setBackground(attrs);

        this.adapter = adapter;
        initPagedScroll();
        initGrid();
    }

    public DraggableViewPager(Context context, AttributeSet attrs, DraggableViewPagerAdapter adapter) {
        super(context, attrs);

        setBackground(attrs);

        this.adapter = adapter;
        initPagedScroll();
        initGrid();
    }

    public DraggableViewPager(Context context, DraggableViewPagerAdapter adapter) {
        super(context);
        this.adapter = adapter;
        initPagedScroll();
        initGrid();
    }

    private void initGrid() {
        final ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        final LockableScrollView lockableScrollView = new LockableScrollView(getContext());
        grid = new DragDropGrid(getContext());
        grid.setLockableScrollView(lockableScrollView);
        if (bgXmlRes != -1) {
            grid.setBackgroundResource(bgXmlRes);
        }
        lockableScrollView.addView(grid);
        addView(lockableScrollView, layoutParams);
    }

    private void setBackground(AttributeSet attrs) {
        final String xmlns = "http://schemas.android.com/apk/res/android";
        bgXmlRes = attrs.getAttributeResourceValue(xmlns, "background", -1);
    }

    public void initPagedScroll() {

        //setScrollBarStyle(SCROLLBARS_INSIDE_OVERLAY);
        //setSmoothScrollingEnabled(true);
        //hide horizontal scrollbar
        setHorizontalScrollBarEnabled(false);

        if (!isInEditMode()) {
            gestureScanner = new GestureDetector(getContext(), this);
        }

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!isScrollEnalbed) return true;
                boolean specialEventUsed = gestureScanner.onTouchEvent(event);
                if (!specialEventUsed && (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)) {
                    int scrollX = getScrollX();
                    int onePageWidth = v.getMeasuredWidth();
                    //int page = ((scrollX + (onePageWidth *1/2)) / onePageWidth);
                    int hoverPageWidth = onePageWidth * 1 / 3;
                    int page = currentPage();
                    if (scrollX - onePageWidth * page >= hoverPageWidth) {
                        page = (scrollX + onePageWidth - hoverPageWidth) / onePageWidth;
                    } else if (scrollX - onePageWidth * page <= -hoverPageWidth) {
                        page = scrollX / onePageWidth;
                    }
                    scrollToPage(page);
                    return true;
                } else if (!specialEventUsed && event.getAction() == MotionEvent.ACTION_MOVE) {
                    int scrollX = getScrollX();
                    int onePageWidth = v.getMeasuredWidth();
                    int hoverPageWidth = onePageWidth * 3 / 4;  // the key factor to get away the hover area
                    int page = currentPage();

                    int roundPage = scrollX / onePageWidth;
                    if (scrollX - onePageWidth * roundPage >= hoverPageWidth) {
                        page = (scrollX + onePageWidth - hoverPageWidth) / onePageWidth;
                    } else if (scrollX - onePageWidth * roundPage <= onePageWidth - hoverPageWidth) {
                        page = scrollX / onePageWidth;
                    }

                    if (page != activePage) {
                        grid.updateCachedPages(page);
                        activePage = page;
                        if (pageChangedListener != null) {
                            pageChangedListener.onPageChanged(DraggableViewPager.this, page);
                        }
                        return true;
                    }
                    return false;
                } else {
                    return specialEventUsed;
                }
            }
        });
    }

    public boolean isFullScreen() {
        return grid.isFullScreen();
    }

    public void exitFullScreen() {
        grid.exitFullScreen();
    }

    public void setOnPageChangedListener(OnPageChangedListener listener) {
        this.pageChangedListener = listener;
    }

    public void setAdapter(DraggableViewPagerAdapter adapter) {
        this.adapter = adapter;
        grid.setAdapter(adapter);
        grid.setContainer(this);
    }

    public void setClickListener(DragDropGrid.OnDragDropGridItemClickListener l) {
        grid.setOnItemClickListener(l);
    }

    public void setSwapListener(DragDropGrid.OnSwapItemListener l) {
        grid.setOnSwapItemListener(l);
    }

    /**
     * enable and disable draggable function,default:enabled
     *
     * @param enabled
     */
    public void setDragEnabled(boolean enabled) {
        grid.setDragEnabled(enabled);
    }

    public void setDragZoomInAnimEnabled(boolean enabled) {
        grid.setDragZoomInAnimEnabled(enabled);
    }

    public void setPageScrollAnimationEnabled(boolean enabled) {
        this.isPageScrollAnimationEnabled = enabled;
    }

    public void setItemDoubleClickFullScreenEnabled(boolean enabled) {
        grid.setItemDoubleClickFullScreenEnabled(enabled);
    }

    public void setOnItemAnimationListener(DragDropGrid.OnDragDropGridItemAnimationListener listener) {
        grid.setOnItemAnimationListener(listener);
    }

    /**
     * set page scroll time in millisecond if scroll animation is enabled,default:500ms
     *
     * @param milliSeconds
     */
    public void setPageScrollSpeed(int milliSeconds) {
        this.PAGE_SCROLL_SPEED = milliSeconds;
    }

    public boolean onLongClick(View v) {
        return grid.onLongClick(v);
    }

    public void notifyDataSetChanged() {
        grid.notifyDataChanged();
    }

    @Override
    public void scrollToPage(int page) {
        grid.updateCachedPages(page);
        int oldActivePage = activePage;
        activePage = page;

        boolean hasNav = SystemUtils.checkDeviceHasNavigationBar(getContext());
        Log.d("navigation_bar_height", "hasNav is " + hasNav);

        final WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        int hasNavOther = SystemUtils.hasSoftKeys(wm);
        Log.d("navigation_bar_height", "hasNavOther is " + hasNavOther);

        final Display display = wm.getDefaultDisplay();
        final Point point = new Point();
        display.getSize(point);
        int onePageWidth = point.x + (hasNavOther > 0 && hasNav? hasNavOther : 0);

        int scrollTo = page * onePageWidth;
        if (isPageScrollAnimationEnabled) {
            ObjectAnimator animator = ObjectAnimator.ofInt(this, "scrollX", scrollTo);
            animator.setDuration(PAGE_SCROLL_SPEED);
            animator.start();
        } else {
            smoothScrollTo(scrollTo, 0);
        }
        if (pageChangedListener != null && oldActivePage != page) {
            pageChangedListener.onPageChanged(this, page);
        }
    }

    @Override
    public void scrollLeft() {
        int newPage = activePage - 1;
        if (canScrollToPreviousPage()) {
            scrollToPage(newPage);
        } else {
            scrollToPage(activePage);
        }
    }

    @Override
    public void scrollRight() {
        int newPage = activePage + 1;
        if (canScrollToNextPage()) {
            scrollToPage(newPage);
        } else {
            scrollToPage(activePage);
        }
    }

    @Override
    public int currentPage() {
        return activePage;
    }

    @Override
    public void enableScroll() {
        requestDisallowInterceptTouchEvent(false);
        isScrollEnalbed = true;
    }

    @Override
    public void disableScroll() {
        requestDisallowInterceptTouchEvent(true);
        isScrollEnalbed = false;
    }

    @Override
    public boolean canScrollToNextPage() {
        int newPage = activePage + 1;
        return (newPage < adapter.pageCount());
    }

    @Override
    public boolean canScrollToPreviousPage() {
        int newPage = activePage - 1;
        return (newPage >= 0);
    }

    public void restoreCurrentPage(int currentPage) {
        activePage = currentPage;
        activePageRestored = true;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (activePageRestored) {
            activePageRestored = false;
            scrollToRestoredPage();
        }
    }

    private void scrollToRestoredPage() {
        scrollToPage(activePage);
    }

    @Override
    public boolean onDown(MotionEvent arg0) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent evt1, MotionEvent evt2, float velocityX, float velocityY) {
        if (velocityX < -FLING_VELOCITY) {
            scrollRight();
            return true;
        } else if (velocityX > FLING_VELOCITY) {
            scrollLeft();
            return true;
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent arg0) {
    }

    @Override
    public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent arg0) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent arg0) {
        return false;
    }

    public DraggableViewPagerAdapter getAdapter() {
        return adapter;
    }
}
