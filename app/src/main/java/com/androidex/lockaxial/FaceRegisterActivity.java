package com.androidex.lockaxial;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.androidex.R;
import com.androidex.lockaxial.util.BitmapUtils;
import com.androidex.lockaxial.util.FaceDB;
import com.arcsoft.facedetection.AFD_FSDKEngine;
import com.arcsoft.facedetection.AFD_FSDKError;
import com.arcsoft.facedetection.AFD_FSDKFace;
import com.arcsoft.facedetection.AFD_FSDKVersion;
import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.guo.android_extend.image.ImageConverter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Administrator on 2018/5/30.
 */

public class FaceRegisterActivity extends Activity implements SurfaceHolder.Callback{
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private String path;
    private Bitmap mBitmap;
    private Rect src = new Rect();
    private Rect dst = new Rect();
    private AFR_FSDKFace mAFR_FSDKFace;
    private static final String TAG = "xiao_";
    private FaceDB faceDB;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 0x01:{
                    showL("FD初始化失败");
                    showToast("系统错误，请联系管理员");
                }break;
                case 0x02:{
                    showL("FR初始化失败");
                    showToast("系统错误，请联系管理员");
                }break;
                case 0x03:{
                    showToast("检查到人脸数据");
                }break;
                case 0x04:{
                    showToast("无法检测人脸特征");
                }break;
                case 0x05:{
                    showToast("未发现人脸");
                }break;
            }
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faceregister);
        if(!getPath()){
            showToast("无效图片地址");
            this.finish();
        }
        mBitmap = BitmapUtils.decodeImage(path);
        if(mBitmap == null){
            showToast("无效图片");
            this.finish();
        }
        faceDB = new FaceDB(getFilesDir().getPath());
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        src.set(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        mSurfaceView.getHolder().addCallback(this);
        new faceThread().start();
    }

    private void showToast(final String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(FaceRegisterActivity.this,msg,Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showL(String msg){
        Log.i("xiao_",msg);
    }

    private boolean getPath(){
        try{
            path = getIntent().getStringExtra("file");
            if(path == null || path.length()<=0){
                return false;
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mSurfaceHolder = surfaceHolder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mSurfaceHolder = null;
    }

    class faceThread extends Thread{
        @Override
        public void run() {
            while (mSurfaceHolder == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            byte[] data = new byte[mBitmap.getWidth() * mBitmap.getHeight() * 3 / 2];
            ImageConverter convert = new ImageConverter();
            convert.initial(mBitmap.getWidth(), mBitmap.getHeight(), ImageConverter.CP_PAF_NV21);
            if (convert.convert(mBitmap, data)) {
                Log.d(TAG, "convert ok!");
            }
            convert.destroy();
            AFD_FSDKEngine engine = new AFD_FSDKEngine();
            AFD_FSDKVersion version = new AFD_FSDKVersion();
            List<AFD_FSDKFace> result = new ArrayList<AFD_FSDKFace>();
            AFD_FSDKError err = engine.AFD_FSDK_InitialFaceEngine(FaceDB.appid, FaceDB.fd_key, AFD_FSDKEngine.AFD_OPF_0_HIGHER_EXT, 16, 5);
            Log.d(TAG, "AFD_FSDK_InitialFaceEngine = " + err.getCode());
            if (err.getCode() != AFD_FSDKError.MOK) {
                mHandler.sendEmptyMessage(0x01); // FD初始化失败
            }
            err = engine.AFD_FSDK_GetVersion(version);
            Log.d(TAG, "AFD_FSDK_GetVersion =" + version.toString() + ", " + err.getCode());
            err  = engine.AFD_FSDK_StillImageFaceDetection(data, mBitmap.getWidth(), mBitmap.getHeight(), AFD_FSDKEngine.CP_PAF_NV21, result);
            Log.d(TAG, "AFD_FSDK_StillImageFaceDetection =" + err.getCode() + "<" + result.size());
            while (mSurfaceHolder != null) {
                Canvas canvas = mSurfaceHolder.lockCanvas();
                if (canvas != null) {
                    Paint mPaint = new Paint();
                    boolean fit_horizontal = canvas.getWidth() / (float)src.width() < canvas.getHeight() / (float)src.height() ? true : false;
                    float scale = 1.0f;
                    if (fit_horizontal) {
                        scale = canvas.getWidth() / (float)src.width();
                        dst.left = 0;
                        dst.top = (canvas.getHeight() - (int)(src.height() * scale)) / 2;
                        dst.right = dst.left + canvas.getWidth();
                        dst.bottom = dst.top + (int)(src.height() * scale);
                    } else {
                        scale = canvas.getHeight() / (float)src.height();
                        dst.left = (canvas.getWidth() - (int)(src.width() * scale)) / 2;
                        dst.top = 0;
                        dst.right = dst.left + (int)(src.width() * scale);
                        dst.bottom = dst.top + canvas.getHeight();
                    }
                    canvas.drawBitmap(mBitmap, src, dst, mPaint);
                    canvas.save();
                    canvas.scale((float) dst.width() / (float) src.width(), (float) dst.height() / (float) src.height());
                    canvas.translate(dst.left / scale, dst.top / scale);
                    for (AFD_FSDKFace face : result) {
                        mPaint.setColor(Color.RED);
                        mPaint.setStrokeWidth(10.0f);
                        mPaint.setStyle(Paint.Style.STROKE);
                        canvas.drawRect(face.getRect(), mPaint);
                    }
                    canvas.restore();
                    mSurfaceHolder.unlockCanvasAndPost(canvas);
                    break;
                }
            }

            if (!result.isEmpty()) {
                AFR_FSDKVersion version1 = new AFR_FSDKVersion();
                AFR_FSDKEngine engine1 = new AFR_FSDKEngine();
                AFR_FSDKFace result1 = new AFR_FSDKFace();
                AFR_FSDKError error1 = engine1.AFR_FSDK_InitialEngine(FaceDB.appid, FaceDB.fr_key);
                Log.d("com.arcsoft", "AFR_FSDK_InitialEngine = " + error1.getCode());
                if (error1.getCode() != AFD_FSDKError.MOK) {
                    mHandler.sendEmptyMessage(0x02);
                }
                error1 = engine1.AFR_FSDK_GetVersion(version1);
                Log.d("com.arcsoft", "FR=" + version.toString() + "," + error1.getCode()); //(210, 178 - 478, 446), degree = 1　780, 2208 - 1942, 3370
                error1 = engine1.AFR_FSDK_ExtractFRFeature(data, mBitmap.getWidth(), mBitmap.getHeight(), AFR_FSDKEngine.CP_PAF_NV21, new Rect(result.get(0).getRect()), result.get(0).getDegree(), result1);
                Log.d("com.arcsoft", "Face=" + result1.getFeatureData()[0] + "," + result1.getFeatureData()[1] + "," + result1.getFeatureData()[2] + "," + error1.getCode());
                if(error1.getCode() == error1.MOK) {
                    mAFR_FSDKFace = result1.clone();
                    int width = result.get(0).getRect().width();
                    int height = result.get(0).getRect().height();
                    Bitmap face_bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                    Canvas face_canvas = new Canvas(face_bitmap);
                    face_canvas.drawBitmap(mBitmap, result.get(0).getRect(), new Rect(0, 0, width, height), null);
                    mHandler.sendEmptyMessage(0x03);
                }else{
                    mHandler.sendEmptyMessage(0x04);
                }
                error1 = engine1.AFR_FSDK_UninitialEngine();
                Log.d("com.arcsoft", "AFR_FSDK_UninitialEngine : " + error1.getCode());


            }else {
                mHandler.sendEmptyMessage(0x05);
            }
            err = engine.AFD_FSDK_UninitialFaceEngine();
            Log.d(TAG, "AFD_FSDK_UninitialFaceEngine =" + err.getCode());
        }
    }
}
