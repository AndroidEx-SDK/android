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

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Administrator on 2018/5/30.
 */

public class FaceAddActivity extends Activity implements CameraHelper.CallBack {
    private SurfaceView mSurfaceView;
    private CameraHelper cameraHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faceadd);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        cameraHelper = new CameraHelper(this,mSurfaceView);
        cameraHelper.addCallBack(this);
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
            this.finish();
        }
    }

    @Override
    public void onFaceDetect(@NotNull ArrayList<RectF> faces) {

    }

    private void startRegisterAcivity(File file){
        Intent i = new Intent(this,FaceRegisterActivity.class);
        i.putExtra("file",file.toString());
        startActivity(i);
    }
}
