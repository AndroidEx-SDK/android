package com.androidex.lockxial;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.androidex.R;
import com.androidex.lockxial.service.MainService;
import com.reactnativenavigation.controllers.SplashActivity;

public class MainActivity extends SplashActivity {
    protected Messenger mainMessenger;
    protected Messenger serviceMessenger;
    protected Handler handler=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initHandler();
        startMainService();
    }

    @Override
    public int getSplashLayout() {
        return R.layout.launch_screen;
    }

    private void initHandler(){
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
            }
        };
        mainMessenger=new Messenger(handler);
    }

    public void unbindService(){
        unbindService(connection);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        try{
            unbindService();
        }catch(Exception e){}
    }

    protected void startMainService(){
        Intent intent = new Intent(MainActivity.this,MainService.class);
        startService(intent);
        bindService(intent, connection, Service.BIND_AUTO_CREATE);
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //获取Service端的Messenger
            serviceMessenger = new Messenger(service);
            //创建消息
            Message message = Message.obtain();
            message.what = MainService.REGISTER_ACTIVITY_MAIN;
            message.replyTo = mainMessenger;
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
}
