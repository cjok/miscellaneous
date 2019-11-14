package com.xo.simpleadapteronclick;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

public class SensorView extends AppCompatActivity implements SensorEventListener{
    private int type;

    private SensorManager sm;
    private Context context;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_view);
        context = getApplicationContext();
        textView = findViewById(R.id.textView);

        sm = (SensorManager) getSystemService(context.SENSOR_SERVICE);

        Intent intent = getIntent();
        type = intent.getIntExtra("type", 0);

        System.out.println("type : " + type);
        getSensorData(type);

    }


    public void getSensorData(int type) {

        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
                Sensor acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                sm.registerListener(this, acc, SensorManager.SENSOR_DELAY_UI);
                break;
            case Sensor.TYPE_LIGHT:
                Sensor light = sm.getDefaultSensor(Sensor.TYPE_LIGHT);
                sm.registerListener(this, light, SensorManager.SENSOR_DELAY_UI);
                break;
            case Sensor.TYPE_PROXIMITY:
                Sensor proximity = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
                sm.registerListener(this, proximity, SensorManager.SENSOR_DELAY_UI);
                break;
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        SensorEvent se = event;
        Sensor s =  event.sensor;

        switch (s.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                textView.setText(s.getStringType() + ":" + " X: " + (float)(Math.round(event.values[0]) * 100) /100
                        + " Y: " + (float)(Math.round(event.values[1]) * 100) /100
                        + " Z: " + (float)(Math.round(event.values[2]) * 100) /100 + "\n");
                break;
            case Sensor.TYPE_LIGHT:
                textView.setText(s.getStringType() + ": " + event.values[0] + "\n");
                break;
            case Sensor.TYPE_PROXIMITY:
                textView.setText(s.getStringType() + ":" + event.values[0] + "\n");
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


}
