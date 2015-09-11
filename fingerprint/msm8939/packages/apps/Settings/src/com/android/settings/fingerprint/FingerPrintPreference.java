package com.android.settings.fingerprint;

import android.preference.Preference;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;
import android.content.Context;
import android.util.Log;

import com.android.settings.R;

public class FingerPrintPreference extends Preference {
    private ImageView mIvIcon;
    private TextView mTvTitile;
    private ImageView mIvDirection;

    public FingerPrintPreference(Context context) {
        super(context, null);
    }

    /*
    @Override
    protected void onBindView(View view) {
        mIvIcon = (ImageView) view.findViewById(R.id.icon);
        mTvTitile = (TextView) view.findViewById(R.id.title);
        mIvDirection = (ImageView) view.findViewById(R.id.direction);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        return LayoutInflater.from(this.getContext()).inflate(R.layout.finger_print_preference,
                parent, false);
    }

    @Override
    public void setTitle(CharSequence title) {
        Log.d("VIM", "[setTitle] mTvTitile : " + mTvTitile);
        mTvTitile.setText(title);
    }

    public void showIcon() {
        mIvIcon.setVisibility(View.VISIBLE);
    }
    */
}
