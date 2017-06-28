package com.androidex.lockxial;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.androidex.R;
import com.androidex.lockxial.config.DeviceConfig;
import com.androidex.lockxial.service.MainService;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class InboundActivity extends Activity{
    public static final int MSG_RTC_ONVIDEO=10001;
    public static final int MSG_RTC_DISCONNECT=10002;
    public static final int MSG_RTC_CONNECTED=10003;
    public static final int MSG_IMG_CHANGE=20001;
    public static final int MSG_APPEND_IMAGE=20002;

    //public static final int MSG_CANCEL_CALL_COMPLETE=10012;

    public static final int CALL_MODE=1;
    public static final int ONCONNECTED_MODE=2;
    public static final int ONVIDEO_MODE=4;
    public static final int ERROR_MODE=6;

    protected Messenger serviceMessenger;
    protected Messenger inboundMessenger;
    protected Handler handler=null;

    public static int currentStatus=CALL_MODE;
    public static Activity instance=null;

    TextView callingText=null;
    TextView switchMicText=null;
    ImageView guestImage = null;
    ImageView voiceImage=null;

    FrameLayout videoLayout= null;
    SurfaceView remoteView = null;
    SurfaceView localView = null;
    LinearLayout remoteLayout= null;
    LinearLayout localLayout= null;

    RelativeLayout callingLayout=null;
    RelativeLayout speakingLayout=null;
    LinearLayout openDoorLayout=null;
    LinearLayout openDoorAndCloseLayout=null;

    String imageUrl=null;
    String unitName=null;
    String communityName=null;
    String lockName=null;
	
	Thread checkDialStatusThread=null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbound);
        instance=this;
        initData();
        initScreen();
        initHandler();
        Intent intent = new Intent(InboundActivity.this,MainService.class);
        bindService(intent,connection,0);
		startCheckDialStatus();
    }
	
	protected void startCheckDialStatus(){
        checkDialStatusThread=new Thread(){
            public void run(){
                try {
                    Thread.sleep(1000*30);
                } catch (InterruptedException e) {
                }
                if(currentStatus==CALL_MODE){
                    sendMainMessenge(MainService.MSG_REJECT_CALL);
                }
                checkDialStatusThread=null;
            }
        };
        checkDialStatusThread.start();
    }
	
    protected void initData(){
        Intent getIntent = getIntent();
        imageUrl = getIntent.getStringExtra("imageUrl");
        unitName = getIntent.getStringExtra("unitName");
        communityName = getIntent.getStringExtra("communityName");
        lockName = getIntent.getStringExtra("lockName");
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        sendMainMessenge(MainService.MSG_REJECT_CALL);
        unbindService(connection);
		if(checkDialStatusThread!=null){
            try {
                checkDialStatusThread.interrupt();
            }catch(Exception e){}
            checkDialStatusThread=null;
        }
    }

    private void initHandler(){
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
            if(msg.what == MSG_RTC_ONVIDEO){
                onRtcVideoOn();
            }else if(msg.what==MSG_RTC_CONNECTED){
                onRtcConnected();
            }else if(msg.what == MSG_RTC_DISCONNECT){
                onRtcDisconnect();
            }else if(msg.what==MSG_IMG_CHANGE){
                Bitmap bitmap=(Bitmap)msg.obj;
                guestImage.setImageBitmap(bitmap);
                voiceImage.setImageBitmap(bitmap);
            }else if(msg.what==MSG_APPEND_IMAGE){
                String imageUrl=(String)msg.obj;
                setImageView(imageUrl);
            }
            }
        };
        inboundMessenger =new Messenger(handler);
    }

    protected void sendMainMessenge(int code){
        Message message = Message.obtain();
        message.what = code;
        try {
            serviceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    protected void sendMainMessenge(int code,Object object){
        Message message = Message.obtain();
        message.what = code;
        message.obj=object;
        try {
            serviceMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    protected void sendInboundMessenge(int code, Object object){
        Message message = Message.obtain();
        message.what = code;
        message.obj=object;
        try {
            inboundMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //获取Service端的Messenger
            serviceMessenger = new Messenger(service);
            Message message = Message.obtain();
            message.what = MainService.REGISTER_ACTIVITY_INBOUND;
            message.replyTo = inboundMessenger;
            try {
                //通过ServiceMessenger将注册消息发送到Service中的Handler
                serviceMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    protected void initScreen(){
        videoLayout=(FrameLayout) findViewById(R.id.videoLayout);
        remoteLayout=(LinearLayout) findViewById(R.id.remoteLayout);
        localLayout=(LinearLayout) findViewById(R.id.localLayout);
        speakingLayout=(RelativeLayout) findViewById(R.id.speakingLayout);
        callingLayout=(RelativeLayout) findViewById(R.id.callingLayout);
        callingLayout.setVisibility(View.VISIBLE);
        speakingLayout.setVisibility(View.INVISIBLE);
        callingText=(TextView) findViewById(R.id.callingText);
        callingText.setText(communityName+lockName);
        switchMicText=(TextView) findViewById(R.id.switchMicText);
        switchMicText.setText("免提");
        guestImage = (ImageView) findViewById(R.id.guestImage);
        voiceImage=(ImageView) findViewById(R.id.voiceImage);
        if(imageUrl!=null&&!imageUrl.equals("")){
            setImageView(imageUrl);
        }
        openDoorLayout=(LinearLayout) findViewById(R.id.openDoorLayout);;
        openDoorAndCloseLayout=(LinearLayout) findViewById(R.id.openDoorAndCloseLayout);
        if(lockName.equals("管理中心")){
            openDoorLayout.setVisibility(View.GONE);
            openDoorAndCloseLayout.setVisibility(View.GONE);
        }else{
            localLayout.setVisibility(View.GONE);
        }
    }

    /**
     * 根据图片的url路径获得Bitmap对象
     * @param url
     * @return
     */
    private Bitmap returnBitmap(String url) {
        URL fileUrl = null;
        Bitmap bitmap = null;
        try {
            fileUrl = new URL(convertImageUrl(url));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            HttpURLConnection conn = (HttpURLConnection) fileUrl
                    .openConnection();
            conn.setDoInput(true);
            conn.connect();
            InputStream is = conn.getInputStream();
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private void setImageView(final String url){
        new Thread(){
            public void run(){
                Bitmap bitmap = returnBitmap(url);
                if(bitmap!=null){
                    sendInboundMessenge(MSG_IMG_CHANGE,bitmap);
                }
            }
        }.start();
    }
    private String convertImageUrl(String url){
        if(url!=null && url.length()>4 && (!url.substring(0,4).toLowerCase().equals("http"))){
            url= DeviceConfig.SERVER_URL+url;
        }
        return url;
    }

    synchronized void setCurrentStatus(int status) {
        currentStatus=status;
    }

    void setTextView(int id,String txt) { ((TextView)findViewById(id)).setText(txt); }

    void initVideoViews() {
        if (remoteView !=null) return;
        if(MainService.callConnection != null) {
            remoteView = (SurfaceView) MainService.callConnection.createVideoView(false, this, true);
            localView=(SurfaceView) MainService.callConnection.createVideoView(true, this, true);
        }
        remoteLayout.addView(remoteView);
        remoteView.setKeepScreenOn(true);
        remoteView.setZOrderMediaOverlay(true);
        remoteView.setZOrderOnTop(true);

        localLayout.addView(localView);
        localView.setKeepScreenOn(true);
        localView.setZOrderMediaOverlay(true);
        localView.setZOrderOnTop(true);
    }

    public void onRtcVideoOn(){
        setCurrentStatus(ONVIDEO_MODE);
        initVideoViews();
        MainService.callConnection.buildVideo(remoteView);
        videoLayout.setVisibility(View.VISIBLE);
    }

    public void onRtcDisconnect(){
        setCurrentStatus(CALL_MODE);
        videoLayout.setVisibility(View.INVISIBLE);
        close();
    }

    public void onRtcConnected(){
        setCurrentStatus(ONCONNECTED_MODE);
    }

    protected void close(){
        if(instance!=null){
            instance.finish();
            instance=null;
        }
    }

    public void rejectCall(View view){
        close();
    }

    public void acceptCall(View view){
        sendMainMessenge(MainService.MSG_OPEN_RTC,"video");
        callingLayout.setVisibility(View.INVISIBLE);
        voiceImage.setVisibility(View.INVISIBLE);
        videoLayout.setVisibility(View.VISIBLE);
        speakingLayout.setVisibility(View.VISIBLE);
    }

    public void acceptCallVoice(View view){
        sendMainMessenge(MainService.MSG_OPEN_RTC,"voice");
        callingLayout.setVisibility(View.INVISIBLE);
        voiceImage.setVisibility(View.VISIBLE);
        videoLayout.setVisibility(View.INVISIBLE);
        speakingLayout.setVisibility(View.VISIBLE);
    }

    public void openDoor(View view){
        sendMainMessenge(MainService.MSG_OPEN_DOOR);
        close();
    }

    public void switchMic(View view){
        if(switchMicText.getText().equals("免提")){
            switchMicText.setText("听筒");
        }else{
            switchMicText.setText("免提");
        }
        sendMainMessenge(MainService.MSG_SWITCH_MIC);
    }

    public void doOpen(View view){
        openDoor(view);
    }

    public void donotOpen(View view){

    }
}
