/*
 * Copyright (C) 2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cyanogenmod;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.exodussettings.exodus.prefrences.BaseExodusSettingSwitchBar;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.internal.util.cm.NavigationRingConstants;
import com.android.internal.util.exodus.SettingsUtils;

public class NavRing extends Fragment implements View.OnClickListener,
        BaseExodusSettingSwitchBar.SwitchBarChangeCallback {

    private BaseExodusSettingSwitchBar mNavRingEnabler;
    private LinearLayout mRestore, mSave, mEdit;
    private TextView mMessage, mDisabledMessage;
    private final static Intent TRIGGER_INTENT =
            new Intent(NavigationRingConstants.BROADCAST);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (SettingsUtils.isMorphExodus(getActivity().getContentResolver()))
            return inflater.inflate(R.layout.exodus_nav_ring, container, false);
        else
            return inflater.inflate(R.layout.nav_bar, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mMessage = (TextView) view.findViewById(R.id.message);
        mMessage.setText(R.string.navigation_ring_message);
        mEdit = (LinearLayout) view.findViewById(R.id.navbar_edit);
        mEdit.setOnClickListener(this);
        mSave = (LinearLayout) view.findViewById(R.id.navbar_save);
        mSave.setOnClickListener(this);
        mRestore = (LinearLayout) view.findViewById(R.id.navbar_restore);
        mRestore.setOnClickListener(this);
        if (SettingsUtils.isMorphExodus(getActivity().getContentResolver())) {
            mDisabledMessage = (TextView) view.findViewById(R.id.disabled_message);
            boolean enabled = Settings.Exodus.getInt(getActivity().getContentResolver(),
                    Settings.Exodus.ENABLE_NAVIGATION_RING, 1) == 1;
            updateNavRing(enabled);
        } else {
            updateNavRing(true);
        }
    }

    @Override
    public void onDestroyView() {
        if (mNavRingEnabler != null) {
            mNavRingEnabler.teardownSwitchBar();
        }
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
        final SettingsActivity activity = (SettingsActivity) getActivity();
        mNavRingEnabler = new BaseExodusSettingSwitchBar(activity, activity.getSwitchBar(),
                Settings.Exodus.ENABLE_NAVIGATION_RING, true, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNavRingEnabler != null) {
            mNavRingEnabler.resume(getActivity());
        }
	setEditMode(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setEditMode(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mNavRingEnabler != null) {
            mNavRingEnabler.pause();
        }
        setEditMode(false);
    }

    @Override
    public void onStop() {
        super.onStop();
        setEditMode(false);
    }

    @Override
    public void onEnablerChanged(boolean isEnabled) {
        updateNavRing(isEnabled);
    }

    private void updateNavRing (boolean isEnabled) {
        if (isEnabled && mDisabledMessage != null) {
            mMessage.setVisibility(View.VISIBLE);
            mDisabledMessage.setVisibility(View.GONE);
            mRestore.setVisibility(View.VISIBLE);
            mEdit.setVisibility(View.VISIBLE);
        } else if (mDisabledMessage != null) {
            mMessage.setVisibility(View.GONE);
            mDisabledMessage.setVisibility(View.VISIBLE);
            mRestore.setVisibility(View.GONE);
            mEdit.setVisibility(View.GONE);
        }
        setEditMode(false);
    }

    private void setEditMode(boolean on) {
        TRIGGER_INTENT.putExtra(NavigationRingConstants.EDIT_STATE_EXTRA, on);
        getActivity().sendBroadcast(TRIGGER_INTENT);
    }

    @Override
    public void onClick(View v) {
        if (v == mEdit) {
            setEditMode(true);
        } else if (v == mRestore) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.profile_reset_title)
                    .setIcon(R.drawable.ic_navbar_restore)
                    .setMessage(R.string.navigation_bar_reset_message)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            for (int i = 0; i < 3; i++) {
                                Settings.Secure.putString(getActivity().getContentResolver(),
                                        Settings.Secure.NAVIGATION_RING_TARGETS[i], null);
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
    }
}
