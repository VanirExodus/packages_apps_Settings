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

package com.android.settings.vanir.buttons;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v13.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost.OnTabChangeListener;

import com.android.settings.R;

import static com.android.internal.util.vanir.HardwareButtonConstants.*;

public class ButtonsTabHost extends Fragment implements OnTabChangeListener {

    private FragmentTabHost mTabHost;

    static String sLastTab;
    static Resources sRes;

    public ButtonsTabHost() {
    }

    private static int[] BUTTONS = new int[] {
        KEY_MASK_MENU,
        KEY_MASK_HOME,
        KEY_MASK_BACK,
        KEY_MASK_ASSIST,
        KEY_MASK_APP_SWITCH
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        sRes = activity.getResources();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mTabHost = new FragmentTabHost(getActivity());
        mTabHost.setup(getActivity(), getChildFragmentManager(), R.id.container_material);

        final int deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);

        for (int buttonMask : BUTTONS) {
            if ((deviceKeys & buttonMask) != 0) {
                mTabHost.addTab(
                        mTabHost.newTabSpec(Integer.toString(buttonMask)).setIndicator(getStringFromMask(buttonMask)),
                                ButtonSettingsFragment.class, null);

            }
        }

        mTabHost.setOnTabChangedListener(this);
        return mTabHost;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sLastTab != null) {
            mTabHost.setCurrentTabByTag(sLastTab);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mTabHost = null;
    }

    @Override
    public void onTabChanged(String s) {
        sLastTab = s;
    }

    protected static String getStringFromMask(int buttonMask) {
        switch (buttonMask) {
            case KEY_MASK_HOME:
                return sRes.getString(R.string.home_button);
            case KEY_MASK_BACK:
                return sRes.getString(R.string.back_button);
            case KEY_MASK_ASSIST:
                return sRes.getString(R.string.assist_button);
            case KEY_MASK_APP_SWITCH:
                return sRes.getString(R.string.app_switch_button);
            case KEY_MASK_MENU:
                return sRes.getString(R.string.menu_button);
         }
         return Integer.toString(buttonMask);
    }
}
