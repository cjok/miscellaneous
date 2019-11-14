package com.xo.simpleadapteronclick;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Context mcontext;
    private Sensorhelper sensorhelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mcontext = getApplicationContext();
        sensorhelper = new Sensorhelper(mcontext);

        final SimpleAdapter simpleAdapter = new SimpleAdapter(mcontext,sensorhelper.getSensorInfo(), R.layout.listitem,
                new String[]{"type", "name"}, new int[]{R.id.name, R.id.data});

        ListView listView = findViewById(R.id.lv);
        listView.setAdapter(simpleAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener( ) {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Map<String, Object> map = (Map<String, Object>) simpleAdapter.getItem(position);

                Intent intent = new Intent(MainActivity.this, SensorView.class);


                switch (map.get("type").toString()) {
                    case Sensor.STRING_TYPE_ACCELEROMETER:
                        Toast.makeText(mcontext, "ACC", Toast.LENGTH_SHORT).show();
                        intent.putExtra("type", Sensor.TYPE_ACCELEROMETER);
                        startActivity(intent);
                        break;
                    case Sensor.STRING_TYPE_LIGHT:
                        Toast.makeText(mcontext, "light", Toast.LENGTH_SHORT).show();
                        intent.putExtra("type", Sensor.TYPE_LIGHT);
                        startActivity(intent);
                        break;
                    case Sensor.STRING_TYPE_PROXIMITY:
                        Toast.makeText(mcontext, "promixity", Toast.LENGTH_SHORT).show();
                        intent.putExtra("type", Sensor.TYPE_PROXIMITY);
                        startActivity(intent);
                        break;
                }
            }
        });
    }


}
