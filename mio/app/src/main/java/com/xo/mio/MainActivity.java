package com.xo.mio;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initView();
    }

    private void initView(){
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        actionBar.addTab(actionBar.newTab()
                .setText(getResources().getString(R.string.tab1_name))
                .setTabListener(new MioTabListener(this, FragmentIn.class)));
        actionBar.addTab(actionBar.newTab()
                .setText(getResources().getString(R.string.tab2_name))
                .setTabListener(new MioTabListener(this, FragmentOut.class)));
    }
}
