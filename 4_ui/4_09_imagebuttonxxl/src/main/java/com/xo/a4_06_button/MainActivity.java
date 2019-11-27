package com.xo.a4_06_button;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //full screen display
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ImageButton btn1 = findViewById(R.id.ib1);
        btn1.setOnClickListener(new View.OnClickListener( ) {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "start game", Toast.LENGTH_SHORT).show();
            }
        });


        ImageButton btn2 = findViewById(R.id.ib2);
        btn2.setOnClickListener(new View.OnClickListener( ) {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "switch", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
