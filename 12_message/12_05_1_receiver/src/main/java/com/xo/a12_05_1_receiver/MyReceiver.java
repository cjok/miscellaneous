package com.xo.a12_05_broadcastreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MyReceiver extends BroadcastReceiver {

    final String ACTION_1 = "SEND_BROADCAST";


    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(ACTION_1)) {

            Toast.makeText(context, "recevied", Toast.LENGTH_SHORT).show();

        }

    }
}
