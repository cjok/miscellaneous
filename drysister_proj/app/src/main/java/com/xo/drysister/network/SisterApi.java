package com.xo.drysister.network;

import android.util.Log;

import com.xo.drysister.bean.entity.Sister;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class SisterApi {
    private static final String TAG = "SisterApi";
    private static final String BASE_URL = "http://gank.io/api/data/福利/";

    public ArrayList<Sister> fetchSister(int count, int page) throws IOException, JSONException {
        String fetchUrl = BASE_URL + count + "/" + page;
        ArrayList<Sister> sisters = new ArrayList<>();

        URL url = new URL(fetchUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        int code = connection.getResponseCode();
        Log.v(TAG, "server response code: " + code);
        if (code == 200) {
            InputStream inputStream = connection.getInputStream();
            byte [] data = readFromStream(inputStream);
            String result = new String(data, "UTF-8");
            sisters = parseSister(result);
        } else  {
            Log.e(TAG, "server request error");
        }

        return sisters;
    }

    public ArrayList<Sister> parseSister(String content) throws JSONException {
        ArrayList<Sister> sisters = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(content);
        JSONArray jsonArray = jsonObject.getJSONArray("results");

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject results = (JSONObject) jsonArray.get(i);
            Sister sister = new Sister();
            sister.set_id(results.getString("_id"));
            sister.setCreateAt(results.getString("createdAt"));
            sister.setDesc(results.getString("desc"));
            sister.setPublishedAt(results.getString("publishedAt"));
            sister.setSource(results.getString("source"));
            sister.setType(results.getString("type"));
            sister.setUrl(results.getString("url"));
            sister.setUsed(results.getBoolean("used"));
            sister.setWho(results.getString("who"));
            System.out.println("paul : " + sister.toString());
            sisters.add(sister);

        }
        return sisters;

    }

    public byte[] readFromStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, len);
        }
        inputStream.close();
        return outputStream.toByteArray();
    }
}
