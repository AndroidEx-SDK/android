package com.androidex.lockaxial;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.androidex.R;

/**
 * Created by Administrator on 2018/5/15.
 */

public class InfoManagerActivity extends Activity {
    private String houseData;
    private String currentUnit;
    private int userid;
    private String token;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_infomanager);
        houseData = getIntent().getStringExtra("data");
        currentUnit = getIntent().getStringExtra("currentUnit");
        userid = getIntent().getIntExtra("userid",-1);
        token = getIntent().getStringExtra("token");

    }

    public void openCardData(View v){
        Intent in = new Intent(this,CardActivity.class);
        in.putExtra("data",houseData);
        in.putExtra("currentUnit",currentUnit);
        in.putExtra("userid",userid);
        in.putExtra("token",token);
        startActivity(in);
    }

    public void openFaceData(View v){
        Intent in = new Intent(this,FaceActivity.class);
        in.putExtra("data",houseData);
        in.putExtra("currentUnit",currentUnit);
        in.putExtra("userid",userid);
        in.putExtra("token",token);
        startActivity(in);
    }

}
