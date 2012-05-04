/*
 Copyright (c) 2011, Sony Ericsson Mobile Communications AB

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 * Neither the name of the Sony Ericsson Mobile Communications AB nor the names
 of its contributors may be used to endorse or promote products derived from
 this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.doubtech.livewatch.googletvremote;

import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.extension.util.SmartWirelessHeadsetProUtil;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.sdk.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;

/**
 * The sample control for Smart Wireless Headset pro handles the control on the
 * accessory. This class exists in one instance for every supported host
 * application that we have registered to.
 */
class SampleControlSmartWirelessHeadsetPro extends ControlExtension {

    private static final Bitmap.Config BITMAP_CONFIG = Bitmap.Config.RGB_565;

    private static final int ANIMATION_DELTA_MS = 500;

    private static final int ANIMATION_DELTA_X_START_VALUE = 5;

    private static final int ANIMATION_DELTA_X_MAX_VALUE = 20;

    private Handler mHandler;

    private boolean mIsShowingAnimation = false;

    private boolean mIsVisible = false;

    private Animation mAnimation = null;

    private final int mWidth;

    private final int mHeight;

    private int mAnimatedTextXPos = 0;

    private int mAnimatedTextXDelta = 0;

    private final String mAnimatedText;

    private TextPaint mTextPaint = null;

    private Rect mAnimatedTextBounds = null;

    /**
     * Create sample control.
     *
     * @param hostAppPackageName Package name of host application.
     * @param context The context.
     * @param handler The handler to use
     */
    SampleControlSmartWirelessHeadsetPro(final String hostAppPackageName, final Context context,
            Handler handler) {
        super(context, hostAppPackageName);
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        mHandler = handler;
        mWidth = getSupportedControlWidth(context);
        mHeight = getSupportedControlHeight(context);

        mAnimatedTextXPos = 0;
        mAnimatedTextXDelta = ANIMATION_DELTA_X_START_VALUE;
        // Create text that will animate in the control.
        mAnimatedText = mContext.getResources().getString(R.string.sample_control_headset_pro_text);

        mTextPaint = SmartWirelessHeadsetProUtil.createTextPaint(mContext);
        mAnimatedTextBounds = new Rect();
        mTextPaint.getTextBounds(mAnimatedText, 0, mAnimatedText.length(), mAnimatedTextBounds);

    }

    /**
     * Get supported control width.
     *
     * @param context The context.
     * @return the width.
     */
    public static int getSupportedControlWidth(Context context) {
        return context.getResources().getDimensionPixelSize(
                R.dimen.headset_pro_control_width);
    }

    /**
     * Get supported control height.
     *
     * @param context The context.
     * @return the height.
     */
    public static int getSupportedControlHeight(Context context) {
        return context.getResources().getDimensionPixelSize(
                R.dimen.headset_pro_control_height);
    }

    @Override
    public void onDestroy() {
        Log.d(SampleExtensionService.LOG_TAG, "SampleControlSmartWirelessHeadsetPro onDestroy");
        stopAnimation();
        mHandler = null;
    };

    @Override
    public void onStart() {
        // Nothing to do. Animation is handled in onResume.
    }

    @Override
    public void onStop() {
        // Nothing to do. Animation is handled in onPause.
    }

    @Override
    public void onResume() {
        mIsVisible = true;

        Log.d(SampleExtensionService.LOG_TAG, "Starting animation");
        startAnimation();
    }

    @Override
    public void onPause() {
        Log.d(SampleExtensionService.LOG_TAG, "Stopping animation");
        mIsVisible = false;

        if (mIsShowingAnimation) {
            stopAnimation();
        }
    }

