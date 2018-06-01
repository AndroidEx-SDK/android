package com.androidex.lockaxial;

import android.app.Activity;
import android.content.Intent;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.SurfaceView;
import android.view.View;

import com.androidex.R;
import com.androidex.lockaxial.util.CameraHelper;
import com.androidex.lockaxial.util.HttpApi;
import com.androidex.lockaxial.util.RegisterEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Administrator on 2018/5/30.
 */

public class FaceAddActivity extends Activity implements CameraHelper.CallBack {
    private SurfaceView mSurfaceView;
    private CameraHelper cameraHelper;

    private String houseData;
    private String currentUnit;
    private String token;
    private int userid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faceadd);
        EventBus.getDefault().register(this);
        houseData = getIntent().getStringExtra("data");
        currentUnit = getIntent().getStringExtra("currentUnit");
        userid = getIntent().getIntExtra("userid",-1);
        token = getIntent().getStringExtra("token");
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        cameraHelper = new CameraHelper(this,mSurfaceView);
        cameraHelper.addCallBack(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.finish();
    }

    @Subscribe
    public void onEventMessage(RegisterEvent event){
        if(event.msg.equals("Exit")){
            this.finish();
        }
    }

    public void onPicture(View view){
        cameraHelper.takePic();
    }

    public void onSwitch(View view){
        cameraHelper.exchangeCamera();
    }

    @Override
    public void onPreviewFrame(@org.jetbrains.annotations.Nullable byte[] data) {

    }

    @Override
    public void onTakePic(@org.jetbrains.annotations.Nullable byte[] data) {
        File file = HttpApi.savePictureFile(this,data,cameraHelper.getCameradirection());
        if(file!=null){
            startRegisterAcivity(file);
        }
    }

    @Override
    public void onFaceDetect(@NotNull ArrayList<RectF> faces) {

    }

    private void startRegisterAcivity(File file){
        Intent i = new Intent(this,FaceRegisterActivity.class);
        i.putExtra("file",file.toString());
        i.putExtra("data",houseData);
        i.putExtra("currentUnit",currentUnit);
        i.putExtra("userid",userid);
        i.putExtra("token",token);
        startActivity(i);
    }
}
