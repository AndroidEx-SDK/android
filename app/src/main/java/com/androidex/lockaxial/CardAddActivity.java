package com.androidex.lockaxial;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.androidex.R;
import com.androidex.lockaxial.util.HttpApi;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by Administrator on 2018/5/17.
 */

public class CardAddActivity extends Activity {
    private TextView title;
    private NfcAdapter mNfcAdapter;
    private PendingIntent pi;
    private EditText name;
    private TextView number;
    private TextView room;
    private String roomData;
    private String currentUnit;
    private String token;
    private String[] roomArray;
    private JSONArray roomJsonArray;

    private int userid;
    private int lockid;
    private int roomid;
    private String cardnumber;
    private String cardname;
    private int communityId;
    private String unitNo;
    private int blockid;

    private Dialog dialog;

    private String url;

    public void onSubmit(View v){
        String strName = name.getText().toString().trim();
        String strNumber = number.getText().toString().trim();
        if(strName == null || strName.length()<=0){
            Toast.makeText(this,"请输入持卡人姓名",Toast.LENGTH_SHORT).show();
            return;
        }
        if(strNumber == null || strNumber.length()<=0){
            Toast.makeText(this,"请刷入门禁卡",Toast.LENGTH_SHORT).show();
            return;
        }
        cardnumber = strNumber;
        cardname = strName;
        dialog = showLoading(this,"正在提交...");
        dialog.show();
        url = "http://www.lockaxial.com/app/rfid/appPostCard?userid="+this.userid;
        url = url +"&roomid="+this.roomid;
        url = url +"&cardnumber="+this.cardnumber;
        url = url +"&cardname="+this.cardname;
        url = url +"&communityId="+this.communityId;
        url = url +"&unitNo="+this.unitNo;
        url = url +"&blockid="+this.blockid;
        Log.i("xiao_","录卡地址："+url);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String result= HttpApi.getInstance().loadHttpforGet(url,token);
                try{
                    JSONObject j = new JSONObject(result);
                    int code = j.getInt("code");
                    resultCode(code);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cardadd);
        roomData = getIntent().getStringExtra("data");
        if(roomData!=null && roomData.length()>0){
            try{
                roomJsonArray = new JSONArray(roomData);
                roomArray = new String[roomJsonArray.length()];
                for(int i=0;i<roomJsonArray.length();i++){
                    roomArray[i] = roomJsonArray.getJSONObject(i).getString("unitName");
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        currentUnit = getIntent().getStringExtra("currentUnit");
        userid = getIntent().getIntExtra("userid",-1);
        if(userid <= 0){
            Toast.makeText(this,"请先登录...",Toast.LENGTH_SHORT).show();
            this.finish();
        }
        token = getIntent().getStringExtra("token");
        initView();
        initData();
    }

    private void initView(){
        title = (TextView) findViewById(R.id.title);
        title.setText("录卡");
        name = (EditText) findViewById(R.id.name);
        number = (TextView) findViewById(R.id.number);
        room = (TextView) findViewById(R.id.room);
        room.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(roomJsonArray!=null && roomArray!=null && roomArray.length>0){
                    buildAlert(roomArray).show();
                }
            }
        });
    }

    private void initData(){
        if(currentUnit!=null){
            try{
                JSONObject j = new JSONObject(currentUnit);
                room.setText(j.getString("unitName"));
                roomid = j.getInt("rid");
                lockid = j.getInt("blockId");
                communityId = j.getInt("communityId");
                unitNo = j.getString("unitNo");
                blockid = j.getInt("blockId");
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        pi = PendingIntent.getActivity(this, 0, new Intent(this, getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNfcAdapter.enableForegroundDispatch(this, pi, null, null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            String cardid = processIntent(intent);
            if(cardid!=null && cardid.length()>0){
                number.setText(cardid);
            }
        }
    }

    private void resultCode(final int code){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String msg = "";
                if(code == 0){
                    msg = "录卡成功";
                }else if(code == 1){
                    msg = "楼栋号不存在";
                }else if(code == 2){
                    msg = "未找到指定单元";
                }else if(code == 3){
                    msg = "卡重复录入";
                }else if(code == 4){
                    msg = "录卡失败，请联系管理员";
                }else if(code == 5){
                    msg = "录卡失败，请联系管理员";
                }else if(code == 6){
                    msg = "您不是业主，不能使用录卡功能";
                }
                showToast(msg);
                hideLoading();
                if(code == 0){
                    CardAddActivity.this.finish();
                }
            }
        });
    }

    private void hideLoading(){
        if(dialog!=null && dialog.isShowing()){
            dialog.dismiss();
            dialog = null;
        }
    }

    private void showToast(final String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CardAddActivity.this,msg,Toast.LENGTH_SHORT).show();
            }
        });
    }

    private AlertDialog buildAlert(String[] data){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(data, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                room.setText(roomArray[i]);
                try {
                    roomid = roomJsonArray.getJSONObject(i).getInt("rid");
                    lockid = roomJsonArray.getJSONObject(i).getInt("blockId");
                    communityId = roomJsonArray.getJSONObject(i).getInt("communityId");
                    unitNo = roomJsonArray.getJSONObject(i).getString("unitNo");
                    blockid = roomJsonArray.getJSONObject(i).getInt("blockId");
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        return builder.create();
    }

    private String processIntent(Intent intent) {
        String CardId = "";
        try {
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            CardId = ByteArrayToHexString(tagFromIntent.getId());
        }catch (Exception e){
            e.printStackTrace();
        }
        return CardId;
    }

    private String ByteArrayToHexString(byte[] inarray) {
        int i, j, in;
        String[] hex = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A",
                "B", "C", "D", "E", "F" };
        String out = "";
        for (j = 0; j < inarray.length; ++j) {
            in = (int) inarray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
        }
        return out;
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


    public void onBackEvent(View v){
        this.finish();
    }

}
