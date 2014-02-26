/*
 * Copyright (C) 2014 Firtecy
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
package com.android.settings.extended;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.RemoteException;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.settings.IntervalSeekBar;
import com.android.settings.R;

public class ShakeThresholdDialogPreference extends DialogPreference
    implements SeekBar.OnSeekBarChangeListener {

    private TextView mPercentageText;
    private IntervalSeekBar mSeekBar;


    public ShakeThresholdDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        setDialogLayoutResource(R.layout.preference_dialog_shake_threshold);
    }

    @Override
    protected View onCreateDialogView() {
        LayoutInflater inflater =
                (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.preference_dialog_shake_threshold, null);

        mPercentageText = (TextView) view.findViewById(R.id.percentage);

        mSeekBar = (IntervalSeekBar) view.findViewById(R.id.threshold);

        int threshold = Settings.System.getIntForUser(getContext().getContentResolver(), 
                Settings.System.JARVIS_SERVICE_SHAKE_THRESHOLD,
                800, ActivityManager.getCurrentUser());
        
        //Need this typecase to ensure we get a non null value!
        // int / int => result will be int not float
        float currentValue = (float)threshold / (float)1600;

        mSeekBar.setProgressFloat(currentValue);
        mSeekBar.setOnSeekBarChangeListener(this);

        setCurrentShownValue(currentValue);
        
        return view;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            Settings.System.putIntForUser(getContext().getContentResolver(),
                    Settings.System.JARVIS_SERVICE_SHAKE_THRESHOLD,
                    Math.round(1600 * mSeekBar.getProgressFloat()),
                    ActivityManager.getCurrentUser());
        }
    }

    @Override
    protected void onClick() {
        // Ignore this until an explicit call to click()
    }

    public void click() {
        super.onClick();
    }

    private void setCurrentShownValue(float fontScaling) {
        // Update the preview text
        String percentage = Math.round(fontScaling * 100) + "%";
        mPercentageText.setText(percentage);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        setCurrentShownValue(mSeekBar.getProgressFloat());
    }

    // Not used
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
