package cnedu.ustcjd.widget.draggableviewpager;

import android.animation.Animator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cnedu.ustcjd.widget.draggableviewpager.utils.SystemUtils;

public class DragDropGrid extends ViewGroup implements OnTouchListener, OnLongClickListener {

    private static final String TAG = "DragDropGrid";
    private static final long DOUBLE_CLICK_INTERVAL = 250; // in millis
    private static final boolean noStatusBar = true;
    public static int ROW_HEIGHT = 300;
    private static int DRAGGED_MOVE_ANIMATION_DURATION = 200;
    private static int DRAGGED_ZOOM_IN_ANIMATION_DURATION = 200;
    private static int FULLSCREEN_ANIMATION_DURATION = 10;
    private static int OFFSCREEN_PAGE_LIMIT = 1;
    private static int EGDE_DETECTION_MARGIN = 35;
    private boolean hasDoubleClick = false;
    private long lastClickTime = 0L;
    private int lastClickItem = -1;
    private int fullScreenItem = -1;
    private boolean hasFullScreen = false;
    private boolean enableDrag = true;
    private boolean enableDragAnim = false;
    private boolean enableFullScreen = false;
    private int offsetPosition = 0; // the index of firs item in views,support a small size views and dynamic store address
    private DraggableViewPagerAdapter adapter;
    private OnDragDropGridItemClickListener onItemClickListener = null;
    private OnDragDropGridItemAnimationListener mItemAnimationListener = null;
    private OnSwapItemListener onSwapItemListener;
    private ViewPagerContainer container;
    private List<View> views = new ArrayList<View>();
    private int gridPageWidth = 0;
    private int dragged = -1;
    private int columnWidth;
    private int rowHeight;
    private int columnCount;
    private int rowCount;
    private LockableScrollView lockableScrollView;

    private boolean movingView;
    private int lastTarget = -1;
    private boolean wasOnEdgeJustNow = false;
    private Timer edgeScrollTimer;
    private int lastTouchX;
    private int lastTouchY;

    /**
     * The width of screen.
     */
    private int displayWidth;
    /**
     * The height of screen.
     */
    private int displayHeight;
    private int measuredHeight;

