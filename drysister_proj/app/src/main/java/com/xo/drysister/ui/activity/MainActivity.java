package com.xo.drysister.ui.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.xo.drysister.bean.entity.Sister;
import com.xo.drysister.imgloader.PictureLoader;
import com.xo.drysister.R;
import com.xo.drysister.network.SisterApi;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Button btn_next, btn_refresh;
    private ImageView img_show;

    private ArrayList<Sister> data;
    private int curPos = 0;
    private int page = 1;
    private PictureLoader loader;
    private SisterApi sisterApi;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loader = new PictureLoader();
        sisterApi = new SisterApi();
        initData();
        initUI();
    }

    private void initData() {
        data = new ArrayList<>();
        new SisterTask(page).execute();
    }

    private void initUI() {
        btn_next = findViewById(R.id.btn_next);
        img_show = findViewById(R.id.img_show);
        btn_refresh = findViewById(R.id.btn_refresh);

        btn_refresh.setOnClickListener(this);
        btn_next.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_next:
                if (data != null && !data.isEmpty()) {
                    if (curPos > 9) {
                        curPos = 0;
                    }
                    loader.load(img_show, data.get(curPos).getUrl());
                    curPos++;
                }
                break;
            case R.id.btn_refresh:
                page++;
                new SisterTask(page).execute();
                curPos = 0;
                break;
        }
    }

    private class SisterTask extends AsyncTask<Void, Void, ArrayList<Sister>> {
        private int page;

        public SisterTask(int page){
            this.page = page;
        }

        @Override
        protected ArrayList<Sister> doInBackground(Void... voids) {
            try {
                return sisterApi.fetchSister(10, page);
            } catch (IOException e) {
                e.printStackTrace( );
            } catch (JSONException e) {
                e.printStackTrace( );
            }

            return null;
        }

        protected void onPostExecute(ArrayList<Sister> sisters) {
            super.onPostExecute(sisters);
            data.clear();
            data.addAll(sisters);
        }
    }
}
