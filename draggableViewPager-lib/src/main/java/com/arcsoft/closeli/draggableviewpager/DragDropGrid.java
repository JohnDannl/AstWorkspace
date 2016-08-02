package com.arcsoft.closeli.draggableviewpager;

import android.animation.Animator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
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
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DragDropGrid extends ViewGroup implements OnTouchListener, OnLongClickListener {

    public static int ROW_HEIGHT = 300;
    private static int DRAGGED_MOVE_ANIMATION_DURATION = 200;
    private static int DRAGGED_ZOOM_IN_ANIMATION_DURATION = 200;
    private static int FULLSCREEN_ANIMATION_DURATION=10;
    private static int OFFSCREEN_PAGE_LIMIT = 1;
    private List<Integer> loadedPages = new ArrayList<Integer>();
    private static final long DOUBLE_CLICK_INTERVAL = 250; // in millis
    private boolean hasDoubleClick = false;
    private long lastClickTime = 0L;
    private int lastClickItem = -1;
    private int fullScreenItem = -1;
    private boolean hasFullScreen = false;
    private boolean enableDrag = true;
    private boolean enableDragAnim = false;
    private boolean enableFullScreen = false;
    private static final boolean noStatusBar = true;
    private int offsetPosition = 0; // the index of firs item in views,support a small size views and dynamic store address

    private static int EGDE_DETECTION_MARGIN = 35;
    private DraggableViewPagerAdapter adapter;
    private OnDragDropGridItemClickListener onItemClickListener = null;
    private OnDragDropGridItemAnimationListener mItemAnimationListener = null;
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
     * The width of the screen.
     */
    private int displayWidth;
    /**
     * The height of the screen.
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

    private void getDisplayDimensions() {
        final WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        final Display display = wm.getDefaultDisplay();
        final Point point = new Point();
        display.getSize(point);
        displayWidth = point.x;
        displayHeight = point.y;
        if(noStatusBar){
            ROW_HEIGHT = (displayHeight - getPaddingTop() - getPaddingBottom()) / 2;
        }else{
            int statusHeight = getStatusBarHeight();
            ROW_HEIGHT = (displayHeight - getPaddingTop() - getPaddingBottom() - statusHeight) / 2;
        }
    }
    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
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

            @Override
            public int itemCountInPage(int page) {
                return 0;
            }

            @Override
            public void deleteItem(int pageIndex, int itemIndex) {

            }

            @Override
            public int columnCount() {
                return 0;
            }

            @Override
            public int getPageWidth(int page) {
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

    public void setDragEnabled(boolean enabled){
        this.enableDrag=enabled;
    }

    public void setDragZoomInAnimEnabled(boolean enabled){
        enableDragAnim=enabled;
    }

    public void setItemDoubleClickFullScreenEnabled(boolean enabled){
        enableFullScreen=enabled;
    }

    private void addChildViews() {
        offsetPosition = 0;
        for (int page = 0; page <= OFFSCREEN_PAGE_LIMIT; page++) {
            for (int item = 0; item < adapter.itemCountInPage(page); item++) {
            View v = adapter.view(page, item);
            v.setTag(adapter.getItemAt(page, item));
            LayoutParams layoutParams = new LayoutParams((displayWidth - getPaddingLeft() - getPaddingRight())/adapter.columnCount(),ROW_HEIGHT);
            addView(v, layoutParams);
            views.add(v);
            }
            loadedPages.add(page);
        }
    }

    public void reloadViews() {
        for (int page = 0; page < adapter.pageCount(); page++) {
            for (int item = 0; item < adapter.itemCountInPage(page); item++) {
                if (indexOfItem(page, item) == -1) {
                    View v = adapter.view(page, item);
                    v.setTag(adapter.getItemAt(page, item));
                    addView(v);
                }
            }
        }
    }

    public int indexOfItem(int page, int index) {
        Object item = adapter.getItemAt(page, index);

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
            View v = (View) this.getChildAt(i);
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
        if (aViewIsDragged()){
            return true;
        }
        return false;
    }

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

    private void touchUp(MotionEvent event) {
        if (!aViewIsDragged()) {
            long clickTime=event.getEventTime();
            final int childIndex=getTargetAtCoor((int) event.getX(), (int) event.getY());
            if(clickTime-lastClickTime<=DOUBLE_CLICK_INTERVAL&&childIndex==lastClickItem){
                hasDoubleClick=true;
                if(enableFullScreen){
                    onItemDoubleClick(childIndex);
                }
            }else{
                hasDoubleClick=false;
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(!hasDoubleClick){
                            onItemClick(childIndex);
                        }
                    }
                },DOUBLE_CLICK_INTERVAL);
            }
            lastClickTime=clickTime;
            lastClickItem=childIndex;
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
    private void onItemClick(int childIndex){
        if (onItemClickListener != null) {
            ItemPosition itemPosition= getItemPositionOf(childIndex);
            View clickedView = getChildView(childIndex);
            if (clickedView != null&&itemPosition!=null){
                onItemClickListener.onClick(clickedView,itemPosition.pageIndex,itemPosition.itemIndex);
            }else{
                onItemClickListener.onClick(null,-1,-1);
            }

        }
    }
    private void onItemDoubleClick(int childIndex){
        ItemPosition itemPosition= getItemPositionOf(childIndex);
        View clickedView = getChildView(childIndex);

        if(!hasFullScreen&&clickedView != null&&itemPosition!=null){
            fullScreenItem =childIndex;
            hasFullScreen=true;
            //extendToFullScreen();
            //extendToFullScreenWithViewAnimation();
            extendToFullScreenWithValueAnimation();
            if(onItemClickListener != null&&clickedView != null&&itemPosition!=null){
                onItemClickListener.onFullScreenChange(clickedView, itemPosition.pageIndex, itemPosition.itemIndex,true);
            }
        }else{
            //shrinkToNormalScreen();
            //shrinkToNormalScreenWithViewAnimation();
            shrinkToNormalScreenWithValueAnimation();
            ItemPosition shrinkItemPosition= getItemPositionOf(fullScreenItem);
            View shrinkItemView = getChildView(fullScreenItem);
            if(onItemClickListener != null&&shrinkItemView != null&&shrinkItemPosition!=null){
                onItemClickListener.onFullScreenChange(shrinkItemView, shrinkItemPosition.pageIndex, shrinkItemPosition.itemIndex,false);
            }
            //fullScreenItem = -1;      // these two lines should execute after the animation finished
            //hasFullScreen=false;
        }

        if(onItemClickListener != null){
            if (clickedView != null&&itemPosition!=null) {
                onItemClickListener.onDoubleClick(clickedView, itemPosition.pageIndex, itemPosition.itemIndex);
            }else{
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

    private void extendToFullScreen(){
        container.disableScroll();
        lockableScrollView.setScrollingEnabled(false);

        if (fullScreenItem != -1) {
            View fullScreenView = getChildView(fullScreenItem);
            LayoutParams vlp=fullScreenView.getLayoutParams();
            vlp.width=displayWidth;
            vlp.height=displayHeight;
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
    private void shrinkToNormalScreen(){
        if (fullScreenItem != -1) {
            /*int page=currentPage();
            for(int i=0;i<adapter.itemCountInPage(page);i++){
                int position=positionOfItem(page,i);
                if(position!=fullScreenItem){
                    View childView=getChildView(position);
                    LayoutParams cvlp =childView.getLayoutParams();
                    cvlp.width=(displayWidth - getPaddingLeft() - getPaddingRight())/adapter.columnCount();
                    cvlp.height=ROW_HEIGHT;
                    childView.setLayoutParams(cvlp);
                    //childView.setVisibility(View.VISIBLE);
                }
            }*/

            View fullScreenView = getChildView(fullScreenItem);
            LayoutParams vlp =fullScreenView.getLayoutParams();
            vlp.width=(displayWidth - getPaddingLeft() - getPaddingRight())/adapter.columnCount();
            vlp.height=ROW_HEIGHT;
            fullScreenView.setLayoutParams(vlp);
        }

        lockableScrollView.setScrollingEnabled(true);
        container.enableScroll();
    }
    private void extendToFullScreenWithViewAnimation(){
        container.disableScroll();
        lockableScrollView.setScrollingEnabled(false);

        if (fullScreenItem != -1) {
            View fullView=getChildView(fullScreenItem);
            bringFullScreenItemToFront();
            int width=fullView.getMeasuredWidth();
            int height=fullView.getMeasuredHeight();
            int left=0;
            int top=0;
            switch(getItemPositionOf(fullScreenItem).itemIndex){
                case 0:
                    left=0;
                    top=0;
                    break;
                case 1:
                    left=width;
                    top=0;
                    break;
                case 2:
                    left=0;
                    top=height;
                    break;
                case 3:
                    left=width;
                    top=height;
                    break;
                default:
                    break;
            }
            ScaleAnimation scale = new ScaleAnimation(1f, 2.0f, 1f, 2.0f, Animation.ABSOLUTE,left,Animation.ABSOLUTE,top);
            scale.setDuration(FULLSCREEN_ANIMATION_DURATION);
            scale.setFillAfter(true);
            scale.setFillEnabled(true);
            fullView.clearAnimation();
            fullView.startAnimation(scale);
        }
    }
    private void shrinkToNormalScreenWithViewAnimation(){
        if(fullScreenItem!=-1){
            View fullView=getChildView(fullScreenItem);
            //fullView.clearAnimation();
            int width=fullView.getMeasuredWidth();
            int height=fullView.getMeasuredHeight();
            int left=0;
            int top=0;
            switch(getItemPositionOf(fullScreenItem).itemIndex){
                case 0:
                    left=0;
                    top=0;
                    break;
                case 1:
                    left=width;
                    top=0;
                    break;
                case 2:
                    left=0;
                    top=height;
                    break;
                case 3:
                    left=width;
                    top=height;
                    break;
                default:
                    break;
            }
            ScaleAnimation scale = new ScaleAnimation(2f, 1.0f, 2f, 1.0f, Animation.ABSOLUTE,left,Animation.ABSOLUTE,top);
            scale.setDuration(FULLSCREEN_ANIMATION_DURATION);
            scale.setFillAfter(true);
            scale.setFillEnabled(true);
            fullView.clearAnimation();
            fullView.startAnimation(scale);
        }
        lockableScrollView.setScrollingEnabled(true);
        container.enableScroll();
    }
    private void extendToFullScreenWithValueAnimation(){
        container.disableScroll();
        lockableScrollView.setScrollingEnabled(false);

        if (fullScreenItem != -1) {
            final View fullView=getChildView(fullScreenItem);
            bringFullScreenItemToFront();
            final LayoutParams oldLayoutParam=fullView.getLayoutParams();
            final LayoutParams newLayoutParam=new LayoutParams(displayWidth,displayHeight);
            layoutAnimateScale(oldLayoutParam,newLayoutParam,fullView,true);
        }
    }
    private void layoutAnimateScale( LayoutParams oldLayoutParam, LayoutParams newLayoutParam,
                                     final View fullView,final boolean toFullScreen){
        ValueAnimator layoutAnim=ValueAnimator.ofObject(new TypeEvaluator() {
            @Override
            public Object evaluate(float fraction, Object startValue, Object endValue) {
                float width=((LayoutParams)startValue).width
                        +fraction*(((LayoutParams)endValue).width-((LayoutParams)startValue).width);
                float height=((LayoutParams)startValue).height
                        +fraction*(((LayoutParams)endValue).height-((LayoutParams)startValue).height);
                LayoutParams tmpLayoutParam=new LayoutParams((int)width,(int)height);
                return tmpLayoutParam;
            }
        },oldLayoutParam,newLayoutParam);
        layoutAnim.setDuration(FULLSCREEN_ANIMATION_DURATION).addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                LayoutParams tmpLayoutParam=(LayoutParams)animation.getAnimatedValue();
                LayoutParams layoutParam=fullView.getLayoutParams();
                layoutParam.width=tmpLayoutParam.width;
                layoutParam.height=tmpLayoutParam.height;
                fullView.setLayoutParams(layoutParam);
            }
        });
        layoutAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                ItemPosition itemPos= getItemPositionOf(fullScreenItem);
                if(mItemAnimationListener!=null&&itemPos!=null){
                    mItemAnimationListener.onFullScreenChangeAnimationStart(fullView,itemPos.pageIndex,itemPos.itemIndex,toFullScreen);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                ItemPosition itemPos= getItemPositionOf(fullScreenItem);
                if(mItemAnimationListener!=null&&itemPos!=null){
                    mItemAnimationListener.onFullScreenChangeAnimationEnd(fullView,itemPos.pageIndex,itemPos.itemIndex,toFullScreen);
                }
                if(!toFullScreen){
                    fullScreenItem = -1;      // these two lines should execute after the animation finished
                    hasFullScreen=false;
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
    private void shrinkToNormalScreenWithValueAnimation(){
        if(fullScreenItem!=-1){
            View fullView=getChildView(fullScreenItem);
            final LayoutParams oldLayoutParam=fullView.getLayoutParams();
            final LayoutParams newLayoutParam=new LayoutParams(columnWidth, rowHeight);
            layoutAnimateScale(oldLayoutParam,newLayoutParam,fullView,false);
        }
        lockableScrollView.setScrollingEnabled(true);
        container.enableScroll();
    }
    private void restoreDraggedItem() {
        Point targetCoor=getCoorForIndex(dragged);
        View targetView=getChildView(dragged);
        targetView.layout(targetCoor.x,targetCoor.y,
                targetCoor.x+targetView.getMeasuredWidth(),targetCoor.y+targetView.getMeasuredHeight());
        if(mItemAnimationListener!=null){
            ItemPosition itemPos=getItemPositionOf(dragged);
            mItemAnimationListener.onDraggedViewAnimationEnd(targetView,itemPos.pageIndex,itemPos.itemIndex);
        }
    }

    private void tellAdapterDraggedIsDeleted(Integer newDraggedPosition) {
        ItemPosition position = getItemPositionOf(newDraggedPosition);
        adapter.deleteItem(position.pageIndex, position.itemIndex);
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


    private void moveDraggedView(int x, int y) {
        View childAt = getDraggedView();

        int width = childAt.getMeasuredWidth();
        int height = childAt.getMeasuredHeight();

        int l = x - (1 * width / 2);
        int t = y - (1 * height / 2);

        childAt.layout(l, t, l + width, t + height);
    }

    private void manageSwapPosition(int x, int y) {
        int target = getTargetAtCoor(x, y);
        if (childHasMoved(target) && target != lastTarget) {
            animateGap(target);
            lastTarget = target;
        }
    }

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
        if (!aViewIsDragged())return; // error occurs if dragged equals -1
        tellAdapterToMoveItemToNextPage(dragged);
        moveDraggedToNextPage();

        container.scrollRight();
        stopAnimateOnTheEdge();
        lockableScrollView.scrollTo(0, 0);
    }

    private void scrollToPreviousPage() {
        if (!aViewIsDragged())return;
        tellAdapterToMoveItemToPreviousPage(dragged);
        moveDraggedToPreviousPage();

        container.scrollLeft();
        stopAnimateOnTheEdge();
        lockableScrollView.scrollTo(0, 0);
    }

    private void moveDraggedToPreviousPage() {
        final Point draggedViewCoor = getCoorForIndex(dragged);
        int indexFirstElementInCurrentPage = findTheIndexOfFirstElementInCurrentPage();
        int indexOfDraggedOnNewPage = indexFirstElementInCurrentPage - 1;
        final int targetIndex=indexOfDraggedOnNewPage;
        final View targetView=getChildView(targetIndex);
        final Point targetViewCoor = getCoorForIndex(targetIndex);
        swapViewsItems(targetIndex, dragged);
        dragged=targetIndex;
        animateMoveView(draggedViewCoor,targetViewCoor,targetView);
    }

    private void moveDraggedToNextPage() {
        final Point draggedViewCoor = getCoorForIndex(dragged);
        int indexFirstElementInNextPage = findTheIndexFirstElementInNextPage();
        int indexOfDraggedOnNewPage = indexFirstElementInNextPage;
        int targetIndex=indexOfDraggedOnNewPage;
        final View targetView = getChildView(targetIndex);
        final Point targetViewCoor = getCoorForIndex(targetIndex);
        swapViewsItems(targetIndex, dragged);
        dragged=targetIndex;
        animateMoveView(draggedViewCoor,targetViewCoor,targetView);
    }

    private int findTheIndexOfFirstElementInCurrentPage() {
        int currentPage = currentPage();
        int indexFirstElementInCurrentPage = 0;
        for (int i = 0; i < currentPage; i++) {
            indexFirstElementInCurrentPage += adapter.itemCountInPage(i);
        }
        return indexFirstElementInCurrentPage;
    }

    private int findTheIndexLastElementInNextPage() {
        int currentPage = currentPage();
        int indexLastElementInNextPage = 0;
        for (int i = 0; i <= currentPage + 1; i++) {
            indexLastElementInNextPage += adapter.itemCountInPage(i);
        }
        return indexLastElementInNextPage;
    }
    private int findTheIndexFirstElementInNextPage() {
        int currentPage = currentPage();
        int indexFirstElementInNextPage = 0;
        for (int i = 0; i <= currentPage; i++) {
            indexFirstElementInNextPage += adapter.itemCountInPage(i);
        }
        return indexFirstElementInNextPage;
    }

    private boolean onLeftEdgeOfScreen(int x) {
        int currentPage = container.currentPage();

        int leftEdgeXCoor = currentPage * gridPageWidth;
        int distanceFromEdge = x - leftEdgeXCoor;
        return (x > 0 && distanceFromEdge <= EGDE_DETECTION_MARGIN);
    }

    private boolean onRightEdgeOfScreen(int x) {
        int currentPage = container.currentPage();

        int rightEdgeXCoor = (currentPage * gridPageWidth) + gridPageWidth;
        int distanceFromEdge = rightEdgeXCoor - x;
        return (x > (rightEdgeXCoor - EGDE_DETECTION_MARGIN)) && (distanceFromEdge < EGDE_DETECTION_MARGIN);
    }

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
        dragged=viewAtPosition;

        /*targetView.layout(draggedViewCoor.x,draggedViewCoor.y,
                draggedViewCoor.x+targetView.getMeasuredWidth(),draggedViewCoor.y+targetView.getMeasuredHeight());*/
        animateMoveView(draggedViewCoor,targetViewCoor,targetView);
    }

    private void animateMoveView(Point draggedViewCoor, Point targetViewCoor, final View targetView){
        ValueAnimator animMove=ValueAnimator.ofObject(new TypeEvaluator() {
            @Override
            public Object evaluate(float fraction, Object startValue, Object endValue) {
                float x=((Point)startValue).x+fraction*(((Point)endValue).x-((Point)startValue).x);
                float y=((Point)startValue).y+fraction*(((Point)endValue).y-((Point)startValue).y);
                return new Point((int)x,(int)y);
            }
        },targetViewCoor,draggedViewCoor);
        animMove.setDuration(DRAGGED_MOVE_ANIMATION_DURATION).addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Point layoutCoor=(Point)animation.getAnimatedValue();
                targetView.layout(layoutCoor.x,layoutCoor.y,
                        layoutCoor.x+targetView.getMeasuredWidth(),layoutCoor.y+targetView.getMeasuredHeight());
            }
        });
        animMove.start();
    }

    private Point getCoorForIndex(int index) {
        ItemPosition page = getItemPositionOf(index);

        int row = page.itemIndex / columnCount;
        int col = page.itemIndex - (row * columnCount);

        int x = (currentPage() * gridPageWidth) + (columnWidth * col);
        int y = rowHeight * row;

        return new Point(x, y);
    }

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

        if (adapter.getPageWidth(currentPage()) != 0) {
            widthSize = adapter.getPageWidth(currentPage());
        }

        gridPageWidth = widthSize;
        return widthSize;
    }
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        //If we don't have pages don't do layout
        if (adapter.pageCount() == 0)
            return;
        int pageWidth = (r-l) / adapter.pageCount();

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
        if (fullScreenItem!=-1){
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

        if (child == null || child.getVisibility()==View.GONE)return;

        int left = 0;
        int top = 0;
        if (position == dragged && lastTouchOnEdge()) {
            left = computePageEdgeXCoor(child);
            top = lastTouchY - (child.getMeasuredHeight() / 2);
        } else if (position == fullScreenItem){
            switch(getItemPositionOf(fullScreenItem).itemIndex){
                case 0:
                    left=page*pageWidth;
                    top=0;
                    break;
                case 1:
                    left=(page+1)*pageWidth - child.getMeasuredWidth();
                    top=0;
                    break;
                case 2:
                    left=page*pageWidth;
                    top=displayHeight-child.getMeasuredHeight();
                    break;
                case 3:
                    left=(page+1)*pageWidth - child.getMeasuredWidth();
                    top=displayHeight-child.getMeasuredHeight();
                    break;
                default:
                    break;
            }
        } else {
            left = (page * pageWidth) + (col * columnWidth) + ((columnWidth - child.getMeasuredWidth()) / 2);
            top = (row * rowHeight) + ((rowHeight - child.getMeasuredHeight()) / 2);
        }
        child.layout(left, top, left + child.getMeasuredWidth(),top + child.getMeasuredHeight());
    }

    private boolean lastTouchOnEdge() {
        return onRightEdgeOfScreen(lastTouchX) || onLeftEdgeOfScreen(lastTouchX);
    }

    private int computePageEdgeXCoor(View child) {
        int left;
        left = lastTouchX - (child.getMeasuredWidth() / 2);
        if (onRightEdgeOfScreen(lastTouchX)) {
            left = left - gridPageWidth;
        } else if (onLeftEdgeOfScreen(lastTouchX)) {
            left = left + gridPageWidth;
        }
        return left;
    }

    @Override
    public boolean onLongClick(View v) {
        int targetPosition = getTargetAtCoor(lastTouchX, lastTouchY);
        if (targetPosition != -1&&enableDrag&&!isFullScreen()) {
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

    private void bringDraggedToFront() {
        View draggedView = getChildView(dragged);
        draggedView.bringToFront();
    }
    private void bringFullScreenItemToFront() {
        View fullScreenView = getChildView(fullScreenItem);
        fullScreenView.bringToFront();
    }
    private View getDraggedView() {
        return getChildView(dragged);
    }

    private void animateDragged() {
        ItemPosition itemPos=getItemPositionOf(dragged);
        if (mItemAnimationListener != null && itemPos != null){
            mItemAnimationListener.onDraggedViewAnimationStart(getChildView(dragged),itemPos.pageIndex,itemPos.itemIndex);
        }
        if (!enableDragAnim)return;
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

    private View getChildView(int index) {
        index -= offsetPosition;
        if (index>=0&&index<views.size()){
            return views.get(index);
        }else{
            return null;
        }
    }
    private int getChildViewCount() {
        return views.size();
    }
    public void setContainer(DraggableViewPager container) {
        this.container = container;
    }

    private int positionOfItem(int pageIndex, int childIndex) {
        int currentGlobalIndex = 0;
        for (int currentPageIndex = 0; currentPageIndex < adapter.pageCount(); currentPageIndex++) {
            int itemCount = adapter.itemCountInPage(currentPageIndex);
            if (pageIndex != currentPageIndex){
                currentGlobalIndex+=itemCount;
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

    private ItemPosition getItemPositionOf(int position) {
        int currentGlobalIndex = 0;
        for (int currentPageIndex = 0; currentPageIndex < adapter.pageCount(); currentPageIndex++) {
            int itemCount = adapter.itemCountInPage(currentPageIndex);
            if (currentGlobalIndex + itemCount <= position){
                currentGlobalIndex += itemCount;
                continue;
            }
            for(int itemIndex = 0; itemIndex < itemCount; itemIndex++){
                if (currentGlobalIndex == position){
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

    public void setLockableScrollView(LockableScrollView lockableScrollView) {
        this.lockableScrollView = lockableScrollView;
        lockableScrollView.setScrollingEnabled(true);
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

    /**
     * Moves to show newPage and updates the cached pages according to OFFSCREEN_PAGE_LIMIT
     * @param newPage the new page to move to
     * @param toLeft whether scrolls to left
     * @param toRight whether scrolls to right
     */
    public void updateCachedPages(int newPage, boolean toLeft, boolean toRight) {
        if (!toLeft && !toRight) return;
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
                newPages.add(0,i);
            } else {
                break;
            }
        }
        if (newPages.isEmpty()) return;
        if (toLeft) {
            int oldSize = loadedPages.size();
            int oldPageMin = loadedPages.get(0);
            int newPageMax = newPages.get(newPages.size() - 1);
            // removes obsolete pages which not included in newPages
            for (int i = oldSize - 1; i >= 0; i--) {
                if (!newPages.contains(loadedPages.get(i))) { // remove from right to left
                    removeChildViewsOfPage(loadedPages.get(i));
                } else {
                    break;
                }
            }
            // adjusts the offsetPosition for example, 4,5,6 <= 8,9,10
            if (oldPageMin - 1 > newPageMax) {
                for (int page = oldPageMin - 1; page > newPageMax; page--) {
                    offsetPosition -= adapter.itemCountInPage(page);
                }
            }
            // adds new pages which not included in loadedPages
            for (int i = newPages.size() - 1; i >= 0; i--) {
                if (!loadedPages.contains(newPages.get(i))) {
                    addChildViewsOfPage(newPages.get(i));
                }
            }
        } else if (toRight){
            int oldSize = loadedPages.size();
            int oldPageMax = loadedPages.get(oldSize - 1);
            int newPageMin = newPages.get(0);
            // removes obsolete pages which not included in newPages
            for (int i = 0; i < oldSize; i++) {
                if (!newPages.contains(loadedPages.get(0))) {
                    removeChildViewsOfPage(loadedPages.get(0));
                } else {
                    break;
                }
            }
            // adjusts the offsetPosition for example, 4,5,6 => 8,9,10
            if (oldPageMax + 1 < newPageMin) {
                for (int page = oldPageMax + 1; page < newPageMin; page++) {
                    offsetPosition += adapter.itemCountInPage(page);
                }
            }
            // adds new pages which not included in loadedPages
            for (int i = 0; i < newPages.size(); i++) {
                if (!loadedPages.contains(newPages.get(i))) {
                    addChildViewsOfPage(newPages.get(i));
                }
            }
        }
        /*if (toLeft) {
            if (newPage + OFFSCREEN_PAGE_LIMIT + 1 < adapter.pageCount()) {
                removeChildViewsOfPage(newPage + OFFSCREEN_PAGE_LIMIT + 1);
            }
            if (newPage - OFFSCREEN_PAGE_LIMIT >= 0) {
                addChildViewsOfPage(newPage - OFFSCREEN_PAGE_LIMIT);
            }
        } else if (toRight){
            if (newPage - OFFSCREEN_PAGE_LIMIT - 1 >= 0) {
                removeChildViewsOfPage(newPage - OFFSCREEN_PAGE_LIMIT - 1);
            }
            if (newPage + OFFSCREEN_PAGE_LIMIT < adapter.pageCount()) {
                addChildViewsOfPage(newPage + OFFSCREEN_PAGE_LIMIT);
            }
        }*/
    }

    /**
     * removes items of certain page in a certain order
     * @param page
     */
    private void removeChildViewsOfPage(int page) {
        if (!loadedPages.contains(page)) return;;
        int firstIndex = positionOfItem(page, 0);
        if (firstIndex - offsetPosition == 0) {     // remove from the left to right
            for (int i = firstIndex; i < firstIndex + adapter.itemCountInPage(page); i++) {
                removeView(getChildView(i));
                removeFromViews(i);
            }
            loadedPages.remove(0);
        } else {        // remove from the right to left
            int lastIndex = firstIndex + adapter.itemCountInPage(page) - 1;
            for (int i = lastIndex; i >= firstIndex; i--) {
                removeView(getChildView(i));
                removeFromViews(i);
            }
            loadedPages.remove(loadedPages.size() - 1);
        }
        tellAdapterPageIsDestroyed(page);
        String leftPages = "";
        for (int i : loadedPages) {
            leftPages += i + ",";
        }
        android.util.Log.d("XXXX","remove page:" + page+", views:"+getChildViewCount()+", child:"+getChildCount()+" ,remain:"+leftPages);
    }

    /**
     * removes items of certain page in a certain order
     * @param page
     */
    private void addChildViewsOfPage(int page) {
        if (loadedPages.contains(page)) return;
        int firstIndex = positionOfItem(page, 0);
        if (firstIndex - offsetPosition == views.size()) {    // add items at the end of views
            for (int i = firstIndex, item = 0; i < firstIndex + adapter.itemCountInPage(page); i++, item++) {
                View v = adapter.view(page, item);
                v.setTag(adapter.getItemAt(page, item));
                LayoutParams layoutParams = new LayoutParams((displayWidth - getPaddingLeft() - getPaddingRight())/adapter.columnCount(),ROW_HEIGHT);
                addToViews(i,v);
                addView(v, 0, layoutParams); // to keep the existed items on the top of viewGroup
            }
            loadedPages.add(page);
        } else {    // add items at the beginning of views
            int lastIndex = firstIndex + adapter.itemCountInPage(page) -1;
            for (int i = lastIndex, item = adapter.itemCountInPage(page) -1; i >= firstIndex; i--, item--) {
                View v = adapter.view(page, item);
                v.setTag(adapter.getItemAt(page, item));
                LayoutParams layoutParams = new LayoutParams((displayWidth - getPaddingLeft() - getPaddingRight())/adapter.columnCount(),ROW_HEIGHT);
                addToViews(i,v);
                addView(v, 0, layoutParams); // to keep the existed items on the top of viewGroup
            }
            loadedPages.add(0,page);
        }
        String leftPages = "";
        for (int i : loadedPages) {
            leftPages += i + ",";
        }
        android.util.Log.d("XXXX","add page:" + page+", views:"+getChildViewCount()+", child:"+getChildCount()+" ,remain:"+leftPages);
    }

    /**
     * Calculates the real restore position of item and inserts into views
     * @param index
     * @param v
     */
    private void addToViews(int index, View v) {
        int position = index - offsetPosition;
        if (position == -1) {     // add at the beginning, update the offset
            views.add(0, v);
            offsetPosition--;
        } else if (position == views.size()) {      // add at the end
            views.add(v);
        }
    }

    /**
     * Calculates the real restore position of item and removes from views
     * @param index
     */
    private void removeFromViews(int index) {
        int position = index - offsetPosition;
        if (position == 0) {        // remove the beginning item ,update the offset
            views.remove(0);
            offsetPosition++;
        } else if (position == views.size()-1) {
            views.remove(position);
        }
    }

    /**
     * Calculate the restore position of items and swaps them
     * @param index1
     * @param index2
     */
    private void swapViewsItems(int index1, int index2){
        int position1 = index1 - offsetPosition;
        int position2 = index2 - offsetPosition;
        if (position1 >=0 && position1 < views.size() && position2 >=0 && position2 < views.size()) {
            Collections.swap(views, position1, position2);
        }
    }
    public interface OnDragDropGridItemClickListener {
        public void onClick(View view, int page, int item);
        public void onDoubleClick(View view, int page, int item);
        public void onFullScreenChange(View view, int page, int item, boolean isFullScreen);
    }
    public interface OnDragDropGridItemAnimationListener {
        public void onDraggedViewAnimationStart(View view, int page, int item);
        public void onDraggedViewAnimationEnd(View view, int page, int item);
        public void onFullScreenChangeAnimationStart(View view, int page, int item, boolean toFullScreen);
        public void onFullScreenChangeAnimationEnd(View view, int page, int item, boolean toFullScreen);
    }
}
