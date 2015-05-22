/*
 * Copyright (C) 2015 Exodus
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

package com.android.settings.exodus;

import android.content.Context;
import android.content.res.Resources;

import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

import android.provider.SearchIndexableResource;

import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.AsyncTask;
import android.os.ServiceManager;

import android.app.ProgressDialog;
import android.app.Activity;
import android.app.IActivityManager;

import android.util.DisplayMetrics;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.List;
import java.util.ArrayList;

/**
 * This class intended to persist the MorhpRom option.
 * TODO: fix the activty issue.
 * --------------------
 * @see #MorphRom
 *
 * @Author	Raja Mungamuri
 * @Version	1.0 (Android 5.1.x)
 * @Since	2015-MAY-23
 */
public class Morphing extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener  {

    private static final String TAG = "Morphing";

    private static final String MORPH_ROM = "morph_rom";

    private static final String MORPH_ACCESS_KEY = "morph_rom";
    private static final String MORPH_ACCESS_PROPERTY = "persist.sys.morph_rom";

    private ListPreference morphRomPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //final Activity activity = getActivity();

        addPreferencesFromResource(R.xml.development_prefs);

        //RomControl_Menu1: LCD Density
        morphTheRom();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (MORPH_ACCESS_KEY.equals(key)) {
            try{
                int value = Integer.parseInt((String) objValue);
              //  writeLcdDensityPreference(preference.getContext(), value);

            SystemProperties.set("persist.sys.morgh_rom", Integer.toString(value));
        
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist display density setting", e);
            }
        }
        return true;
    }

    private void morphTheRom(){
        morphRomPreference = (ListPreference) findPreference(MORPH_ACCESS_KEY);
        if (morphRomPreference != null) {
            morphRomPreference.setOnPreferenceChangeListener(this);
        }
    }
}