    @Override
    public void onKey(int action, int keyCode, long timeStamp) {
        if (action != Control.Intents.KEY_ACTION_RELEASE) {
            return;
        }

        switch (keyCode) {
            case Control.KeyCodes.KEYCODE_PREVIOUS:
                // Move animation text left
                pauseAnimation();
                mAnimatedTextXPos -= mAnimatedTextXDelta;
                if (mAnimatedTextXPos < -mAnimatedTextBounds.width()) {
                    mAnimatedTextXPos = -mAnimatedTextBounds.width();
                }
                break;
            case Control.KeyCodes.KEYCODE_NEXT:
                // Move animation text right
                pauseAnimation();
                mAnimatedTextXPos += mAnimatedTextXDelta;
                if (mAnimatedTextXPos > mWidth) {
                    mAnimatedTextXPos = mWidth;
                }
                break;
            case Control.KeyCodes.KEYCODE_VOLUME_UP:
                // Increase animation speed
                mAnimatedTextXDelta += ANIMATION_DELTA_X_START_VALUE;
                if (mAnimatedTextXDelta > ANIMATION_DELTA_X_MAX_VALUE) {
                    mAnimatedTextXDelta = ANIMATION_DELTA_X_MAX_VALUE;
                }
                break;
            case Control.KeyCodes.KEYCODE_VOLUME_DOWN:
                // Decrease animation speed
                mAnimatedTextXDelta -= ANIMATION_DELTA_X_START_VALUE;
                if (mAnimatedTextXDelta < ANIMATION_DELTA_X_START_VALUE) {
                    mAnimatedTextXDelta = ANIMATION_DELTA_X_START_VALUE;
                }
                break;
            case Control.KeyCodes.KEYCODE_PLAY:
                // Play/pause animation
                startOrPauseAnimation();
                break;
            case Control.KeyCodes.KEYCODE_BACK:
                // Stop animation and quit sample control
                stopAnimation();
                break;
            default:
                // no action
                break;
        }
        updateText();
    }

    /**
     * Start a paused animation or pause an ongoing animation.
     */
    private void startOrPauseAnimation() {
        // Animation not showing. Show animation.
        if (mIsShowingAnimation) {
            pauseAnimation();
        } else {
            startAnimation();
        }
    }

    /**
     * Start animation
     */
    private void startAnimation() {
        if (mAnimation != null) {
            mHandler.removeCallbacks(mAnimation);
            mAnimation = null;
        }
        mIsShowingAnimation = true;
        mAnimation = new Animation();
        mAnimation.run();
    }

    /**
     * Pause animation on control.
     */
    public void pauseAnimation() {
        // Stop animation on accessory
        if (mAnimation != null) {
            mHandler.removeCallbacks(mAnimation);
            mAnimation = null;
        }
        mIsShowingAnimation = false;
    }

    /**
     * Stop showing animation and stop control.
     */
    public void stopAnimation() {
        pauseAnimation();
        // If the control is visible then stop it
        if (mIsVisible) {
            stopRequest();
        }
    }

    /**
     * Update the text on the accessory.
     */
    private void updateText() {

        Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, BITMAP_CONFIG);
        // Set the density to default to avoid scaling.
        bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);

        Canvas canvas = new Canvas(bitmap);
        // Black background
        canvas.drawColor(Color.BLACK);

        // Draw text
        canvas.drawText(mAnimatedText, 0, mAnimatedText.length(), mAnimatedTextXPos,
                SmartWirelessHeadsetProUtil.CONFIRM_TEXT_Y, mTextPaint);

        showBitmap(bitmap, 0, 0);
    }

    /**
     * The animation class shows an animation on the accessory. The animation
     * runs until mHandler.removeCallbacks has been called.
     */
    private class Animation implements Runnable {
        /**
         * Create animation.
         */
        Animation() {
        }

        public void run() {

            mAnimatedTextXPos += mAnimatedTextXDelta;
            if (mAnimatedTextXPos > mWidth) {
                mAnimatedTextXPos = -mAnimatedTextBounds.width();
            }
            updateText();
            if (mHandler != null) {
                // Schedule next animation update
                mHandler.postDelayed(this, ANIMATION_DELTA_MS);
            }
        }
    };
}
