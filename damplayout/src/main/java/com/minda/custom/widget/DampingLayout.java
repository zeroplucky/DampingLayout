package com.minda.custom.widget;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.Scroller;

/**
 * Created by Administrator on 2019/7/9.
 * <p>
 * 注意：1. onTouchEvent 事件的DOWN返回true，
 */
public class DampingLayout extends FrameLayout {

    private String TAG = "dragLayout";

    private View firstView;
    private View secondView;

    private Scroller scroller;
    private VelocityTracker mVelocityTracker;

    private int heightPixels;
    /**
     * 在被判定为滚动之前用户手指可以移动的最大值。
     */
    private int touchSlop;
    private int minFlingVelocity;
    private int maxFlingVelocity;
    /*
    * 是否可以滚动
    * */
    private boolean isScroller = false;

    private int deltaY; // 偏移量
    private float lastY;
    private int maxScrollHeight; // 最大的滑动距离
    private int secondScrollHeight;
    /*
    * 第一个View已经拉到底部， 是否可以再次上拉，拉出第二个View
    * */
    private boolean isDraggable = false;
    /*
    *  第二个View滑出后，能否将第一个View下拉出来
    * */
    private boolean isAbleSlide = false;

    /*
    * 第二个View完全可见
    * */
    private boolean secondItemCompletelyVisible = false;
    private int totalY;
    private boolean direction = true;//true为上滑

    public DampingLayout(Context context) {
        this(context, null);
    }

    public DampingLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DampingLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        scroller = new Scroller(context);

        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        touchSlop = viewConfiguration.getScaledTouchSlop();
        minFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        maxFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        heightPixels = getMeasuredHeight();

        firstView = getChildAt(0);
        secondView = getChildAt(1);

        final int firstViewMeasuredHeight = firstView.getMeasuredHeight();
        final int secondViewMeasuredHeight = secondView.getMeasuredHeight();
        setMeasuredDimension(Math.max(firstView.getMeasuredWidth(), secondView.getMeasuredWidth()),
                firstViewMeasuredHeight + secondViewMeasuredHeight);

        isScroller = heightPixels > firstViewMeasuredHeight ? false : true;
        maxScrollHeight = firstViewMeasuredHeight - heightPixels;
        secondScrollHeight = secondViewMeasuredHeight;
        totalY = maxScrollHeight + secondScrollHeight;

