package com.uestc.sporttrails;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    Button btn_xoss,btn_upload,btn_igps;
    TextView textView;
    ListView listView;
    TrailsHelper trailsHelper;

    // 检查网络是否可用
    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        @SuppressLint("MissingPermission") NetworkInfo activeNetwork = manager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String long2time(long timestamp) {

        // 将时间戳转换为 Instant 对象
        Instant instant = Instant.ofEpochMilli(timestamp);

        // 转换为指定时区的 ZonedDateTime 对象，这里使用系统默认时区
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());

        // 定义日期格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        // 格式化日期
        String formattedDate = zonedDateTime.format(formatter);

//        System.out.println("格式化后的日期: " + formattedDate);
        return formattedDate;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void jsonStr2ListView(String jsonData){
//        String jsonData = response.body().string();
        Gson gson = new Gson();
        // 将 JSON 字符串转换为 JsonObject
        JsonObject jsonObject = gson.fromJson(jsonData, JsonObject.class);
//        System.out.println(jsonObject);
//                // 若要转换为 Map
//                Map<String, Object> map = gson.fromJson(jsonData, HashMap.class);
//                System.out.println(map);
        try{
            jsonObject = jsonObject.getAsJsonObject("data");
//            System.out.println("jsonObject:"+jsonObject);
        }catch (Exception e){
//            System.out.println("jsonObject:"+e.toString());
            dataList.clear();
            dataList.add("获取失败！");
            adapter.notifyDataSetChanged();
            return;
        }

        dataList.clear();
        if(jsonObject.has("rows")){
//                {"total":68,"item":[
//                  {"RideId":27326716,"MemberId":1456042,"MemberName":"成都街溜子",
//                  "MemberImg":"https://igp-zh.oss-cn-hangzhou.aliyuncs.com/1456042-356be54d-b21d-42c1-8323-5e92927b2fff",
//                  "Title":"185387836",
//                  "StartTime":"2025-03-25 03:25:03",
//                  "ThumbPath":"https://igp-zh.oss-cn-hangzhou.aliyuncs.com/9e4a4cca-073c-4caa-87e6-b1b4fc58471a.png",
//                  "RideDistance":"23.13","TotalAscent":"0","RecordTime":"1时39分","Praise":0,"IsMy":1,"RideTag":"","Metric":0},

//            {"code":0,"message":"success","data":{"pageNo":1,"pageSize":20,"totalPage":4,"totalRows":68,"rows":[
//                    {"id":"67e7d33798fb1b31c74b11bc","rideId":27357885,"exerciseType":0,
//                            "title":"185719628","startTime":"2025.03.25","rideDistance":23132.87,"totalMovingTime":5990.0,
//                            "avgSpeed":3.816,"dataStatus":1,"analysisStatus":1,
//                            "fitOssPath":"https://igp-zh.oss-cn-hangzhou.aliyuncs.com/b426909c-423d-4eae-aa4f-c8cb5e920e34.fit","label":1,"isOpen":0,"unRead":true},
            JsonArray jsonArray = jsonObject.getAsJsonArray("rows");
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject element = jsonArray.get(i).getAsJsonObject();
//                System.out.println(element.toString());
                String RideId = element.get("rideId").getAsString();
//                String MemberId = element.get("MemberId").getAsString();
//                String MemberName = element.get("MemberName").getAsString();
                String Title = element.get("title").getAsString();
                String StartTime = element.get("startTime").getAsString();
                double RideDistance = element.get("rideDistance").getAsDouble();
                String RecordTime = element.get("totalMovingTime").getAsString();
                dataList.add("IGPS"+"[ID="+RideId+"]@"+StartTime);
                textView.setText("IGPSORTS@"+jsonArray.size());
            }
        }else if(jsonObject.has("data")){
//        {"code":0,"data":{"data":[
//        {"id":185387836,"uuid":"943cd422-8815-414e-9d5c-eca8813a526b","sport":3,
//        "title":"2025-03-24 晚上 骑行","duration":5909,"distance":22559.0,"elevation_gain":77.0,"start_time":1742815464000,
//        "thumbnail":"https://static.imxingzhe.com/workout/943cd422-8815-414e-9d5c-eca8813a526b.png!workoutThumb",
//        "tss":0.0,"loc_source":1,"has_cadence":false,"has_heartrate":false,"has_power":false,"credits":23511,"avg_speed":13.716,"hidden":0},

            JsonArray jsonArray = jsonObject.getAsJsonArray("data");
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject element = jsonArray.get(i).getAsJsonObject();
//                System.out.println(element.toString());
                String RideId = element.get("id").getAsString();
                String uuid = element.get("uuid").getAsString();
                String Title = element.get("title").getAsString();
                long StartTime = element.get("start_time").getAsLong();
                double RideDistance = element.get("distance").getAsDouble();
                String RecordTime = element.get("duration").getAsString();
                dataList.add("XOSS"+"[ID="+RideId+"]@"+long2time(StartTime));
                textView.setText("XOSS@"+jsonArray.size());
            }
        }
        adapter.notifyDataSetChanged();
    }

    private Handler handler = new ThreadHandler();
    private String flag_btn = "", ui_string="";
    @SuppressLint("HandlerLeak")
    class ThreadHandler extends Handler {
        @RequiresApi(api = Build.VERSION_CODES.O)
        public void handleMessage(Message msg){
//            Toast.makeText(getBaseContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
//            Toast toast = new Toast(getApplicationContext());
//////            toast = Toast.makeText(getApplicationContext(), null, LENGTH_SHORT);
////            toast.setText(msg.obj.toString());
////            toast.show();
//            Toast.makeText(getApplicationContext(), msg.obj.toString(), LENGTH_SHORT).show();
            
            flag_btn = msg.obj.toString();

            if (flag_btn=="refresh_ui"){
                if (ui_string!="") {
                    jsonStr2ListView(ui_string);
                    ui_string="";
                }
            }else if(flag_btn.contains("login") || flag_btn.contains("upload")) new Thread(new Runnable() {
                @Override
                public void run() {
                    if(flag_btn=="login_xoss"){
                        try {
                            ui_string = trailsHelper.loginXOSS();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
//                        // 创建 Intent
//                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                        intent.setType("*/*");
//                        intent.addCategory(Intent.CATEGORY_OPENABLE);
//                        try {
//                            intent.putExtra("android.content.extra.FOLDER_NAME", trailsHelper.getAppPath()); // 部分设备支持
//                            startActivity(Intent.createChooser(intent, "选择文件管理器"));
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                            Log.e("Main", "未找到文件管理器应用");
//                        }


                    }else if(flag_btn=="login_igps"){
                        try {
                            ui_string = trailsHelper.loginIGPS();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }else if(flag_btn=="upload_igps"){
                        try {
                            trailsHelper.uploadGPXFiles();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    flag_btn = "";
                    Message msg = handler.obtainMessage();
                    msg.obj = "refresh_ui";
                    handler.sendMessage(msg);
                }
            }).start();
            else{
//                Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
                textView.setText(msg.obj.toString());
            }
//            textView.setText(info);
        }
    }

    private ArrayAdapter<String> adapter;
    private List<String> dataList;
    UtilPreferences preferences;
    EditText xoss_username, xoss_password, igps_username, igps_password;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textview);

        btn_xoss = findViewById(R.id.btn_xoss);
        btn_xoss.setOnClickListener(this);

        btn_igps = findViewById(R.id.btn_igps);
        btn_igps.setOnClickListener(this);

        btn_upload = findViewById(R.id.btn_upload);
        btn_upload.setOnClickListener(this);

        listView = findViewById(R.id.listview);
        listView.setScrollbarFadingEnabled(true);

        dataList = new ArrayList<>();
        dataList.add("1.Support check XOSS");
        dataList.add("2.Support check IGPSPORT");
        dataList.add("2.Support XOSS --> IGPSPORT");
//        for(int i=0;i<5;i++){
//            dataList.add("Item "+i);
//        }
        // 创建适配器
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataList);

        // 设置适配器
        listView.setAdapter(adapter);
//        adapter.notifyDataSetChanged();


        // 使用示例
//        if (isNetworkAvailable()) {
//            // 执行网络请求
//            textView.setText("有网络连接");
//            Toast.makeText(getBaseContext(), "有网络连接", Toast.LENGTH_SHORT).show();
//        } else {
//            textView.setText("无网络连接");
//            Toast.makeText(getBaseContext(), "无网络连接", Toast.LENGTH_SHORT).show();
//        }

        getPermissions();
        preferences = new UtilPreferences(this);
        // 个人信息
        xoss_username = findViewById(R.id.xoss_username);
        xoss_password = findViewById(R.id.xoss_password);
        igps_username = findViewById(R.id.igps_username);
        igps_password = findViewById(R.id.igps_password);
        xoss_username.setText(preferences.getParam("xoss_username", "").toString());
        xoss_password.setText(preferences.getParam("xoss_password", "").toString());
        igps_username.setText(preferences.getParam("igps_username", "").toString());
        igps_password.setText(preferences.getParam("igps_password", "").toString());

        trailsHelper = new TrailsHelper(getApplicationContext(), handler=handler);
    }

    @Override
    public void onClick(View v) {
        Toast.makeText(getApplicationContext(), xoss_username.getText()+":"+xoss_password.getText(), Toast.LENGTH_LONG).show();
        preferences.saveParam("xoss_username", xoss_username.getText().toString().trim());
        preferences.saveParam("xoss_password", xoss_password.getText().toString().trim());
        preferences.saveParam("igps_username", igps_username.getText().toString().trim());
        preferences.saveParam("igps_password", igps_password.getText().toString().trim());
        if(v.getId()==R.id.btn_xoss){
            Message msg = handler.obtainMessage();
            msg.obj = "login_xoss";
            handler.sendMessage(msg);
        }
        else if(v.getId()==R.id.btn_igps){
            Message msg = handler.obtainMessage();
            msg.obj = "login_igps";
            handler.sendMessage(msg);
        }
        else if(v.getId()==R.id.btn_upload){
            Message msg = handler.obtainMessage();
            msg.obj = "upload_igps";
            handler.sendMessage(msg);
        }
    }


    private void getPermissions(){
        List<String> permissionList = new ArrayList<>();
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)!= PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)!= PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.CALL_PHONE);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE)!= PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.VIBRATE);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.CAMERA);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)!= PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.INTERNET);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)!= PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_WIFI_STATE);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE)!= PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_NETWORK_STATE);
        }
        if(!permissionList.isEmpty()){
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(this, permissions, 1);
        }

//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 1);
//        }
//        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//        Uri uri = Uri.fromParts("package", getPackageName(), null);
//        intent.setData(uri);
//        startActivity(intent);
    }

}