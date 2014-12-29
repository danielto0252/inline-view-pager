package com.danielto.inlineviewpager;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;

public class InlineViewPager extends ViewPager {

    private TypedArray attributes;
    private ViewConfiguration viewConfig;
    private int slop;

    private OnPageChangeListener internalOnPageListener; // fixed action on page scrolled
    private OnPageChangeListener onPageChangeListener; // optional user action

    private float peekingWidth; // the amount you want the items to peek from the side
    private float leftAlignment;
    private float maxWidth;
    private float originalleftAlignment;

    private float startX;
    private float startY;
    private float currentX;
    private float currentY;

    private boolean isDragging;
    private GestureDetector.OnGestureListener gestureListener = new GestureListener();
    private GestureDetector gestureDetector;

    public InlineViewPager(Context context) {
        super(context);

        this.gestureDetector = new GestureDetector(context, gestureListener);
        this.viewConfig = ViewConfiguration.get(context);
        this.slop = ViewConfigurationCompat.getScaledPagingTouchSlop(viewConfig); // get standard android slop for paging
    }

    public InlineViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.gestureDetector = new GestureDetector(context, gestureListener);
        this.viewConfig = ViewConfiguration.get(context);
        this.slop = ViewConfigurationCompat.getScaledPagingTouchSlop(viewConfig); // get standard android slop for paging

        this.attributes = context.obtainStyledAttributes(attrs, R.styleable.InlineViewPager);
        try {
            this.peekingWidth = attributes.getDimension(R.styleable.InlineViewPager_peekingWidth, 10F);
            this.leftAlignment = attributes.getDimension(R.styleable.InlineViewPager_leftAlignment, -1F);
            this.maxWidth = attributes.getDimension(R.styleable.InlineViewPager_maxWidth, -1F);
            this.originalleftAlignment = leftAlignment;
        } finally {
            attributes.recycle();
        }

        // set the peeking width & offscreen limit
        setOffscreenPageLimit(2);
        setPeekingWidth();

        // set the action for page change listener
        setOnPageChangeListener(new OnPageChangeListener() {

            float currentOffset = -1;
            float range = 0.025F; // Represents range of 0.05 for check below

            @Override
            public void onPageScrolled(int pos, float posOffset, int posOffsetPixels) {
                // We want to left align the first item of the pager and center align the other items
                if(pos == 0) {
                    if(leftAlignment != -1F) {
                        if(posOffset > (currentOffset + range) || posOffset < (currentOffset - range) || currentOffset == -1) {
                            int leftPadding = (int)(peekingWidth * posOffset);
                            int rightPadding = (int)((peekingWidth * 2) - (peekingWidth * posOffset));

                            leftPadding += (int)leftAlignment - (leftAlignment * posOffset);
                            rightPadding -= (int)leftAlignment - (leftAlignment * posOffset);

                            // set the padding for the available offscreen items
                            for(int i = 0; i < getChildCount(); i++) {
                                View child = getChildAt(i);
                                child.setPadding(leftPadding, child.getPaddingTop(), rightPadding, child.getPaddingBottom());

                            }
                        }
                    }
                }

                if(onPageChangeListener != null) onPageChangeListener.onPageScrolled(pos, posOffset, posOffsetPixels);
            }

            @Override
            public void onPageSelected(int pos) {
                // adjust all items except the first one which could be left aligned!
                if(getCurrentItem() != 0) {
                    for(int i = 0; i < getChildCount(); i++) {
                        View child = getChildAt(i);
                        child.setPadding((int)peekingWidth, child.getPaddingTop(), (int) peekingWidth, child.getPaddingBottom());
                    }
                }

                // call user added listener if present
                if(onPageChangeListener != null) onPageChangeListener.onPageSelected(pos);

            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // call user added listener if present
                if(onPageChangeListener != null) onPageChangeListener.onPageScrollStateChanged(state);
            }
        });

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // constrain main item to 4:3
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = Math.round((width * 3) / 4);

        heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean handled = true;
        gestureDetector.onTouchEvent(ev);

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDragging = false;
                startX = ev.getX();
                startY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float xDiff, yDiff;

                currentX = ev.getX();
                currentY = ev.getY();

                xDiff = Math.abs(startX - currentX);
                yDiff = Math.abs(startY - currentY);

                if(xDiff > yDiff || yDiff < slop * 2) {
                    isDragging = true;
                } else {
                    handled = false;
                }

                startX = currentX;
                startY = currentY;

                break;
        }

        // disallow parent to intercept the touch if we're paging!
        ViewParent parent = getParent();
        if(parent != null) {
            parent.requestDisallowInterceptTouchEvent(handled || isDragging);
        }
        return super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public void setOnPageChangeListener(OnPageChangeListener listener) {
        if(internalOnPageListener == null) {
            // only happens the first time when the view is created
            internalOnPageListener = listener;
            super.setOnPageChangeListener(listener);
        } else {
            // this is called from the outside by user
            onPageChangeListener = listener;
        }
    }

    @Override
    public void addView(View child) {
        // Set the padding space on each item to offset negative page margins. This will allow each page to align at peeking edges.
        // ** Padding and margin in the child view have no effect on its layout. Edges will always be aligned
        // ** Padding and margin can only be applied to the views inside the child
        // ** To add padding between each item, padding/margin must be added to a view within the child

        if (getCurrentItem() == 0 && leftAlignment > -1) {
            // only set the left alignment spacing for the first child of the pager
            child.setPadding((int) leftAlignment, child.getPaddingTop(), ((int) peekingWidth*2) - (int) leftAlignment, child.getPaddingBottom());
        } else {
            child.setPadding((int)peekingWidth, child.getPaddingTop(), (int) peekingWidth, child.getPaddingBottom());
        }

        child.setBackgroundDrawable(null);
        super.addView(child);
    }

    // Set how much we want items peeking from both sides of the pager
    public void setPeekingWidth(float peekingWidth) {
        this.peekingWidth = peekingWidth;
        setPeekingWidth();
    }

    private void setPeekingWidth() {
        this.setPageMargin((int)peekingWidth * -2);
    }

    class GestureListener implements GestureDetector.OnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            View currentView = findViewWithTag(getCurrentItem());
            if (currentView != null) currentView.dispatchTouchEvent(e);
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            View currentView = findViewWithTag(getCurrentItem());
            if (currentView != null) currentView.dispatchTouchEvent(e);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }
}
