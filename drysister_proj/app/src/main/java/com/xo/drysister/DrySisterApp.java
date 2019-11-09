package com.xo.drysister;

import android.app.Application;
import android.content.Context;

public class DrySisterApp extends Application {
    private static Context context;

    public void onCreate() {

        super.onCreate( );
        context = this;
    }

    public static DrySisterApp getContext(){
        return (DrySisterApp) context;
    }
}
