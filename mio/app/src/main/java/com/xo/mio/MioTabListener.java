package com.xo.mio;

import android.app.Activity;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class MioTabListener implements ActionBar.TabListener {

    private final Activity activity;
    private final Class aClass;

    private Fragment fragment;

    public MioTabListener(Activity activity, Class aClass) {
        this.activity = activity;
        this.aClass = aClass;
    }


    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        if (fragment == null) {
            fragment = Fragment.instantiate(activity, aClass.getName( ));
            ft.add(android.R.id.content, fragment, null);
        }
        ft.attach(fragment);
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {

        if (fragment != null) {
            ft.detach(fragment);
        }
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }
}
