/*
 * Copyright 2014 Exodus
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

package com.android.settings.vanir.activities;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import android.renderscript.Allocation;
import android.renderscript.Allocation.MipmapControl;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

public class BackDropActivity extends Activity {

	private static final String TAG = "BackDropActivity";

    private static final int MAX_BLUR_WIDTH = 900;
    private static final int MAX_BLUR_HEIGHT = 1600;
    private static final int UI_OPTIONS = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

	private final Object mLock = new Object();

    private int mBlurRadius = 15;
    private Bitmap mBackgroundBlurImage = null;

    /** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

        Window w = getWindow();
		View decorView = w.getDecorView();
		decorView.setSystemUiVisibility(UI_OPTIONS);

        w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

		final DisplayMetrics metrics = new DisplayMetrics();
		Display display = getWindowManager().getDefaultDisplay();
		display.getRealMetrics(metrics);
        int mWidth = metrics.widthPixels;
        int mHeight = metrics.heightPixels;

		Log.e(TAG, "size.x and size.y " + mWidth + " " + mHeight);

        int orientation = this.getResources().getConfiguration().orientation;
        switch (orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				break;
			case Configuration.ORIENTATION_SQUARE:
			case Configuration.ORIENTATION_PORTRAIT:
			    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				break;
        }

        synchronized(mLock) {
			final Bitmap bmp = SurfaceControl.screenshot(mWidth, mHeight);
			if (bmp != null) {
				mBackgroundBlurImage = blurBitmap(bmp, mBlurRadius);
			} else {
				Log.e(TAG, "onCreate: bitmap was null");
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		synchronized(mLock) {
			if (mBackgroundBlurImage != null) {
				getWindow().setBackgroundDrawable(new BitmapDrawable(mBackgroundBlurImage));
			} else {
				Log.e(TAG, "Failed to set mBackgroundBlurImage because it's null");
				destroy();
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		// override activity entrance animations for now
		this.overridePendingTransition(0, 0);
	}

    @Override
    public void onPause() {
		super.onPause();
		// override activity exit animations for now
		this.overridePendingTransition(0, 0);
		destroy();
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		destroy();
		return super.dispatchTouchEvent(ev);
	}

    private Bitmap blurBitmap(Bitmap bmp, int radius) {
		Bitmap localbmp = bmp;
        Bitmap out = Bitmap.createBitmap(localbmp);

		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        RenderScript rs = RenderScript.create(this);
        Allocation input = Allocation.createFromBitmap(
                rs, localbmp, MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        Allocation output = Allocation.createTyped(rs, input.getType());

        ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        script.setInput(input);
        script.setRadius (radius);
        script.forEach (output);

        output.copyTo (out);
        return out;
    }
    
    private void destroy() {
		synchronized(this) {
			if (mBackgroundBlurImage != null) {
				mBackgroundBlurImage.recycle();
				mBackgroundBlurImage = null;
			}
			this.finish();
		}
	}
}
