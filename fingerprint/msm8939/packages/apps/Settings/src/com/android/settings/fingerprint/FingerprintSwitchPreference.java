package com.android.settings.fingerprint;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.provider.Settings;

import com.android.internal.widget.LockPatternUtils;

import com.android.settings.R;

public class FingerprintSwitchPreference extends Preference
            implements CompoundButton.OnCheckedChangeListener ,OnClickListener{
    private static final String TAG = "FingerprintSwitchPreference";
    private String mProperties = null;
    private Switch mSwitch;
    private SwitchChange mSwitchChange;
    private boolean mFirst = true;
    private LockPatternUtils mLockPatternUtils;

    public FingerprintSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLockPatternUtils = new LockPatternUtils(context);
    }

    public boolean getCurrentStatus() {
        if (mSwitch != null) {
            return mSwitch.isChecked();
        } else {
            return false;
        }
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        mSwitch = (Switch) view.findViewById(R.id.hct_switchWidget);
        mSwitch.setOnCheckedChangeListener(this);
        mSwitch.setOnClickListener(this);
        if (FingerPrintSettings.KEY_FINGERPRINT_UNLOCK_KEYGUARD.equals(getKey())) {
            mSwitch.setChecked(mLockPatternUtils.isLockFingerprintEnabled());
        } else if (FingerPrintSettings.KEY_FINGERPRINT_COLD_UNLOCK_KEYGUARD.equals(getKey())) {
            mSwitch.setChecked(mLockPatternUtils.isFingerprintColdUnlockEnable());
        }
    }

    public void onCheckedChanged(CompoundButton paramCompoundButton, boolean isChecked) {
        Log.d("VIM", "[onCheckedChanged] key=" + getKey() + ", isChecked=" + isChecked + ", mFirst : " + mFirst);

        if (mSwitchChange != null) {
            mSwitchChange.onSwitchChange(getKey(), isChecked);
        }
    }

    protected View onCreateView(ViewGroup parent) {
        return super.onCreateView(parent);
    }

    public void setCurrentStatus(boolean isCheck) {
        if (mSwitch != null) {
          mSwitch.setChecked(isCheck);
        }
    }

    public void setSwitchChange(SwitchChange switchChange) {
        mSwitchChange = switchChange;
    }

    public static abstract interface SwitchChange {
        public abstract void onSwitchChange(String paramString, boolean paramBoolean);
        public abstract void onSwitchClick(View v, String paramString);
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        mSwitchChange.onSwitchClick(v, getKey());
    }
        
}
