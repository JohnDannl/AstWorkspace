package com.arcsoft.closeli.draggableviewpager;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

import com.arcsoft.closeli.draggableviewpager.callbacks.OnPageChangedListener;

public class DraggableViewPager extends HorizontalScrollView implements ViewPagerContainer, OnGestureListener {

    private static final int FLING_VELOCITY = 500;
    private int PAGE_SCROLL_SPEED=800;
    private int activePage = 0;
    private boolean activePageRestored = false;

    private DragDropGrid grid;
    private DraggableViewPagerAdapter adapter;
    private OnClickListener listener;
    private GestureDetector gestureScanner;

    private OnPageChangedListener pageChangedListener;
    private int bgXmlRes;

    public DraggableViewPager(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

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
                boolean specialEventUsed = gestureScanner.onTouchEvent(event);
                if (!specialEventUsed && (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)) {
                    int scrollX = getScrollX();
                    int onePageWidth = v.getMeasuredWidth();
                    //int page = ((scrollX + (onePageWidth *1/2)) / onePageWidth);
                    int hoverPageWidth=onePageWidth*1/3;
                    int page=currentPage();
                    if(scrollX-onePageWidth*page>=hoverPageWidth){
                        page = (scrollX + onePageWidth -hoverPageWidth) / onePageWidth;
                    }else if(scrollX-onePageWidth*page<=-hoverPageWidth){
                        page=scrollX / onePageWidth;
                    }
                    scrollToPage(page);
                    return true;
                } else {
                    return specialEventUsed;
                }
            }
        });
    }

    public void setOnPageChangedListener(OnPageChangedListener listener) {
        this.pageChangedListener = listener;
    }

    public void setAdapter(DraggableViewPagerAdapter adapter) {
        this.adapter = adapter;
        grid.setAdapter(adapter);
        grid.setContainer(this);
    }

    public void setClickListener(OnClickListener l) {
        this.listener = l;
        grid.setOnClickListener(l);
    }

    /**
     * enable and disable draggable function,default:enabled
     * @param enabled
     */
    public void setDragEnabled(boolean enabled){
        grid.setDragEnabled(enabled);
    }

    /**
     * set page scroll time in millisecond,default:800ms
     * @param milliSeconds
     */
    public void setPageScrollSpeed(int milliSeconds){
        this.PAGE_SCROLL_SPEED=milliSeconds;
    }

    public boolean onLongClick(View v) {
        return grid.onLongClick(v);
    }

    public void removeItem(int page, int index) {
        grid.removeItem(page, index);
    }

    public void notifyDataSetChanged() {
        grid.reloadViews();
    }

    @Override
    public void scrollToPage(int page) {
        activePage = page;
        int onePageWidth = getMeasuredWidth();
        int scrollTo = page * onePageWidth;
        ObjectAnimator animator= ObjectAnimator.ofInt(this, "scrollX",scrollTo);
        animator.setDuration(PAGE_SCROLL_SPEED);
        animator.start();
        //smoothScrollTo(scrollTo, 0);
        if (pageChangedListener != null)
            pageChangedListener.onPageChanged(this, page);
    }

    @Override
    public void scrollLeft() {
        int newPage = activePage - 1;
        if (canScrollToPreviousPage()) {
            scrollToPage(newPage);
        }
    }

    @Override
    public void scrollRight() {
        int newPage = activePage + 1;
        if (canScrollToNextPage()) {
            scrollToPage(newPage);
        }
    }

    @Override
    public int currentPage() {
        return activePage;
    }

    @Override
    public void enableScroll() {
        requestDisallowInterceptTouchEvent(false);
    }

    @Override
    public void disableScroll() {
        requestDisallowInterceptTouchEvent(true);
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
}
