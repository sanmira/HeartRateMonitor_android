package com.example.testapplication.ui.beat_counter;

import android.content.ContentValues;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

public class StatClient extends AsyncTask<String, String, String> {

    private String TAG = "STAT";

    public StatClient(){
        //set context variables if required
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    public String doInBackground(String... params) {
        String response = "";
        String urlString = params[0]; // URL to call
        String data = params[1]; //data to post
        String result = params[2];

        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            try {
                urlConnection.setRequestMethod("PUT");
                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(0);
                urlConnection.setRequestProperty("Content-type", "application/json; charset=UTF-8");

                OutputStream os = urlConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("data", data);
                jsonParam.put("result", result);

                writer.write(jsonParam.toString());
                writer.flush();
                writer.close();
                os.close();

                byte[] readData = new byte[256];
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                in.read(readData);
                System.out.println(readData.toString());
                Log.e(TAG, "PUT REQUEST");
            } catch (Exception e) {
                Log.e(TAG, "exception", e);
                System.out.println(e.getMessage());
            } finally {
                urlConnection.disconnect();
            }

//            urlConnection.connect();
        } catch (Exception e) {
            Log.e(TAG, "exception", e);
            System.out.println(e.getMessage());
        }
        return response;
    }
}