    public DragDropGrid(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public DragDropGrid(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DragDropGrid(Context context) {
        super(context);
        init();
    }

    public DragDropGrid(Context context, AttributeSet attrs, int defStyle, DraggableViewPagerAdapter adapter, ViewPagerContainer container) {
        super(context, attrs, defStyle);
        this.adapter = adapter;
        this.container = container;
        init();
    }

    public DragDropGrid(Context context, AttributeSet attrs, DraggableViewPagerAdapter adapter, ViewPagerContainer container) {
        super(context, attrs);
        this.adapter = adapter;
        this.container = container;
        init();
    }

    public DragDropGrid(Context context, DraggableViewPagerAdapter adapter, ViewPagerContainer container) {
        super(context);
        this.adapter = adapter;
        this.container = container;
        init();
    }

    private void init() {
        if (isInEditMode() && adapter == null) {
            useEditModeAdapter();
        }
        getDisplayDimensions();
        setOnTouchListener(this);
        setOnLongClickListener(this);
    }

    /**
     * Gets dimensions of screen and calculates the rowHeight size
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void getDisplayDimensions() {
        final WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        final Display display = wm.getDefaultDisplay();
        DisplayMetrics realDisplayMetrics = new DisplayMetrics();
        display.getRealMetrics(realDisplayMetrics);
        int realHeight = realDisplayMetrics.heightPixels;

        boolean hasNav = SystemUtils.checkDeviceHasNavigationBar(getContext());
        Log.d("navigation_bar_height", "hasNav is " + hasNav);

        int hasNavOther = SystemUtils.hasSoftKeys(wm);
        Log.d("navigation_bar_height", "hasNavOther is " + hasNavOther);

        final Point point = new Point();
        display.getSize(point);
        displayWidth = point.x + (hasNavOther > 0 && hasNav ? hasNavOther : 0);
        displayHeight = realHeight;

        if (noStatusBar) {
            ROW_HEIGHT = (displayHeight - getPaddingTop() - getPaddingBottom()) / 2;
        } else {
            int statusHeight = getStatusBarHeight();
            ROW_HEIGHT = (displayHeight - getPaddingTop() - getPaddingBottom() - statusHeight) / 2;
        }
    }

    /**
     * Calculates status bar height
     *
     * @return
     */
    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * Used when in edit-mode
     */
    private void useEditModeAdapter() {
        adapter = new DraggableViewPagerAdapter() {

            @Override
            public View view(int page, int index) {
                return null;
            }

            @Override
            public void swapItems(int pageIndex, int itemIndexA, int itemIndexB) {

            }

            @Override
            public int rowCount() {
                return -1;
            }

            @Override
            public void printLayout() {

            }

            @Override
            public int pageCount() {
                return -1;
            }

            @Override
            public void moveItemToPreviousPage(int pageIndex, int itemIndex) {

            }

            @Override
            public void moveItemToNextPage(int pageIndex, int itemIndex) {

            }

            /**
             * deletes the item in page and at position
             *
             * @param obj
             */
            @Override
            public void deleteItem(Object obj) {

            }

            /**
             * add the item to adapter end
             *
             * @param obj
             */
            @Override
            public void addItem(Object obj) {

            }

            @Override
            public int itemCountInPage(int page) {
                return 0;
            }

            @Override
            public int columnCount() {
                return 0;
            }

            @Override
            public int getPageWidth() {
                return 0;
            }

            @Override
            public Object getItemAt(int page, int index) {
                return null;
            }

            @Override
            public boolean disableZoomAnimationsOnChangePage() {
                return false;
            }

            @Override
            public void destroyPage(int page) {

            }

            @Override
            public void destroyItem(Object obj) {

            }

            @Override
            public boolean containsObject(Object obj) {
                return false;
            }

            @Override
            public int size() {
                return 0;
            }
        };
    }

    public void setAdapter(DraggableViewPagerAdapter adapter) {
        this.adapter = adapter;
        addChildViews();
    }

    public void setOnItemClickListener(OnDragDropGridItemClickListener l) {
        onItemClickListener = l;
    }

    public void setOnItemAnimationListener(OnDragDropGridItemAnimationListener listener) {
        mItemAnimationListener = listener;
    }

    public void setDragEnabled(boolean enabled) {
        this.enableDrag = enabled;
    }

    public void setDragZoomInAnimEnabled(boolean enabled) {
        enableDragAnim = enabled;
    }

    public void setItemDoubleClickFullScreenEnabled(boolean enabled) {
        enableFullScreen = enabled;
    }

    public void setOnSwapItemListener(OnSwapItemListener onSwapItemListener) {
        this.onSwapItemListener = onSwapItemListener;
    }

    private void addChildViews() {
        removeAllViews();
        views.clear();
        offsetPosition = 0;
        for (int page = 0; page <= OFFSCREEN_PAGE_LIMIT; page++) {
            for (int item = 0; item < adapter.itemCountInPage(page); item++) {
                View v = adapter.view(page, item);
                v.setTag(adapter.getItemAt(page, item));
                LayoutParams layoutParams = new LayoutParams((displayWidth - getPaddingLeft() - getPaddingRight()) / adapter.columnCount(), ROW_HEIGHT);
                addView(v, layoutParams);
                views.add(v);
            }
        }
    }

    public void notifyDataChanged() {
        int currentPage = container.currentPage();
        boolean goToPrevious = adapter.itemCountInPage(currentPage) == 0 && currentPage > 0;
        if (goToPrevious) {
            container.scrollLeft();
        } else {
            updateCachedPages(currentPage);
        }
    }

    public int indexOfItem(int page, int index) {
        Object item = adapter.getItemAt(page, index);
        return indexOfItem(item);
    }

    private int indexOfItem(Object item) {
        for (int i = 0; i < this.getChildCount(); i++) {
            View v = this.getChildAt(i);
            if (item.equals(v.getTag()))
                return i;
        }
        return -1;
    }

    public void removeItem(int page, int index) {
        Object item = adapter.getItemAt(page, index);
        for (int i = 0; i < this.getChildCount(); i++) {
            View v = this.getChildAt(i);
            if (item.equals(v.getTag())) {
                this.removeView(v);
                return;
            }
        }
    }

    private void cancelAnimations() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child != null) {
                child.clearAnimation();
            }
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        return onTouch(null, event);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                touchDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(event);
                break;
            case MotionEvent.ACTION_UP:
                touchUp(event);
                break;
        }
        if (aViewIsDragged()) {
            return true;
        }
        return false;
    }

    /**
     * For vertical scrolling when item is dragged to the screen edge
     *
     * @return
     */
    private boolean scrollIfNeeded() {
        int height = displayHeight;
        final View draggedView = getDraggedView();
        int hoverHeight = draggedView.getHeight();
        int scrollAmount = 20;


        int[] locations = new int[2];
        draggedView.getLocationOnScreen(locations);
        int y = locations[1];
        if (y <= 0) {
            lockableScrollView.scrollBy(0, -scrollAmount);
            return true;
        }
        if (y + hoverHeight >= height) {
            lockableScrollView.scrollBy(0, scrollAmount);
            return true;
        }

        return false;
    }

    /**
     * Deals with touch-up actions
     *
     * @param event
     */
    private void touchUp(MotionEvent event) {
        if (!aViewIsDragged()) {
            long clickTime = event.getEventTime();
            final int childIndex = getTargetAtCoor((int) event.getX(), (int) event.getY());
            if (clickTime - lastClickTime <= DOUBLE_CLICK_INTERVAL && childIndex == lastClickItem) {
                hasDoubleClick = true;
                if (enableFullScreen) {
                    onItemDoubleClick(childIndex);
                }
            } else {
                hasDoubleClick = false;
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!hasDoubleClick) {
                            onItemClick(childIndex);
                        }
                    }
                }, DOUBLE_CLICK_INTERVAL);
            }
            lastClickTime = clickTime;
            lastClickItem = childIndex;
        } else {
            cancelAnimations();
            cancelEdgeTimer();
            restoreDraggedItem();

            lockableScrollView.setScrollingEnabled(true);
            container.enableScroll();

            movingView = false;
            dragged = -1;
            lastTarget = -1;
        }
    }

    private void onItemClick(int childIndex) {
        if (onItemClickListener != null) {
            ItemPosition itemPosition = getItemPositionOf(childIndex);
            View clickedView = getChildView(childIndex);
            if (clickedView != null && itemPosition != null) {
                onItemClickListener.onClick(clickedView, itemPosition.pageIndex, itemPosition.itemIndex);
            } else {
                onItemClickListener.onClick(null, -1, -1);
            }

        }
    }

    private void onItemDoubleClick(int childIndex) {
        ItemPosition itemPosition = getItemPositionOf(childIndex);
        View clickedView = getChildView(childIndex);

        if (!hasFullScreen && clickedView != null && itemPosition != null) {
            fullScreenItem = childIndex;
            hasFullScreen = true;
            //extendToFullScreen();
            extendToFullScreenWithValueAnimation();
            if (onItemClickListener != null) {
                onItemClickListener.onFullScreenChange(clickedView, itemPosition.pageIndex, itemPosition.itemIndex, true);
            }
        } else {
            //shrinkToNormalScreen();
            shrinkToNormalScreenWithValueAnimation();
            ItemPosition shrinkItemPosition = getItemPositionOf(fullScreenItem);
            View shrinkItemView = getChildView(fullScreenItem);
            if (onItemClickListener != null && shrinkItemView != null && shrinkItemPosition != null) {
                onItemClickListener.onFullScreenChange(shrinkItemView, shrinkItemPosition.pageIndex, shrinkItemPosition.itemIndex, false);
            }
            //fullScreenItem = -1;      // these two lines should execute after the animation finished
            //hasFullScreen=false;
        }

        if (onItemClickListener != null) {
            if (clickedView != null && itemPosition != null) {
                onItemClickListener.onDoubleClick(clickedView, itemPosition.pageIndex, itemPosition.itemIndex);
            } else {
                onItemClickListener.onDoubleClick(null, -1, -1);
            }
        }

    }

    public boolean isFullScreen() {
        return hasFullScreen;
    }

    public void exitFullScreen() {
        if (fullScreenItem >= 0 && enableFullScreen) {
            onItemDoubleClick(fullScreenItem);
        }
    }

    /**
     * Extends item to full screen with directly adjusting layout parameters
     */
    private void extendToFullScreen() {
        container.disableScroll();
        lockableScrollView.setScrollingEnabled(false);

        if (fullScreenItem != -1) {
            View fullScreenView = getChildView(fullScreenItem);
            LayoutParams vlp = fullScreenView.getLayoutParams();
            vlp.width = displayWidth;
            vlp.height = displayHeight;
            fullScreenView.setLayoutParams(vlp);
            bringFullScreenItemToFront();

            /*int page=currentPage();
            for(int i=0;i<adapter.itemCountInPage(page);i++){
                int position=positionOfItem(page,i);
                if(position!=fullScreenItem){
                    View childView=getChildView(position);
                    LayoutParams cvlp=childView.getLayoutParams();
                    cvlp.width=0;
                    cvlp.height=0;
                    childView.setLayoutParams(cvlp);
                    //childView.setVisibility(View.GONE);
                }
            }*/
        }
    }

    /**
     * Shrinks item to normal size with directly adjusting layout parameters
     */
    private void shrinkToNormalScreen() {
        if (fullScreenItem != -1) {
            /*int page=currentPage();
            for(int i=0;i<adapter.itemCountInPage(page);i++){
                int position=positionOfItem(page,i);
                if(position!=fullScreenItem){
                    View childView=getChildView(position);
                    LayoutParams cvlp =childView.getLayoutParams();
                    cvlp.width = columnWidth;
                    cvlp.height = rowHeight;
                    childView.setLayoutParams(cvlp);
                    //childView.setVisibility(View.VISIBLE);
                }
            }*/

            View fullScreenView = getChildView(fullScreenItem);
            LayoutParams vlp = fullScreenView.getLayoutParams();
            vlp.width = (displayWidth - getPaddingLeft() - getPaddingRight()) / adapter.columnCount();
            vlp.height = ROW_HEIGHT;
            fullScreenView.setLayoutParams(vlp);
        }

        lockableScrollView.setScrollingEnabled(true);
        container.enableScroll();
    }

    /**
     * Extends item to full screen with value animation of layout parameters
     */
    private void extendToFullScreenWithValueAnimation() {
        container.disableScroll();
        lockableScrollView.setScrollingEnabled(false);

        if (fullScreenItem != -1) {
            final View fullView = getChildView(fullScreenItem);
            bringFullScreenItemToFront();
            final LayoutParams oldLayoutParam = fullView.getLayoutParams();
            final LayoutParams newLayoutParam = new LayoutParams(displayWidth, displayHeight);
            layoutAnimateScale(oldLayoutParam, newLayoutParam, fullView, true);
            Log.i(TAG, String.format("ExtendsToFullScreen item:%d", fullScreenItem));
        }
    }

    /**
     * Scale ValueAnimator constructor for <code>fullView</code> layoutParameter, scale from <code>oldLayoutParam</code>
     * to <code>newLayoutParam</code>
     *
     * @param oldLayoutParam
     * @param newLayoutParam
     * @param fullView
     * @param toFullScreen   is to extend to fullscreen or not(shrink to normal size)
     */
    private void layoutAnimateScale(LayoutParams oldLayoutParam, LayoutParams newLayoutParam,
                                    final View fullView, final boolean toFullScreen) {
        ValueAnimator layoutAnim = ValueAnimator.ofObject(new TypeEvaluator() {
            @Override
            public Object evaluate(float fraction, Object startValue, Object endValue) {
                float width = ((LayoutParams) startValue).width
                        + fraction * (((LayoutParams) endValue).width - ((LayoutParams) startValue).width);
                float height = ((LayoutParams) startValue).height
                        + fraction * (((LayoutParams) endValue).height - ((LayoutParams) startValue).height);
                LayoutParams tmpLayoutParam = new LayoutParams((int) width, (int) height);
                return tmpLayoutParam;
            }
        }, oldLayoutParam, newLayoutParam);
        layoutAnim.setDuration(FULLSCREEN_ANIMATION_DURATION).addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                LayoutParams tmpLayoutParam = (LayoutParams) animation.getAnimatedValue();
                LayoutParams layoutParam = fullView.getLayoutParams();
                layoutParam.width = tmpLayoutParam.width;
                layoutParam.height = tmpLayoutParam.height;
                fullView.setLayoutParams(layoutParam);
            }
        });
        layoutAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                ItemPosition itemPos = getItemPositionOf(fullScreenItem);
                if (mItemAnimationListener != null && itemPos != null) {
                    mItemAnimationListener.onFullScreenChangeAnimationStart(fullView, itemPos.pageIndex, itemPos.itemIndex, toFullScreen);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                ItemPosition itemPos = getItemPositionOf(fullScreenItem);
                if (mItemAnimationListener != null && itemPos != null) {
                    mItemAnimationListener.onFullScreenChangeAnimationEnd(fullView, itemPos.pageIndex, itemPos.itemIndex, toFullScreen);
                }
                if (!toFullScreen) {
                    fullScreenItem = -1;      // these two lines should execute after the animation finished
                    hasFullScreen = false;
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        layoutAnim.start();
    }

    /**
     * Shrinks item to normal size with value animation of layout parameters
     */
    private void shrinkToNormalScreenWithValueAnimation() {
        if (fullScreenItem != -1) {
            View fullView = getChildView(fullScreenItem);
            final LayoutParams oldLayoutParam = fullView.getLayoutParams();
            final LayoutParams newLayoutParam = new LayoutParams(columnWidth, rowHeight);
            layoutAnimateScale(oldLayoutParam, newLayoutParam, fullView, false);
            Log.i(TAG, String.format("ShrinksToNormalScreen item:%d", fullScreenItem));
        }
        lockableScrollView.setScrollingEnabled(true);
        container.enableScroll();
    }

    /**
     * Calls when user cancels the drag action to restore the dragged item to original position
     */
    private void restoreDraggedItem() {
        Point targetCoor = getCoorForIndex(dragged);
        View targetView = getChildView(dragged);
        targetView.layout(targetCoor.x, targetCoor.y,
                targetCoor.x + targetView.getMeasuredWidth(), targetCoor.y + targetView.getMeasuredHeight());
        if (mItemAnimationListener != null) {
            ItemPosition itemPos = getItemPositionOf(dragged);
            mItemAnimationListener.onDraggedViewAnimationEnd(targetView, itemPos.pageIndex, itemPos.itemIndex);
        }
    }

    private void tellAdapterDraggedIsDeleted(Integer newDraggedPosition) {
        ItemPosition position = getItemPositionOf(newDraggedPosition);
        //adapter.deleteItem(position.pageIndex, position.itemIndex);
    }

    private void tellAdapterPageIsDestroyed(int page) {
        adapter.destroyPage(page);
    }

    private void touchDown(MotionEvent event) {
        //lastTouchX = (int) event.getRawX() + (currentPage() * gridPageWidth);
        lastTouchX = (int) event.getX();
        lastTouchY = (int) event.getRawY();
    }

    private void touchMove(MotionEvent event) {
        if (movingView && aViewIsDragged()) {
            lastTouchX = (int) event.getX();
            lastTouchY = (int) event.getY();

            ensureThereIsNoArtifact();
            moveDraggedView(lastTouchX, lastTouchY);
            manageSwapPosition(lastTouchX, lastTouchY);
            manageEdgeCoordinates(lastTouchX);
            scrollIfNeeded();
        }
    }

    private void ensureThereIsNoArtifact() {
        invalidate();
    }

    /**
     * Moves the dragged item to position (<code>x,y</code>)
     *
     * @param x
     * @param y
     */
    private void moveDraggedView(int x, int y) {
        View childAt = getDraggedView();

        int width = childAt.getMeasuredWidth();
        int height = childAt.getMeasuredHeight();

        int l = x - (1 * width / 2);
        int t = y - (1 * height / 2);

        childAt.layout(l, t, l + width, t + height);
    }

    /**
     * Calls when the dragged item is above another item to swap their position with animation
     *
     * @param x
     * @param y
     */
    private void manageSwapPosition(int x, int y) {
        int target = getTargetAtCoor(x, y);
        if (childHasMoved(target) && target != lastTarget) {
            animateGap(target);
            lastTarget = target;
        }
    }

    /**
     * Calls when the dragged item is on edge of screen, determine whether to scroll left or right
     *
     * @param x
     */
    private void manageEdgeCoordinates(int x) {
        final boolean onRightEdge = onRightEdgeOfScreen(x);
        final boolean onLeftEdge = onLeftEdgeOfScreen(x);

        if (canScrollToEitherSide(onRightEdge, onLeftEdge)) {
            if (!wasOnEdgeJustNow) {
                startEdgeDelayTimer(onRightEdge, onLeftEdge);
                wasOnEdgeJustNow = true;
            }
        } else {
            if (wasOnEdgeJustNow) {
                stopAnimateOnTheEdge();
            }
            wasOnEdgeJustNow = false;
            cancelEdgeTimer();
        }
    }

    private void stopAnimateOnTheEdge() {
        View draggedView = getDraggedView();
        draggedView.clearAnimation();
        animateDragged();
    }

    private void cancelEdgeTimer() {

        if (edgeScrollTimer != null) {
            edgeScrollTimer.cancel();
            edgeScrollTimer = null;
        }
    }

    /**
     * Starts the timer for edge scrolling animation
     *
     * @param onRightEdge
     * @param onLeftEdge
     */
    private void startEdgeDelayTimer(final boolean onRightEdge, final boolean onLeftEdge) {
        if (canScrollToEitherSide(onRightEdge, onLeftEdge)) {
            animateOnTheEdge();
            if (edgeScrollTimer == null) {
                edgeScrollTimer = new Timer();
                scheduleScroll(onRightEdge, onLeftEdge);
            }
        }
    }

    private void scheduleScroll(final boolean onRightEdge, final boolean onLeftEdge) {
        edgeScrollTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (wasOnEdgeJustNow) {
                    wasOnEdgeJustNow = false;
                    post(new Runnable() {
                        @Override
                        public void run() {
                            scroll(onRightEdge, onLeftEdge);
                            cancelAnimations();
                            animateDragged();
                        }
                    });
                }
            }
        }, 400);
    }

    private boolean canScrollToEitherSide(final boolean onRightEdge, final boolean onLeftEdge) {
        return (onLeftEdge && container.canScrollToPreviousPage()) || (onRightEdge && container.canScrollToNextPage());
    }

    /**
     * Scrolls to right or left when dragged item is on edge
     *
     * @param onRightEdge
     * @param onLeftEdge
     */
    private void scroll(boolean onRightEdge, boolean onLeftEdge) {
        cancelEdgeTimer();

        if (onLeftEdge && container.canScrollToPreviousPage()) {
            scrollToPreviousPage();
        } else if (onRightEdge && container.canScrollToNextPage()) {
            scrollToNextPage();
        }
        wasOnEdgeJustNow = false;
    }

    private void scrollToNextPage() {
        if (!aViewIsDragged()) return; // error occurs if dragged equals -1
        tellAdapterToMoveItemToNextPage(dragged);
        moveDraggedToNextPage();

        container.scrollRight();
        stopAnimateOnTheEdge();
        lockableScrollView.scrollTo(0, 0);
    }

    private void scrollToPreviousPage() {
        if (!aViewIsDragged()) return;
        tellAdapterToMoveItemToPreviousPage(dragged);
        moveDraggedToPreviousPage();

        container.scrollLeft();
        stopAnimateOnTheEdge();
        lockableScrollView.scrollTo(0, 0);
    }

    /**
     * Moves the dragged item to previous page and swaps with the last item of previous page
     * with translation  {@link ValueAnimator}
     */
    private void moveDraggedToPreviousPage() {
        final Point draggedViewCoor = getCoorForIndex(dragged);
        int indexFirstElementInCurrentPage = findIndexOfTheFirstElementInCurrentPage();
        int indexOfDraggedOnNewPage = indexFirstElementInCurrentPage - 1;
        final int targetIndex = indexOfDraggedOnNewPage;
        final View targetView = getChildView(targetIndex);
        final Point targetViewCoor = getCoorForIndex(targetIndex);
        swapViewsItems(targetIndex, dragged);
        dragged = targetIndex;
        animateMoveView(draggedViewCoor, targetViewCoor, targetView);
    }

    /**
     * Moves the dragged item to next page and swaps with the first item of next page
     * with translation  {@link ValueAnimator}
     */
    private void moveDraggedToNextPage() {
        final Point draggedViewCoor = getCoorForIndex(dragged);
        int indexFirstElementInNextPage = findIndexOfTheFirstElementInNextPage();
        int indexOfDraggedOnNewPage = indexFirstElementInNextPage;
        int targetIndex = indexOfDraggedOnNewPage;
        final View targetView = getChildView(targetIndex);
        final Point targetViewCoor = getCoorForIndex(targetIndex);
        swapViewsItems(targetIndex, dragged);
        dragged = targetIndex;
        animateMoveView(draggedViewCoor, targetViewCoor, targetView);
    }

    private int findIndexOfTheFirstElementInCurrentPage() {
        int currentPage = currentPage();
        int indexFirstElementInCurrentPage = 0;
        for (int i = 0; i < currentPage; i++) {
            indexFirstElementInCurrentPage += adapter.itemCountInPage(i);
        }
        return indexFirstElementInCurrentPage;
    }

    private int findIndexOfTheFirstElementInNextPage() {
        int currentPage = currentPage();
        int indexFirstElementInNextPage = 0;
        for (int i = 0; i <= currentPage; i++) {
            indexFirstElementInNextPage += adapter.itemCountInPage(i);
        }
        return indexFirstElementInNextPage;
    }

    /**
     * Determines whether is on the left edge of screen according to <code>x</code>
     *
     * @param x coordinate
     * @return <code>true</code> if is on the left edge of screen, <code>false</code> otherwise
     */
    private boolean onLeftEdgeOfScreen(int x) {
        int currentPage = container.currentPage();

        int leftEdgeXCoor = currentPage * gridPageWidth;
        int distanceFromEdge = x - leftEdgeXCoor;
        return (x > 0 && distanceFromEdge <= EGDE_DETECTION_MARGIN);
    }

    /**
     * Determines whether is on the right edge of screen according to <code>x</code>
     *
     * @param x coordinate
     * @return <code>true</code> if is on the right edge of screen, <code>false</code> otherwise
     */
    private boolean onRightEdgeOfScreen(int x) {
        int currentPage = container.currentPage();

        int rightEdgeXCoor = (currentPage * gridPageWidth) + gridPageWidth;
        int distanceFromEdge = rightEdgeXCoor - x;
        return (x > (rightEdgeXCoor - EGDE_DETECTION_MARGIN)) && (distanceFromEdge < EGDE_DETECTION_MARGIN);
    }

    /**
     * A reverse repeated scale animation for item on the edge
     */
    private void animateOnTheEdge() {
        if (!adapter.disableZoomAnimationsOnChangePage()) {
            View v = getDraggedView();

            ScaleAnimation scale = new ScaleAnimation(.9f, 1.1f, .9f, 1.1f, v.getMeasuredWidth() * 3 / 4, v.getMeasuredHeight() * 3 / 4);
            scale.setDuration(200);
            scale.setRepeatMode(Animation.REVERSE);
            scale.setRepeatCount(Animation.INFINITE);

            v.clearAnimation();
            v.startAnimation(scale);
        }
    }

    /**
     * Animation for swap dragged item and another item on position <code>targetLocationInGrid</code>
     *
     * @param targetLocationInGrid
     */
    private void animateGap(int targetLocationInGrid) {
        int viewAtPosition = targetLocationInGrid;
        if (viewAtPosition == dragged) {
            return;
        }
        final View targetView = getChildView(viewAtPosition);
        final Point draggedViewCoor = getCoorForIndex(dragged);
        final Point targetViewCoor = getCoorForIndex(viewAtPosition);
        swapViewsItems(dragged, viewAtPosition);
        tellAdapterToSwapDraggedWithTarget(dragged, viewAtPosition);
        dragged = viewAtPosition;

        /*targetView.layout(draggedViewCoor.x,draggedViewCoor.y,
                draggedViewCoor.x + targetView.getMeasuredWidth(),draggedViewCoor.y + targetView.getMeasuredHeight());*/
        animateMoveView(draggedViewCoor, targetViewCoor, targetView);
    }

    /**
     * Translation ValueAnimator constructor for swapping dragged item and <code>targetView</code>
     *
     * @param draggedViewCoor Coordinate of dragged item
     * @param targetViewCoor  Coordinate of target item
     * @param targetView      View of target item
     */
    private void animateMoveView(Point draggedViewCoor, Point targetViewCoor, final View targetView) {
        ValueAnimator animMove = ValueAnimator.ofObject(new TypeEvaluator() {
            @Override
            public Object evaluate(float fraction, Object startValue, Object endValue) {
                float x = ((Point) startValue).x + fraction * (((Point) endValue).x - ((Point) startValue).x);
                float y = ((Point) startValue).y + fraction * (((Point) endValue).y - ((Point) startValue).y);
                return new Point((int) x, (int) y);
            }
        }, targetViewCoor, draggedViewCoor);
        animMove.setDuration(DRAGGED_MOVE_ANIMATION_DURATION).addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Point layoutCoor = (Point) animation.getAnimatedValue();
                targetView.layout(layoutCoor.x, layoutCoor.y,
                        layoutCoor.x + targetView.getMeasuredWidth(), layoutCoor.y + targetView.getMeasuredHeight());
            }
        });
        animMove.start();
    }

    /**
     * Gets coordinate of item at adapter position
     *
     * @param index adapter position of item
     * @return
     */
    private Point getCoorForIndex(int index) {
        ItemPosition itemPosition = getItemPositionOf(index);

        int row = itemPosition.itemIndex / columnCount;
        int col = itemPosition.itemIndex - (row * columnCount);

        int x = (currentPage() * gridPageWidth) + (columnWidth * col);
        int y = rowHeight * row;

        return new Point(x, y);
    }

    /**
     * Gets adapter position of item at coordinate <code>(x,y)</code>
     *
     * @param x
     * @param y
     * @return
     */
    private int getTargetAtCoor(int x, int y) {
        int page = currentPage();

        int col = getColumnOfCoordinate(x, page);
        int row = getRowOfCoordinate(y);
        int positionInPage = col + (row * columnCount);

        return positionOfItem(page, positionInPage);
    }

    private int getColumnOfCoordinate(int x, int page) {
        int col = 0;
        int pageLeftBorder = (page) * gridPageWidth;
        for (int i = 1; i <= columnCount; i++) {
            int colRightBorder = (i * columnWidth) + pageLeftBorder;
            if (x < colRightBorder) {
                break;
            }
            col++;
        }
        return col;
    }

    private int getRowOfCoordinate(int y) {
        int row = 0;
        for (int i = 1; i <= rowCount; i++) {
            if (y < i * rowHeight) {
                break;
            }
            row++;
        }
        return row;
    }

    private int currentPage() {
        return container.currentPage();
    }

    private boolean childHasMoved(int position) {
        return position != -1;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        widthSize = remeasure(widthMode, widthSize);
        int largestSize = getLargestPageSize();

        measuredHeight = rowCount * rowHeight;//(largestSize+columnCount-1)/columnCount * rowHeight;
        setMeasuredDimension(widthSize * adapter.pageCount(), measuredHeight);
    }

    private int remeasure(int widthMode, int widthSize) {
        widthSize = acknowledgeWidthSize(widthMode, widthSize, displayWidth);
        measureChildren(MeasureSpec.EXACTLY, MeasureSpec.UNSPECIFIED);
        columnCount = adapter.columnCount();
        rowCount = adapter.rowCount();
        columnWidth = widthSize / adapter.columnCount();
        rowHeight = ROW_HEIGHT;
        return widthSize;
    }

    private int getLargestPageSize() {
        int size = 0;
        for (int page = 0; page < adapter.pageCount(); page++) {
            final int currentSize = adapter.itemCountInPage(page);
            if (currentSize > size) {
                size = currentSize;
            }
        }
        return size;
    }

    private int acknowledgeWidthSize(int widthMode, int widthSize, int displayWidth) {
        if (widthMode == MeasureSpec.UNSPECIFIED) {
            widthSize = displayWidth;
        }

        if (adapter.getPageWidth() != 0) {
            widthSize = adapter.getPageWidth();
        }

        gridPageWidth = widthSize;
        return widthSize;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        //If we don't have pages don't do layout
        if (adapter.pageCount() == 0)
            return;
        int pageWidth = (r - l) / adapter.pageCount();

        /*for (int page = 0; page < adapter.pageCount(); page++) {
            layoutPage(pageWidth, page);
        }*/
        int pageShowMin = 0 > currentPage() - OFFSCREEN_PAGE_LIMIT ? 0 : currentPage() - OFFSCREEN_PAGE_LIMIT;
        int pageShowMax = adapter.pageCount() - 1 < currentPage() + OFFSCREEN_PAGE_LIMIT ? adapter.pageCount() - 1 : currentPage() + OFFSCREEN_PAGE_LIMIT;
        for (int page = pageShowMin; page <= pageShowMax; page++) {
            layoutPage(pageWidth, page);
        }
        if (weWereMovingDragged()) {
            bringDraggedToFront();
        }
        if (fullScreenItem != -1) {
            bringFullScreenItemToFront();
        }
    }

    private boolean weWereMovingDragged() {
        return dragged != -1;
    }

    private void layoutPage(int pageWidth, int page) {
        int col = 0;
        int row = 0;
        for (int childIndex = 0; childIndex < adapter.itemCountInPage(page); childIndex++) {
            layoutAChild(pageWidth, page, col, row, childIndex);
            col++;
            if (col == columnCount) {
                col = 0;
                row++;
            }
        }
    }

    private void layoutAChild(int pageWidth, int page, int col, int row, int childIndex) {
        int position = positionOfItem(page, childIndex);

        View child = getChildView(position);

        if (child == null || child.getVisibility() == View.GONE) return;

        int left = 0;
        int top = 0;
        if (position == dragged) {
            left = lastTouchX - child.getMeasuredWidth() / 2;
            top = lastTouchY - child.getMeasuredHeight() / 2;
        } else if (position == fullScreenItem) {
            switch (getItemPositionOf(fullScreenItem).itemIndex) {
                case 0:
                    left = page * pageWidth;
                    top = 0;
                    break;
                case 1:
                    left = (page + 1) * pageWidth - child.getMeasuredWidth();
                    top = 0;
                    break;
                case 2:
                    left = page * pageWidth;
                    top = displayHeight - child.getMeasuredHeight();
                    break;
                case 3:
                    left = (page + 1) * pageWidth - child.getMeasuredWidth();
                    top = displayHeight - child.getMeasuredHeight();
                    break;
                default:
                    break;
            }
        } else {
            left = (page * pageWidth) + (col * columnWidth) + ((columnWidth - child.getMeasuredWidth()) / 2);
            top = (row * rowHeight) + ((rowHeight - child.getMeasuredHeight()) / 2);
        }
        child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
        Log.i(TAG, String.format("Layout child(%d, %d), left:%d, top:%d, w:%d, h:%d",
                page, childIndex, left, top, child.getMeasuredWidth(), child.getMeasuredHeight()));
    }

    private boolean lastTouchOnEdge() {
        return onRightEdgeOfScreen(lastTouchX) || onLeftEdgeOfScreen(lastTouchX);
    }

    @Override
    public boolean onLongClick(View v) {
        int targetPosition = getTargetAtCoor(lastTouchX, lastTouchY);
        if (targetPosition != -1 && enableDrag && !isFullScreen()) {
            container.disableScroll();
            lockableScrollView.setScrollingEnabled(false);

            movingView = true;
            dragged = targetPosition;

            bringDraggedToFront();
            animateDragged();

            return true;
        }

        return false;
    }

    /**
     * Brings the dragged item to front of other items so that the dragged item is shown above others
     */
    private void bringDraggedToFront() {
        View draggedView = getChildView(dragged);
        draggedView.bringToFront();
    }

    /**
     * Brings the fullscreen item to front of other items so that the dragged item is shown above others
     */
    private void bringFullScreenItemToFront() {
        View fullScreenView = getChildView(fullScreenItem);
        fullScreenView.bringToFront();
    }

    private View getDraggedView() {
        return getChildView(dragged);
    }

    /**
     * Scale animation to highlight the dragged item if <code>enableDragAnim</code> is <code>true</code>
     */
    private void animateDragged() {
        ItemPosition itemPos = getItemPositionOf(dragged);
        if (mItemAnimationListener != null && itemPos != null) {
            mItemAnimationListener.onDraggedViewAnimationStart(getChildView(dragged), itemPos.pageIndex, itemPos.itemIndex);
        }
        if (!enableDragAnim) return;
        ScaleAnimation scale = new ScaleAnimation(1f, 1.1f, 1f, 1.1f, displayWidth / 2, ROW_HEIGHT / 2);
        scale.setDuration(DRAGGED_ZOOM_IN_ANIMATION_DURATION);
        scale.setFillAfter(true);
        scale.setFillEnabled(true);

        if (aViewIsDragged()) {
            View draggedView = getDraggedView();
            draggedView.clearAnimation();
            draggedView.startAnimation(scale);
        }
    }

    private boolean aViewIsDragged() {
        return weWereMovingDragged();
    }

    /**
     * Calculates index in <code>views</code> of child at adapter position <code>index</code>,
     * because <code>views</code> just keeps <code>OFFSCREEN_PAGE_LIMIT</code> page's children
     * to save memory,so each position should subtract an <code>offsetPosition</code>
     *
     * @param index
     * @return
     */
    private View getChildView(int index) {
        index -= offsetPosition;
        if (index >= 0 && index < views.size()) {
            return views.get(index);
        } else {
            return null;
        }
    }

    private int getChildViewCount() {
        return views.size();
    }

    public void setContainer(DraggableViewPager container) {
        this.container = container;
    }

    /**
     * Gets adapter position of item at position <code>childIndex</code> of page <code>pageIndex</code>
     *
     * @param pageIndex
     * @param childIndex
     * @return
     */
    private int positionOfItem(int pageIndex, int childIndex) {
        int currentGlobalIndex = 0;
        for (int currentPageIndex = 0; currentPageIndex < adapter.pageCount(); currentPageIndex++) {
            int itemCount = adapter.itemCountInPage(currentPageIndex);
            if (pageIndex != currentPageIndex) {
                currentGlobalIndex += itemCount;
                continue;
            }
            for (int currentItemIndex = 0; currentItemIndex < itemCount; currentItemIndex++) {
                if (pageIndex == currentPageIndex && childIndex == currentItemIndex) {
                    return currentGlobalIndex;
                }
                currentGlobalIndex++;
            }
        }
        return -1;
    }

    /**
     * Gets {@link ItemPosition (pageIndex, itemIndex)} of item at adapter position <code>position</code>
     *
     * @param position
     * @return
     */
    private ItemPosition getItemPositionOf(int position) {
        int currentGlobalIndex = 0;
        for (int currentPageIndex = 0; currentPageIndex < adapter.pageCount(); currentPageIndex++) {
            int itemCount = adapter.itemCountInPage(currentPageIndex);
            if (currentGlobalIndex + itemCount <= position) {
                currentGlobalIndex += itemCount;
                continue;
            }
            for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
                if (currentGlobalIndex == position) {
                    return new ItemPosition(currentPageIndex, itemIndex);
                }
                currentGlobalIndex++;
            }

        }
        return null;
    }

    private void tellAdapterToSwapDraggedWithTarget(int dragged, int target) {
        ItemPosition draggedItemPositionInPage = getItemPositionOf(dragged);
        ItemPosition targetItemPositionInPage = getItemPositionOf(target);
        if (draggedItemPositionInPage != null && targetItemPositionInPage != null) {
            adapter.swapItems(draggedItemPositionInPage.pageIndex, draggedItemPositionInPage.itemIndex, targetItemPositionInPage.itemIndex);
        }
    }

    private void tellAdapterToMoveItemToPreviousPage(int itemIndex) {
        ItemPosition itemPosition = getItemPositionOf(itemIndex);
        adapter.moveItemToPreviousPage(itemPosition.pageIndex, itemPosition.itemIndex);
    }

    private void tellAdapterToMoveItemToNextPage(int itemIndex) {
        ItemPosition itemPosition = getItemPositionOf(itemIndex);
        adapter.moveItemToNextPage(itemPosition.pageIndex, itemPosition.itemIndex);
    }

    /**
     * Sets the vertical scrolling view
     *
     * @param lockableScrollView
     */
    public void setLockableScrollView(LockableScrollView lockableScrollView) {
        this.lockableScrollView = lockableScrollView;
        lockableScrollView.setScrollingEnabled(true);
    }

    /**
     * Updates the cached pages according to the showing <code>newPage</code> and OFFSCREEN_PAGE_LIMIT because of the scrolling action
     *
     * @param newPage the new page is to scroll to
     */
    public void updateCachedPages(int newPage) {
        Log.i(TAG, String.format("UpdateCachedPage:%d", newPage));
        // get the pages to be cached
        List<Integer> newPages = new ArrayList<Integer>();
        for (int i = newPage; i <= newPage + OFFSCREEN_PAGE_LIMIT; i++) {
            if (i < adapter.pageCount()) {
                newPages.add(i);
            } else {
                break;
            }
        }
        for (int i = newPage - 1; i >= newPage - OFFSCREEN_PAGE_LIMIT; i--) {
            if (i >= 0) {
                newPages.add(0, i);
            } else {
                break;
            }
        }
        //if (newPages.isEmpty()) return;
        List<Object> items = new ArrayList<>();
        for (int page : newPages) {
            for (int item = 0; item < adapter.itemCountInPage(page); item++) {
                items.add(adapter.getItemAt(page, item));
            }
        }
        // delete obsolete items
        Iterator<View> iter = views.listIterator();
        while(iter.hasNext()) {
            View view = iter.next();
            Object tag = view.getTag();
            if (!items.contains(tag)) {
                iter.remove();
                removeView(view);
                adapter.destroyItem(tag);
            }
        }
        // add new items
        List<View> newViews = new ArrayList<>();
        for (int page : newPages) {
            for (int item = 0; item < adapter.itemCountInPage(page); item++) {
                View v = null;
                Object obj = adapter.getItemAt(page, item);
                if (indexOfItem(obj) == -1) {
                    v = adapter.view(page, item);
                    v.setTag(adapter.getItemAt(page, item));
                    LayoutParams layoutParams = new LayoutParams((displayWidth - getPaddingLeft() - getPaddingRight()) / adapter.columnCount(), ROW_HEIGHT);
                    addView(v, 0, layoutParams); // to keep the existed items on the top of viewGroup
                } else {
                    for (View oldV : views) {
                        if (oldV.getTag().equals(obj)) {
                            v = oldV;
                            break;
                        }
                    }
                }
                newViews.add(v);
            }
        }
        // 保持缓存的views和adapter内数据顺序一致
        views.clear();
        views.addAll(newViews);

        int newOffset = 0;
        for (int i = 0; newPages.size() > 0 && i < newPages.get(0); i++) {
            newOffset += adapter.itemCountInPage(i);
        }
        offsetPosition = newOffset;
    }

    /**
     * Calculates the real position of items in <code>views</code> and swaps them
     *
     * @param index1
     * @param index2
     */
    private void swapViewsItems(int index1, int index2) {
        int position1 = index1 - offsetPosition;
        int position2 = index2 - offsetPosition;
        if (position1 >= 0 && position1 < views.size() && position2 >= 0 && position2 < views.size()) {
            Collections.swap(views, position1, position2);
        }
        if (onSwapItemListener != null) onSwapItemListener.swapItem(index1, index2);
    }

    /**
     * Item click listener interface for {@link DragDropGrid}
     */
    public interface OnDragDropGridItemClickListener {
        public void onClick(View view, int page, int item);

        public void onDoubleClick(View view, int page, int item);

        public void onFullScreenChange(View view, int page, int item, boolean isFullScreen);
    }

    /**
     * Animation listener interface for {@link DragDropGrid}
     */
    public interface OnDragDropGridItemAnimationListener {
        public void onDraggedViewAnimationStart(View view, int page, int item);

        public void onDraggedViewAnimationEnd(View view, int page, int item);

        public void onFullScreenChangeAnimationStart(View view, int page, int item, boolean toFullScreen);

        public void onFullScreenChangeAnimationEnd(View view, int page, int item, boolean toFullScreen);
    }

    public interface OnSwapItemListener {
        void swapItem(int from, int to);
    }

    private class ItemPosition {
        public int pageIndex;
        public int itemIndex;

        public ItemPosition(int pageIndex, int itemIndex) {
            super();
            this.pageIndex = pageIndex;
            this.itemIndex = itemIndex;
        }
    }
}