        Log.e(TAG, "onMeasure:  firstViewMeasuredHeight = " + firstViewMeasuredHeight + "  , heightPixels =  "
                + heightPixels + "  , maxScrollHeight = " + maxScrollHeight + "  , secondScrollHeight = "
                + secondScrollHeight + " , totalY = " + totalY + " , isScroller = " + isScroller);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        final int count = getChildCount();
        int childLeft = 0;
        int childTop = 0;
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            final int width = child.getMeasuredWidth();
            final int height = child.getMeasuredHeight();
            child.layout(childLeft, childTop, childLeft + width, childTop + height);
            childLeft += childLeft;
            childTop += childTop + height;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isScroller || secondItemCompletelyVisible) {
            return false;
        }

        switch (ev.getAction()) {

            case MotionEvent.ACTION_DOWN: {
                lastY = ev.getY();

                Log.e(TAG, "onInterceptTouchEvent: ACTION_DOWN -------  , lastY = " + lastY + " ,getScrollY() = " + getScrollY());

                if (!scroller.isFinished()) {
                    scroller.abortAnimation();
                    return true;
                }
                return false;
            }

            case MotionEvent.ACTION_MOVE: {

                Log.e(TAG, "onInterceptTouchEvent: ========= ev.getY() =  " + ev.getY() + "  , lastY = " + lastY + " ,getScrollY() = " + getScrollY());

                return true; // move事件打断，则事件会传递到 onTouchEvent事件的MOVE中
            }

            default:
                return false;
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        createVelocityTracker(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                lastY = event.getY();
                if (!scroller.isFinished()) {
                    scroller.forceFinished(true);
                }
                return true; // down事件为事件序列的第一步，必须拦截，否则up和move事件无法拦截
            }
            case MotionEvent.ACTION_UP: {
                Log.e(TAG, "onTouchEvent:  isDraggable = " + isDraggable + "  ,secondItemCompletelyVisible = " + secondItemCompletelyVisible);

                int scrollY = getScrollY();
                if (scrollY == 0) break;

                // 加偏移值，防止点击即上拉
                if (Math.abs(deltaY) >= touchSlop || scrollY > maxScrollHeight + dp2px(100)) {
                    if (isDraggable) {
                        int offset = totalY - scrollY; // 上滑
                        Log.e(TAG, "onTouchEvent: ACTION_UP---------------  offset = " + offset + "   , totalY = " + totalY);
                        startScroll(offset);
                        break;
                    }
                } else if (Math.abs(deltaY) <= touchSlop || (scrollY <= maxScrollHeight + dp2px(100)
                        && scrollY > maxScrollHeight)) {
                    if (isDraggable) {
                        int offset = maxScrollHeight - scrollY;
                        Log.e(TAG, "onTouchEvent: ACTION_UP================  offset = " + offset + "   , totalY = " + totalY);
                        startScroll(offset);
                        break;
                    }
                }

                int velocity = getScrollVelocity(); // 上滑 velocity < 0 , 下滑 velocity > 0
                Log.e(TAG, "onTouchEvent:  --------->>>>>  velocity = " + velocity + "  , scrollY = " + scrollY);
                if (velocityIsValid(velocity)) {
                    fling(scrollY, -velocity);
                }
                recycleVelocityTracker();

            }
            break;
            case MotionEvent.ACTION_MOVE: {
                deltaY = (int) (event.getY() - lastY);
                Log.e(TAG, "onTouchEvent: before------------ deltaY =  " + deltaY + "  , touchSlop = " + touchSlop);
                if (Math.abs(deltaY) >= touchSlop) {
                    int scrollY = getScrollY(); // 起始值为0，上滑值>0 , 下滑值<0
                    if (scrollY < maxScrollHeight) {
                        isDraggable = false;
                    } else if (scrollY >= maxScrollHeight) {
                        isDraggable = true;
                    }

                    if (!secondItemCompletelyVisible) { // 第二个View完全可见之前的逻辑
                        if (deltaY > 0) { //  deltaY > 0 ,scrollY<0  为下滑

                            direction = false; // 下滑

                            if (deltaY <= scrollY && scrollY > 0) {
                                scrollBy(0, -deltaY);
                                invalidate();
                            }
                        } else if (deltaY < 0) {// deltaY < 0 ,scrollY>0  为上滑

                            direction = true; // 下滑

                            if (scrollY + Math.abs(deltaY) <= maxScrollHeight) { // 和坐标值的正反相反
                                scrollBy(0, -deltaY);
                                invalidate();
                            } else if (isDraggable && scrollY <= maxScrollHeight + dp2px(200)) {
                                scrollBy(0, (int) (-deltaY * 0.3f));
                                invalidate();
                            }

                        }
                    }

                    Log.e(TAG, "onTouchEvent: ================ scrollY =  " + scrollY +
                            "  , deltaY = " + deltaY + "  , maxScrollHeight = " + maxScrollHeight + " ,isDraggable = " + isDraggable);

                    lastY = event.getY();
                }
                return true;
            }
            default:
                return false;
        }
        return super.onTouchEvent(event);
    }

    /*
   * 显示第一个View
   * */
    public void scrollOpen() {
        scroller.startScroll(0, getScrollY(), 0, -getScrollY());
        invalidate();
    }

    @Override
    public void computeScroll() {
        if (scroller != null && !scroller.isFinished()
                && scroller.computeScrollOffset()) {

            int currY = scroller.getCurrY();
            Log.e(TAG, "computeScroll:   currY = " + currY + "  , isDraggable = " + isDraggable);

            //第一次上拉
            if (!isDraggable) {
                if (currY <= maxScrollHeight && currY >= 0) {
                    scrollTo(0, currY);
                    invalidate();
                } else if (currY > maxScrollHeight && getScrollY() != maxScrollHeight) {
                    scrollTo(0, maxScrollHeight);
                    invalidate();
                } else {
                    if (!scroller.isFinished()) {
                        scroller.forceFinished(true);
                    }
                    if (getScrollY() == maxScrollHeight) {
                        isDraggable = true;
                    }
                }
            } else {
                if (currY <= totalY) {
                    scrollTo(0, currY);
                    invalidate();
                }
                // 当第二个View全部显示后，设置为TURE
                if (currY == totalY) {
                    secondItemCompletelyVisible = true;
                }
                //恢复默认值
                if (currY == 0) {
                    secondItemCompletelyVisible = false;
                    isDraggable = false;
                }
            }
        }
    }

    /**
     * 创建VelocityTracker对象，并将触摸事件加入到VelocityTracker当中。
     *
     * @param event 右侧布局监听控件的滑动事件
     */
    private void createVelocityTracker(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
    }


    /**
     * 获取手指在绑定布局上的滑动速度。
     *
     * @return 滑动速度，以每秒钟移动了多少像素值为单位。
     */
    private int getScrollVelocity() {
        mVelocityTracker.computeCurrentVelocity(1000, maxFlingVelocity);
        int velocity = (int) mVelocityTracker.getYVelocity();
        return velocity;
    }

    /**
     * 回收VelocityTracker对象。
     */
    private void recycleVelocityTracker() {
        mVelocityTracker.recycle();
        mVelocityTracker = null;
    }

    /*
    * 速度是否可用
    * */
    private boolean velocityIsValid(int velocity) {
        int absVelocity = Math.abs(velocity);
        return absVelocity > minFlingVelocity && absVelocity < maxFlingVelocity;
    }

    /*
    * 滑动View
    * */
    private void fling(int currY, int velocityY) {
        if (!scroller.isFinished()) {
            return;
        }
        scroller.fling(
                0, currY,
                0, velocityY, //velocity
                0, 0,
                -maxScrollHeight * 10, maxScrollHeight * 10); //y
        if (scroller.computeScrollOffset()) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /*
    *
    * */
    private void startScroll(int offSetY) {
        scroller.startScroll(0, getScrollY(), 0, offSetY);
        invalidate();
    }


    private static int dp2px(final float dpValue) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private static int px2dp(final float pxValue) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }


}
