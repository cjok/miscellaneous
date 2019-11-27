package com.xo.a12_message;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.icu.text.UnicodeSetSpanner;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private Button btn1, btn2, btn3, btn4;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn1 = findViewById(R.id.btn1);
        btn2 = findViewById(R.id.btn2);
        btn3 = findViewById(R.id.btn3);
        btn4 = findViewById(R.id.btn4);

        btn1.setOnClickListener(onClickListener);
        btn2.setOnClickListener(onClickListener);
        btn3.setOnClickListener(onClickListener);
        btn4.setOnClickListener(onClickListener);
    }

    View.OnClickListener onClickListener = new View.OnClickListener( ) {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                // yes or no
                case R.id.btn1:
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                    alertDialog.setIcon(R.drawable.advise);
                    alertDialog.setTitle("jobs:");
                    alertDialog.setMessage("hello world!");
                    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "no", new DialogInterface.OnClickListener( ) {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(MainActivity.this, "no", Toast.LENGTH_SHORT).show();
                        }
                    });

                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "yes", new DialogInterface.OnClickListener( ) {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(MainActivity.this, "yes", Toast.LENGTH_SHORT).show();
                        }
                    });

                    alertDialog.show();

                break;

                //list Dialog
                case R.id.btn2:
                    final String[] items = new String[] {"aaa", "bbb", "ccc", "ddd"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setIcon(R.drawable.advise1);
                    builder.setTitle("choice:");
                    builder.setItems(items, new DialogInterface.OnClickListener( ) {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(MainActivity.this, items[which],
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                    builder.create().show();

                    break;
                case R.id.btn3:
                    final String[] items1 = new String[] {"aaa", "bbb", "ccc", "ddd"};
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                    builder1.setIcon(R.drawable.advise2);
                    builder1.setTitle("choice one:");
                    builder1.setSingleChoiceItems(items1, 0, new DialogInterface.OnClickListener( ) {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            Toast.makeText(MainActivity.this, items1[which],
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                    builder1.setPositiveButton("yes", null);

                    builder1.create().show();

                    break;
                case R.id.btn4:
                    final String[] items2 = new String[] {"aaa", "bbb", "ccc", "ddd"};
                    final boolean[] checkedItem = new boolean[]{false, true, false, true};
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(MainActivity.this);
                    builder2.setIcon(R.drawable.advise2);
                    builder2.setTitle("choice more:");
                    builder2.setMultiChoiceItems(items2, checkedItem,
                            new DialogInterface.OnMultiChoiceClickListener( ) {
                        @Override
                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {

                            checkedItem[which] = isChecked;

                            Toast.makeText(MainActivity.this, items2[which],
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                    builder2.setPositiveButton("yes", new DialogInterface.OnClickListener( ) {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String result = new String();

                            for (int i = 0; i < checkedItem.length; i++) {
                                if (checkedItem[i]) {
                                    result += items2[i] + ",";
                                }
                            }
                            Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
                        }
                    });

                    builder2.create().show();

                    break;
            }

        }
    };
}
