package com.androidex.lockaxial.util;

import android.util.Log;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by Administrator on 2018/5/19.
 */

public class HttpApi {
    private static HttpApi api;
    private static OkHttpClient client;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static HttpApi getInstance(){
        if(api == null){
            api = new HttpApi();
            client = new OkHttpClient();
        }
        return api;
    }


    public String loadHttpforGet(String u,String t){
        try {
            Log.i("xiao_","发出请求："+u);
            Call call = client.newCall(BuildRequest(null, u, t));
            return call.execute().body().string();
        }catch(Exception e){
            return null;
        }
    }

    public String loadHttpforPost(String u, JSONObject j, String t) throws Exception{
        try {
            RequestBody body = RequestBody.create(JSON, j.toString());
            Call call = client.newCall(BuildRequest(body, u, t));
            return call.execute().body().string();
        }catch(Exception e){
            return null;
        }
    }

    private Request BuildRequest(RequestBody body, String url, String token){
        Request.Builder builder= new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json");
        if(token!=null && token.length()>0){
            builder.header("Authorization","Bearer " + token);
        }
        if(body!=null){
            builder.post(body);
        }
        return builder.build();
    }

    public static String UTCStringtODefaultString(String UTCString) {
        try {
            UTCString = UTCString.replace("Z", " UTC");
            SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS Z");
            SimpleDateFormat defaultFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = utcFormat.parse(UTCString);
            return defaultFormat.format(date);
        } catch (ParseException pe) {
            pe.printStackTrace();
            return null;
        }
    }
}
