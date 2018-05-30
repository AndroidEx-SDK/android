package com.androidex.lockaxial;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidex.R;
import com.androidex.lockaxial.bean.CardBean;
import com.androidex.lockaxial.bean.FaceBean;
import com.androidex.lockaxial.util.HttpApi;
import com.mcxtzhang.commonadapter.lvgv.CommonAdapter;
import com.mcxtzhang.commonadapter.lvgv.ViewHolder;
import com.mcxtzhang.swipemenulib.SwipeMenuLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/5/15.
 */

public class FaceActivity extends Activity {
    private ListView mlv;
    private TextView title;
    private TextView house_name;
    private TextView error;

    private String houseData;
    private String currentUnit;

    private String[] roomArray;
    private JSONArray roomJsonArray;

    private int roomid = -1;
    private int userid;
    private String token;


    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            hideLoading();
            switch (msg.what){
                case 0x01:{
                    //获取到人脸数据
                    String result = (String) msg.obj;
                    showL("结果："+result);
                    if (result != null && result.length() > 0) {
                        List<FaceBean> data = new ArrayList<>();
                        try{
                            JSONObject j = new JSONObject(result);
                            int code = j.getInt("code");
                            if (code == 0 && j.has("data")) {
                                JSONArray array = j.getJSONArray("data");
                                if (array != null && array.length() > 0) {
                                    for(int i=0;i<array.length();i++){
                                        String imageUrl = array.getJSONObject(i).getString("imageUrl");
                                        String dataUrl = array.getJSONObject(i).getString("dataUrl");
                                        String faceName = array.getJSONObject(i).getString("faceName");
                                        String createDate = array.getJSONObject(i).getString("createDate");
                                        data.add(new FaceBean(imageUrl,dataUrl,faceName,createDate));
                                    }
                                }
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        showData(data);
                    }
                }break;
                case 0x02:{
                    showToast("请选择房间");
                }break;
                case 0x03:{
                    String result = (String) msg.obj;
                    if(result!=null && result.length()>0){
                        try{
                            JSONObject j = new JSONObject(result);
                            int code = j.getInt("code");
                            if(code == 0){
                                showToast("操作成功");
                                new getFaceThread().start();
                            }else if(code == 1){
                                showToast("操作失败，您不是业主");
                            }else if(code == 2){
                                showToast("操作失败，请联系管理员");
                            }else if(code == 3){
                                showToast("操作失败，请联系管理员");
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }break;
            }
        }
    };

    private void showData(List<FaceBean> data){
        if(data!=null && data.size()>0){
            mlv.setVisibility(View.VISIBLE);
            error.setVisibility(View.GONE);
            mlv.setAdapter(new CommonAdapter<FaceBean>(FaceActivity.this,data,R.layout.activity_face_item) {
                @Override
                public void convert(final ViewHolder viewHolder, final FaceBean faceBean, int i) {
                    viewHolder.setText(R.id.name, faceBean.faceName);
                    viewHolder.setText(R.id.createDate, faceBean.createDate);
                    ImageView imageView = viewHolder.getView(R.id.image);
                    HttpApi.loadImage(FaceActivity.this,faceBean.imageUrl,imageView);
                    viewHolder.setOnClickListener(R.id.btnDelete, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ((SwipeMenuLayout) viewHolder.getConvertView()).quickClose();
                            new deteleFaceThread(userid,faceBean.imageUrl,faceBean.dataUrl,faceBean.faceName,roomid).start();
                        }
                    });
                }
            });
        }else{
            mlv.setVisibility(View.GONE);
            error.setVisibility(View.VISIBLE);
        }
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face);
        houseData = getIntent().getStringExtra("data");
        currentUnit = getIntent().getStringExtra("currentUnit");
        userid = getIntent().getIntExtra("userid",-1);
        token = getIntent().getStringExtra("token");

        mlv = (ListView) findViewById(R.id.mlv);
        title = (TextView) findViewById(R.id.title);
        title.setText("人脸识别");
        house_name = (TextView) findViewById(R.id.house_name);
        error = (TextView) findViewById(R.id.error);

        if(currentUnit!=null && currentUnit.length()>0){
            try{
                JSONObject j = new JSONObject(currentUnit);
                house_name.setText(j.getString("unitName"));
                roomid = j.getInt("rid");
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        if(houseData!=null && houseData.length()>0){
            try{
                roomJsonArray = new JSONArray(houseData);
                roomArray = new String[roomJsonArray.length()];
                for(int i=0;i<roomJsonArray.length();i++){
                    roomArray[i] = roomJsonArray.getJSONObject(i).getString("unitName");
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        new getFaceThread().start();
    }

    public void selectHouse(View v){
        if(roomArray!=null && roomArray.length>0){
            buildAlert(roomArray).show();
        }
    }

    private void showToast(final String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(FaceActivity.this,msg,Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void showL(String msg){
        Log.i("xiao_",msg);
    }

    private Dialog dialog;
    private void hideLoading(){
        if(dialog!=null && dialog.isShowing()){
            dialog.dismiss();
            dialog = null;
        }
    }

    public Dialog showLoading(Context context, String msg) {
        Dialog dialog = null;
        dialog = new Dialog(context, R.style.image_dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View main = View.inflate(context, R.layout.dialog_main, null);
        dialog.setContentView(main);
        TextView tv = (TextView) main.findViewById(R.id.msg);
        tv.setText(msg);
        dialog.setCancelable(false);
        return dialog;
    }

    private AlertDialog buildAlert(String[] data){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(data, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    house_name.setText(roomJsonArray.getJSONObject(i).getString("unitName"));
                    roomid = roomJsonArray.getJSONObject(i).getInt("rid");
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        return builder.create();
    }

    public void onBackEvent(View v){
        this.finish();
    }

    public void onAddEvent(View v){
        Intent in = new Intent(this,FaceAddActivity.class);
        in.putExtra("data",houseData);
        in.putExtra("currentUnit",currentUnit);
        in.putExtra("userid",userid);
        in.putExtra("token",token);
        startActivity(in);
    }

    class getFaceThread extends Thread{
        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    dialog = showLoading(FaceActivity.this,"正在获取数据...");
                    dialog.show();
                }
            });
            if(roomid!=-1){
                String url = "http://www.lockaxial.com/app/rfid/getFaceDataByRoomid?roomid="+roomid;
                String result = HttpApi.getInstance().loadHttpforGet(url,token);
                Message message = Message.obtain();
                message.what = 0x01;
                message.obj = result;
                mHandler.sendMessage(message);
            }else{
                mHandler.sendEmptyMessage(0x02);
            }
        }
    }

    class deteleFaceThread extends Thread{
        private String imgUrl;
        private String daUrl;
        private int rid;
        private String fname;
        private int uid;
        public deteleFaceThread(int uid,String imgUrl,String daUrl,String fname,int rid){
            this.uid = uid;
            this.imgUrl = imgUrl;
            this.daUrl = daUrl;
            this.rid = rid;
            this.fname = fname;
        }

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    dialog = showLoading(FaceActivity.this,"正在删除...");
                    dialog.show();
                }
            });
            String url = "http://www.lockaxial.com/app/rfid/deleteFaceData?userid="+uid;
            url = url+"&roomid="+rid;
            url = url+"&imageUrl="+imgUrl;
            url = url+"&dataUrl="+daUrl;
            url = url+"&faceName="+fname;
            showL("删除："+url);
            String result = HttpApi.getInstance().loadHttpforGet(url,token);
            Message message = Message.obtain();
            message.what = 0x03;
            message.obj = result;
            mHandler.sendMessage(message);
        }
    }
}
