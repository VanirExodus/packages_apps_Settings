/*
 * Copyright (C) 2014 AOKP
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

package com.android.settings.exodus.fragments;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.IntervalSeekBar;

public class AnimSpeedBarPreference extends Preference
    implements SeekBar.OnSeekBarChangeListener {

    private Context mContext;

    private TextView mMonitorBox;
    private IntervalSeekBar mSeekBar;

    private float mScale = 1.0f;

    float defaultValue = 85;

    private OnPreferenceChangeListener changer;

    public AnimSpeedBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        mContext = getContext();
        View layout = View.inflate(mContext, R.layout.preference_animation_slider, null);

        mMonitorBox = (TextView) layout.findViewById(R.id.monitor_box);
        mMonitorBox.setText(String.valueOf(mScale) + "x");

        mSeekBar = (IntervalSeekBar) layout.findViewById(R.id.scale_seekbar);
        mSeekBar.setProgressFloat(mScale);
        mSeekBar.setOnSeekBarChangeListener(this);

        return layout;
    }

    public void setScale(float scale) {
        //if (mSeekBar != null)
        //    mSeekBar.setProgressFloat(scale);
        if (mMonitorBox != null)
            mMonitorBox.setText(String.valueOf(scale) + "x");

        mScale = scale;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        // TODO Auto-generated method stub
        return super.onGetDefaultValue(a, index);
    }

    @Override
    public void setOnPreferenceChangeListener(
            OnPreferenceChangeListener onPreferenceChangeListener) {
        changer = onPreferenceChangeListener;
        super.setOnPreferenceChangeListener(onPreferenceChangeListener);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        setScale(mSeekBar.getProgressFloat());
        changer.onPreferenceChange(this, Float.toString(mScale));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
