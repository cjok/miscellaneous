package com.paul.jnihello;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    //加载libhello.so
    static {
        System.loadLibrary("hello");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    //点击button时弹出吐司
    public  void click(View v) {
        Toast.makeText(this, stringFromJNI(), 0).show();
    }

    //声明native接口
    public  native String stringFromJNI();
}
