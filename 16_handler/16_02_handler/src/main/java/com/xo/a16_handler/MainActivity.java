package com.xo.a16_handler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn = findViewById(R.id.btn);
        final TextView tv = findViewById(R.id.tv);

        handler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);

                if (msg.what == 0x123) {
                    tv.setText("bbbbbbbbbbbbbbbbb");

                }
            }
        };


        btn.setOnClickListener(new View.OnClickListener( ) {
            @Override
            public void onClick(View v) {
                Thread thread = new Thread(new Runnable( ) {
                    @Override
                    public void run() {
                        handler.sendEmptyMessage(0x123);
                     //   tv.setText("222222222222222222222");
                    }
                });

                thread.start();
            }
        });

    }
}
