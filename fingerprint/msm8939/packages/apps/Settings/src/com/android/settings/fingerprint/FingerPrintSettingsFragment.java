package com.android.settings.fingerprint;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.android.settings.SettingsPreferenceFragment;

public class FingerPrintSettingsFragment extends SettingsPreferenceFragment {
    private static final String TAG = "FingerPrintSettings";
    private static final String TAG_FP = "VIM";

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        logi("[onCreate]");

        Intent intent = new Intent();
        intent.setClass(this.getActivity(), com.android.settings.fingerprint.FingerPrintSettings.class);
        this.getActivity().startActivity(intent);

        this.getActivity().finish();
    }

    private static void logi(String strs) {
        Log.i(TAG_FP, strs);
    }

    private static void logd(String strs) {
        Log.d(TAG_FP, strs);
    }

    private static void logw(String strs) {
        Log.w(TAG_FP, strs);
    }

    private static void loge(String strs) {
        Log.e(TAG_FP, strs);
    }
}

