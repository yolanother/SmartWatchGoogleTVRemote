package com.doubtech.livewatch.googletvremote.ui;

/*
 * Modifications by Yoni Samlan; based on RealViewSwitcher, whose license is:
 *
 * Copyright (C) 2010 Marc Reichelt
 *
 * Work derived from Workspace.java of the Launcher application
 *  see http://android.git.kernel.org/?p=platform/packages/apps/Launcher.git
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.doubtech.livewatch.googletvremote.SampleExtensionService;
import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlTouchEvent;
import com.sonyericsson.extras.liveware.sdk.R;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

/**
 * A view group that allows users to switch between multiple screens (layouts) in the same way as
 * the Android home screen (Launcher application).
 * <p>
 * You can add and remove views using the normal methods {@link ViewGroup#addView(View)},
 * {@link ViewGroup#removeView(View)} etc. You may want to listen for updates by calling
 * {@link HorizontalPager#setOnScreenSwitchListener(OnScreenSwitchListener)} in order to perform
 * operations once a new screen has been selected.
 *
 * Modifications from original version (ysamlan): Animate argument in setCurrentScreen and duration
 * in snapToScreen; onInterceptTouchEvent handling to support nesting a vertical Scrollview inside
 * the RealViewSwitcher; allowing snapping to a view even during an ongoing scroll; snap to
 * next/prev view on 25% scroll change; density-independent swipe sensitivity; width-independent
 * pager animation durations on scrolling to properly handle large screens without excessively
 * long animations.
 *
 * Other modifications:
 * (aveyD) Handle orientation changes properly and fully snap to the right position.
 *
 * @author Marc Reichelt, <a href="http://www.marcreichelt.de/">http://www.marcreichelt.de/</a>
 * @version 0.1.0
 */
public class HorizontalPager extends ControlExtensionViewGroup {
    /*
     * How long to animate between screens when programmatically setting with setCurrentScreen using
     * the animate parameter
     */
    private static final int ANIMATION_SCREEN_SET_DURATION_MILLIS = 500;
    // What fraction (1/x) of the screen the user must swipe to indicate a page change
    private static final int FRACTION_OF_SCREEN_WIDTH_FOR_SWIPE = 4;
    private static final int INVALID_SCREEN = -1;
    /*
     * Velocity of a swipe (in density-independent pixels per second) to force a swipe to the
     * next/previous screen. Adjusted into mDensityAdjustedSnapVelocity on init.
     */
    private static final int SNAP_VELOCITY_DIP_PER_SECOND = 600;
    // Argument to getVelocity for units to give pixels per second (1 = pixels per millisecond).
    private static final int VELOCITY_UNIT_PIXELS_PER_SECOND = 1000;

    private static final int TOUCH_STATE_REST = 0;
    private static final int TOUCH_STATE_HORIZONTAL_SCROLLING = 1;
    private static final int TOUCH_STATE_VERTICAL_SCROLLING = -1;
    private int mCurrentScreen;
    private int mDensityAdjustedSnapVelocity;
    private boolean mFirstLayout = true;
    private float mLastMotionX;
    private float mLastMotionY;
    private int mMaximumVelocity;
    private int mNextScreen = INVALID_SCREEN;
    private Scroller mScroller;
    private int mTouchSlop;
    private int mTouchState = TOUCH_STATE_REST;
    private VelocityTracker mVelocityTracker;
    private int mLastSeenLayoutWidth = -1;

    /**
     * Simple constructor to use when creating a view from code.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     */
    public HorizontalPager(final Context context, final String hostAppPackageName,
            Handler handler) {
        super(context, hostAppPackageName, handler);
        init();
    }
    
    /**
     * Sets up the scroller and touch/fling sensitivity parameters for the pager.
     */
    private void init() {
        mScroller = new Scroller(getContext());

        // Calculate the density-dependent snap velocity in pixels
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
                .getMetrics(displayMetrics);
        mDensityAdjustedSnapVelocity =
                (int) (displayMetrics.density * SNAP_VELOCITY_DIP_PER_SECOND);

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    @Override
    protected void onLayout(final boolean changed, final int l, final int t, final int r,
            final int b) {
        int childLeft = 0;
        final int count = getChildCount();

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                final int childWidth = child.getMeasuredWidth();
                child.layout(childLeft, 0, childLeft + childWidth, child.getMeasuredHeight());
                childLeft += childWidth;
            }
        }
    }
    
    @Override
    public void onSwipe(int direction) {
        super.onSwipe(direction);
        Log.d("HorizontalPager", "Event: " + direction);
        if(direction == Control.Intents.SWIPE_DIRECTION_RIGHT) {
            setCurrentScreen(Math.min(mCurrentScreen - 1, 0), false);
            Log.d("HorizontalPager", "Swipe left");
        } else if(direction == Control.Intents.SWIPE_DIRECTION_LEFT) {
            setCurrentScreen(mCurrentScreen + 1, false);
            Log.d("HorizontalPager", "Swipe right");
            
        }
    }
    /**
     * Sets the current screen.
     *
     * @param currentScreen The new screen.
     * @param animate True to smoothly scroll to the screen, false to snap instantly
     */
    public void setCurrentScreen(final int currentScreen, final boolean animate) {
        mCurrentScreen = Math.max(0, Math.min(currentScreen, getChildCount() - 1));
        if (animate) {
            //snapToScreen(currentScreen, ANIMATION_SCREEN_SET_DURATION_MILLIS);
        } else {
            scrollTo(mCurrentScreen * getWidth(), 0);
        }
        invalidate();
    }

}