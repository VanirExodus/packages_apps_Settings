/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import static android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGING;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.UserManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.util.Log;
import android.widget.Switch;

import com.android.internal.os.PowerProfile;
import com.android.internal.util.exodus.SettingsUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.notification.SettingPref;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;

import net.margaritov.preference.colorpicker.ListColorPreference;

import static com.android.internal.util.exodus.SettingsUtils.*;

public class BatterySaverSettings extends SettingsPreferenceFragment
        implements SwitchBar.OnSwitchChangeListener, Preference.OnPreferenceChangeListener,
        ListColorPreference.OnColorChangedListener {
    private static final String TAG = "BatterySaverSettings";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private static final String KEY_PERF_PROFILE = "pref_perf_profile";
    private static final String KEY_PER_APP_PROFILES = "app_perf_profiles_enabled";
    private static final String KEY_TURN_ON_AUTOMATICALLY = "turn_on_automatically";
    private static final String KEY_BATTERY_SAVER_WARNING_COLOR = "battery_saver_warning_color";
    private static final String KEY_DESCRIPTION = "description";

    private static final long WAIT_FOR_SWITCH_ANIM = 500;

    private final Handler mHandler = new Handler();
    private final SettingsObserver mSettingsObserver = new SettingsObserver(mHandler);
    private final Receiver mReceiver = new Receiver();

    private UserManager mUm;
    private Context mContext;
    private boolean mCreated;
    private SettingPref mTriggerPref;
    private SwitchBar mSwitchBar;
    private Switch mSwitch;
    private ListColorPreference mWarningColor;
    private boolean mValidListener;
    private PowerManager mPowerManager;
    private int mExodusMode;

    private ListPreference mPerfProfilePref;
    private String[] mPerfProfileEntries;
    private String[] mPerfProfileValues;
    private String mPerfProfileDefaultEntry;
    private PerformanceProfileObserver mPerformanceProfileObserver = null;
    private SwitchPreference mPerAppProfiles;

    private class PerformanceProfileObserver extends ContentObserver {
        public PerformanceProfileObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updatePerformanceValue();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mUm = (UserManager) activity.getSystemService(Context.USER_SERVICE);
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mExodusMode = SettingsUtils.CurrentMorphMode(getActivity().getContentResolver());
        if (mCreated && mExodusMode == MORPH_MODE_AOSP) {
            mSwitchBar.show();
            return;
        }
        mCreated = true;
        addPreferencesFromResource(R.xml.battery_saver_settings);


        mExodusMode = SettingsUtils.CurrentMorphMode(getActivity().getContentResolver());
        mContext = getActivity();
        mSwitchBar = ((SettingsActivity) mContext).getSwitchBar();
        mSwitch = mSwitchBar.getSwitch();
        if (mExodusMode == MORPH_MODE_AOSP) {
            mSwitchBar.show();
        }

        mTriggerPref = new SettingPref(SettingPref.TYPE_GLOBAL, KEY_TURN_ON_AUTOMATICALLY,
                Global.LOW_POWER_MODE_TRIGGER_LEVEL,
                0, /*default*/
                getResources().getIntArray(R.array.battery_saver_trigger_values)) {
            @Override
            protected String getCaption(Resources res, int value) {
                if (value > 0 && value < 100) {
                    return res.getString(R.string.battery_saver_turn_on_automatically_pct,
                                         Utils.formatPercentage(value));
                }
                return res.getString(R.string.battery_saver_turn_on_automatically_never);
            }
        };
        mTriggerPref.init(this);

        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

        mPerfProfileEntries = getResources().getStringArray(
                com.android.internal.R.array.perf_profile_entries);
        mPerfProfileValues = getResources().getStringArray(
                com.android.internal.R.array.perf_profile_values);

        //Only in Exodus
        if (mExodusMode == MORPH_MODE_EXODUS) {
            initWarningColor();
            mPerfProfilePref = (ListPreference) findPreference(KEY_PERF_PROFILE);
            mPerAppProfiles = (SwitchPreference) findPreference(KEY_PER_APP_PROFILES);
            if (mPerfProfilePref != null && !mPowerManager.hasPowerProfiles()) {
                removePreference(KEY_PERF_PROFILE);
                removePreference(KEY_PER_APP_PROFILES);
                mPerfProfilePref = null;
                mPerAppProfiles = null;
            } else if (mPerfProfilePref != null) {
                mPerfProfilePref.setOrder(-1);
                mPerfProfilePref.setEntries(mPerfProfileEntries);
                mPerfProfilePref.setEntryValues(mPerfProfileValues);
                updatePerformanceValue();
                mPerfProfilePref.setOnPreferenceChangeListener(this);
            }
        }

        //Respect Morph
        if (!(mExodusMode == MORPH_MODE_EXODUS)) {
            removePreference(KEY_BATTERY_SAVER_WARNING_COLOR);
            removePreference(KEY_PERF_PROFILE);
            removePreference(KEY_PER_APP_PROFILES);
        }
        if (!(mExodusMode == MORPH_MODE_AOSP)) {
            removePreference(KEY_DESCRIPTION);
        }

        mPerformanceProfileObserver = new PerformanceProfileObserver(new Handler());
    }

    private void initWarningColor() {
        ContentResolver resolver = getActivity().getContentResolver();

        mWarningColor = (ListColorPreference) findPreference(KEY_BATTERY_SAVER_WARNING_COLOR);
        mWarningColor.setOnPreferenceChangeListener(this);
        mWarningColor.setOnColorChangedListener(this);

        int batterySaverWarningColorStyle = Settings.System.getInt(resolver,
                 Settings.System.BATTERY_SAVER_MODE_COLOR_STYLE, 0);
        mWarningColor.setValue(String.valueOf(batterySaverWarningColorStyle));
        mWarningColor.setSummary(mWarningColor.getEntry());
        mWarningColor.setColorSelection(String.valueOf(1));

        if ( batterySaverWarningColorStyle == 1 ) {
            int intColor = Settings.System.getInt(resolver,
                    Settings.System.BATTERY_SAVER_MODE_COLOR, -2);
            if (intColor == -2) {
                intColor = getResources().getColor(
                        com.android.internal.R.color.battery_saver_mode_color);
            } else {
                String hexColor = String.format("#%08x", (0xffffffff & intColor));
            }
            mWarningColor.setNewPreviewColor(intColor);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSwitchBar.hide();
    }

    @Override
    public void onResume() {
        super.onResume();
        mSettingsObserver.setListening(true);
        mReceiver.setListening(true);
        if (!mValidListener) {
            mSwitchBar.addOnSwitchChangeListener(this);
            mValidListener = true;
        }
        updateSwitch();

        if (mPerfProfilePref != null) {
            updatePerformanceValue();
            ContentResolver resolver = getActivity().getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor(
                    Settings.Secure.PERFORMANCE_PROFILE), false, mPerformanceProfileObserver);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mSettingsObserver.setListening(false);
        mReceiver.setListening(false);
        if (mValidListener) {
            mSwitchBar.removeOnSwitchChangeListener(this);
            mValidListener = false;
        }

        if (mPerfProfilePref != null) {
            ContentResolver resolver = getActivity().getContentResolver();
            resolver.unregisterContentObserver(mPerformanceProfileObserver);
        }
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        mHandler.removeCallbacks(mStartMode);
        if (isChecked) {
            mHandler.postDelayed(mStartMode, WAIT_FOR_SWITCH_ANIM);
        } else {
            if (DEBUG) Log.d(TAG, "Stopping low power mode from settings");
            trySetPowerSaveMode(false);
        }
    }

    @Override
    public void onColorChanged(int color) {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.BATTERY_SAVER_MODE_COLOR, color);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue != null) {
            if (preference == mPerfProfilePref) {
                mPowerManager.setPowerProfile(String.valueOf(newValue));
                updatePerformanceSummary();
                return true;
            } else if (preference == mWarningColor) {
                int warningStyle = Integer.parseInt((String) newValue);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.BATTERY_SAVER_MODE_COLOR_STYLE, warningStyle);
                int index = mWarningColor.findIndexOfValue((String) newValue);
                mWarningColor.setSummary(mWarningColor.getEntries()[index]);
                return true;
            }
        }
        return false;
    }

    private void trySetPowerSaveMode(boolean mode) {
        if (!mPowerManager.setPowerSaveMode(mode)) {
            if (DEBUG) Log.d(TAG, "Setting mode failed, fallback to current value");
            mHandler.post(mUpdateSwitch);
        }
    }

    private void updateSwitch() {
        final boolean mode = mPowerManager.isPowerSaveMode();
        if (DEBUG) Log.d(TAG, "updateSwitch: isChecked=" + mSwitch.isChecked() + " mode=" + mode);
        if (mode == mSwitch.isChecked()) return;

        // set listener to null so that that code below doesn't trigger onCheckedChanged()
        if (mValidListener) {
            mSwitchBar.removeOnSwitchChangeListener(this);
        }
        mSwitch.setChecked(mode);
        if (mValidListener) {
            mSwitchBar.addOnSwitchChangeListener(this);
        }
    }

    private final Runnable mUpdateSwitch = new Runnable() {
        @Override
        public void run() {
            updateSwitch();
        }
    };

    private final Runnable mStartMode = new Runnable() {
        @Override
        public void run() {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    if (DEBUG) Log.d(TAG, "Starting low power mode from settings");
                    trySetPowerSaveMode(true);
                }
            });
        }
    };

    private final class Receiver extends BroadcastReceiver {
        private boolean mRegistered;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) Log.d(TAG, "Received " + intent.getAction());
            mHandler.post(mUpdateSwitch);
        }

        public void setListening(boolean listening) {
            if (listening && !mRegistered) {
                mContext.registerReceiver(this, new IntentFilter(ACTION_POWER_SAVE_MODE_CHANGING));
                mRegistered = true;
            } else if (!listening && mRegistered) {
                mContext.unregisterReceiver(this);
                mRegistered = false;
            }
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri LOW_POWER_MODE_TRIGGER_LEVEL_URI
                = Global.getUriFor(Global.LOW_POWER_MODE_TRIGGER_LEVEL);

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (LOW_POWER_MODE_TRIGGER_LEVEL_URI.equals(uri)) {
                mTriggerPref.update(mContext);
            }
        }

        public void setListening(boolean listening) {
            final ContentResolver cr = getContentResolver();
            if (listening) {
                cr.registerContentObserver(LOW_POWER_MODE_TRIGGER_LEVEL_URI, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }
    }

    private void updatePerformanceSummary() {
        String value = mPowerManager.getPowerProfile();
        String summary = "";
        int count = mPerfProfileValues.length;
        for (int i = 0; i < count; i++) {
            try {
                if (mPerfProfileValues[i].equals(value)) {
                    summary = mPerfProfileEntries[i];
                }
            } catch (IndexOutOfBoundsException ex) {
                // Ignore
            }
        }
        mPerfProfilePref.setSummary(String.format("%s", summary));
    }

    private void updatePerformanceValue() {
        if (mPerfProfilePref == null) {
            return;
        }
        mPerfProfilePref.setValue(mPowerManager.getPowerProfile());
        updatePerformanceSummary();
    }
}
