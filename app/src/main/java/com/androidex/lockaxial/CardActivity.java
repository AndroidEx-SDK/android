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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidex.lockaxial.bean.CardBean;
import com.androidex.lockaxial.util.HttpApi;
import com.mcxtzhang.commonadapter.lvgv.CommonAdapter;

import com.androidex.R;
import com.mcxtzhang.commonadapter.lvgv.ViewHolder;
import com.mcxtzhang.swipemenulib.SwipeMenuLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Administrator on 2018/5/15.
 */

public class CardActivity  extends Activity{
    private ListView mlv;
    private TextView title;
    private TextView house_name;
    private TextView error;

    private String houseData;
    private String currentUnit;
    private int userid;
    private String token;

    private String[] roomArray;
    private JSONArray roomJsonArray;

    private int roomid = -1;
    private int communityId = -1;

    private Dialog dialog;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 0x01: {
                    hideLoading();
                    String result = (String) msg.obj;
                    showL(result);
                    if (result != null && result.length() > 0) {
                        try {
                            JSONObject j = new JSONObject(result);
                            int code = j.getInt("code");
                            if (code == 0) {
                                JSONArray array = j.getJSONArray("data");
                                if (array != null && array.length() > 0) {
                                    List<CardBean> d = new ArrayList<>();
                                    for (int i = 0; i < array.length(); i++) {
                                        String cardName = array.getJSONObject(i).getString("cardname");
                                        String cardNumber = array.getJSONObject(i).getString("cardnumber");
                                        String createDate = array.getJSONObject(i).getString("credate");
                                        d.add(new CardBean(cardName, cardNumber, createDate));
                                    }
                                    showData(d);
                                }else{
                                    showData(null);
                                }
                            } else {
                                showData(null);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }break;
                case 0x02: {
                    hideLoading();
                    showToast("请先选择房间号");
                }break;
                case 0x03: {
                    hideLoading();
                    String result = (String) msg.obj;
                    if(result!=null && result.length()>0){
                        try{
                            JSONObject j = new JSONObject(result);
                            int code = j.getInt("code");
                            if(code == 0){
                                showToast("操作成功");
                                new getCardThread().start();
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


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card);
        houseData = getIntent().getStringExtra("data");
        currentUnit = getIntent().getStringExtra("currentUnit");
        userid = getIntent().getIntExtra("userid",-1);
        token = getIntent().getStringExtra("token");
        mlv = (ListView) findViewById(R.id.mlv);
        title = (TextView) findViewById(R.id.title);
        title.setText("门禁卡");
        house_name = (TextView) findViewById(R.id.house_name);
        error = (TextView) findViewById(R.id.error);
        if(currentUnit!=null && currentUnit.length()>0){
            try{
                JSONObject j = new JSONObject(currentUnit);
                house_name.setText(j.getString("unitName"));
                roomid = j.getInt("rid");
                communityId = j.getInt("communityId");
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
        new getCardThread().start();
    }

    private void showData(List<CardBean> data){
        if(data!=null && data.size()>0){
            mlv.setVisibility(View.VISIBLE);
            error.setVisibility(View.GONE);
            mlv.setAdapter(new CommonAdapter<CardBean>(CardActivity.this,data,R.layout.activity_card_item) {
                @Override
                public void convert(final ViewHolder viewHolder, final CardBean cardBean, int i) {
                    viewHolder.setText(R.id.name, cardBean.cardName);
                    viewHolder.setText(R.id.number, cardBean.cardNumber);
                    viewHolder.setText(R.id.createDate, HttpApi.UTCStringtODefaultString(cardBean.createDate));
                    viewHolder.setOnClickListener(R.id.btnDelete, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ((SwipeMenuLayout) viewHolder.getConvertView()).quickClose();
                            new deteleCardThread(userid,roomid,cardBean.cardNumber,communityId).start();
                        }
                    });
                }
            });
        }else{
            mlv.setVisibility(View.GONE);
            error.setVisibility(View.VISIBLE);
        }
    }

    private AlertDialog buildAlert(String[] data){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(data, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    house_name.setText(roomJsonArray.getJSONObject(i).getString("unitName"));
                    roomid = roomJsonArray.getJSONObject(i).getInt("rid");
                    communityId = roomJsonArray.getJSONObject(i).getInt("communityId");
                    new getCardThread().start();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        return builder.create();
    }

    public void selectHouse(View v){
        if(roomArray!=null && roomArray.length>0){
            buildAlert(roomArray).show();
        }
    }

    public void onBackEvent(View v){
        this.finish();
    }

    public void onAddEvent(View v){
        Intent in = new Intent(this,CardAddActivity.class);
        in.putExtra("data",houseData);
        in.putExtra("currentUnit",currentUnit);
        in.putExtra("userid",userid);
        in.putExtra("token",token);
        startActivity(in);
    }

    private void showToast(final String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CardActivity.this,msg,Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void showL(String msg){
        Log.i("xiao_",msg);
    }

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

    class getCardThread extends Thread{
        @Override
        public void run() {
            super.run();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    dialog = showLoading(CardActivity.this,"正在获取数据...");
                    dialog.show();
                }
            });
            if(roomid!=-1){
                String url = "http://www.lockaxial.com/app/rfid/getCardAccess?roomid="+roomid;
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

    class deteleCardThread extends Thread{
        private int uid;
        private int rid;
        private String cardno;
        private int cid;
        public deteleCardThread(int uid,int rid,String cardno,int cid){
            this.uid = uid;
            this.rid = rid;
            this.cardno = cardno;
            this.cid = cid;
        }
        @Override
        public void run() {
            super.run();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    dialog = showLoading(CardActivity.this,"正在删除...");
                    dialog.show();
                }
            });
            String url = "http://www.lockaxial.com/app/rfid/deleteCardAccess?communityId="+cid;
            url = url+"&cardnumber="+cardno;
            url = url+"&userid="+uid;
            url = url+"&roomid="+rid;
            String result = HttpApi.getInstance().loadHttpforGet(url,token);
            Message message = Message.obtain();
            message.what = 0x03;
            message.obj = result;
            mHandler.sendMessage(message);
        }
    }
}
