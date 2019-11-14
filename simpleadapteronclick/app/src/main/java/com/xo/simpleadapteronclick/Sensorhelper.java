package com.xo.simpleadapteronclick;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PropertyResourceBundle;

public class Sensorhelper {
    final String TAG = "Sensorhelper";
    private Context mcontext;
    private SensorManager sm;



    public Sensorhelper(Context context){
        mcontext = context;
    };

    public List<Map<String, Object>> getSensorInfo() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        sm = (SensorManager) mcontext.getSystemService(mcontext.SENSOR_SERVICE);
        List<Sensor> sensorList = sm.getSensorList(Sensor.TYPE_ALL);

        Log.v(TAG, "sensors: " + sensorList.size( ) + "\n");

        for (Sensor s: sensorList) {
            Log.v(TAG, s.getStringType() + " ");
            Log.v(TAG, " name: " + s.getName() + " vendor:" + s.getVendor() + " version:" + s.getVersion() + "\n");

            Map<String, Object> item = new HashMap<>();
            item.put("type", s.getStringType());
            item.put("name", s.getName());
            item.put("vendor", s.getVendor());
            item.put("version", s.getVersion());
            list.add(item);

        }
        return list;
    }




}
