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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    Button btn_xoss,btn_upload,btn_igps;
    TextView textView;
    ListView listView;
    TrailsHelper trailsHelper;
    GpxEleHelper gpxEleHelper;

    // 检查网络是否可用
    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        @SuppressLint("MissingPermission") NetworkInfo activeNetwork = manager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
    private void log2textview(String text) {
        // 追加新内容
        if(text.startsWith("\r")){
            textView.append(text);
        }else
            textView.append("\n"+text);
        // 滚动 ScrollView 到底部
        scroll_textView.post(() -> scroll_textView.fullScroll(View.FOCUS_DOWN));
    }
    private void log2handler(String log){
        Message msg = handler.obtainMessage();
        msg.obj = log;
        handler.sendMessage(msg);
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
//        log2handler(jsonData);
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

//                Map<String, Object> map = new HashMap<>();
//                map.put("platform", "IGPS");
//                map.put("id", RideId);
//                map.put("time", StartTime);
//                dataList.add(gson.toJson(map));
                dataList.add("iGPS"+"#"+RideId+"@"+StartTime);
            }
            log2textview("IGPSORTS@最新轨迹数量："+jsonArray.size());
        }else if(jsonObject.has("data")){
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
                dataList.add("XOSS"+"#"+RideId+"@"+long2time(StartTime));
//                Map<String, Object> map = new HashMap<>();
//                map.put("platform", "IGPS");
//                map.put("id", RideId);
//                map.put("time", StartTime);
//                dataList.add(gson.toJson(map));
            }
            log2textview("XOSS@最新轨迹数量："+jsonArray.size());
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
            }else if(flag_btn.startsWith("download_xoss")){
                try {
//                    log2handler("正在登录XOSS...");
//                    ui_string = trailsHelper.loginXOSS();
                    log2handler("准备下载XOSS...");
                    // downloadGpxFromXoss
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String idstr = flag_btn.split("_")[2];
                            log2handler("正在下载最新GPX@ID="+idstr);
                            boolean flag_download = trailsHelper.download_gpxfile_byid(idstr);
                            try {
                                File gpxFile = new File(trailsHelper.getAppPath(), trailsHelper.gpx_name);
                                if(flag_download && gpxFile.exists()){
                                    Log.i("ELE", gpxFile.getAbsolutePath());
                                    log2handler("正在生成海拔数据...");
                                    gpxEleHelper.processGpxFile(gpxFile);

                                    File eleFile = new File(trailsHelper.getAppPath(), trailsHelper.current_RideId + ".gpx");
                                    while (gpxEleHelper.flag_running || !eleFile.exists()){
                                        Thread.sleep(1);
                                    }
                                    log2handler("正在上传文件给iGPSPORT...");
                                    trailsHelper.uploadGPXFiles();
                                }
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else if(flag_btn.contains("login") || flag_btn.contains("upload")) new Thread(new Runnable() {
                @Override
                public void run() {
                    if(flag_btn=="login_xoss"){
                        try {
                            log2handler("正在登录XOSS...");
                            ui_string = trailsHelper.loginXOSS();

//                            // downloadGpxFromXoss
//                            new Thread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    log2handler("正在下载最新GPX...");
//                                    boolean flag_download = trailsHelper.download_latest_gpxfile();
////                                    log2handler("下载最新GPX完成！");
//                                    File gpxFile = null;
//                                    try {
//                                        gpxFile = new File(trailsHelper.getAppPath(), trailsHelper.gpx_name);
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                    }
//                                    if(flag_download && gpxFile.exists()){
//                                        Log.i("ELE", gpxFile.getAbsolutePath());
//                                        log2handler("正在生成海拔数据...");
//                                        gpxEleHelper.processGpxFile(gpxFile);
//                                    }
//                                }
//                            }).start();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // 打开文件所在文件夹
                        File folder = null;
                        try {
                            folder = trailsHelper.getAppPath();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        log2handler("文件夹："+folder.getAbsolutePath());
                    }else if(flag_btn=="login_igps"){
                        try {
                            log2handler("正在登录IGPS...");
                            ui_string = trailsHelper.loginIGPS();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
//                    }else if(flag_btn=="upload_igps"){
//                        try {
//                            log2handler("正在上传文件给iGPSPORT...");
//                            trailsHelper.uploadGPXFiles();
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
                    }

                    flag_btn = "";
                    Message msg = handler.obtainMessage();
                    msg.obj = "refresh_ui";
                    handler.sendMessage(msg);
                }
            }).start();
            else{
//                Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
                log2textview(msg.obj.toString());
            }
//            textView.setText(info);
        }
    }

    private ArrayAdapter<String> adapter;
    private List<String> dataList;
    UtilPreferences preferences;
    EditText xoss_username, xoss_password, igps_username, igps_password;
    ScrollView scroll_textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scroll_textView = findViewById(R.id.scroll_textView);
        textView = findViewById(R.id.textview);
        textView.setMovementMethod(new ScrollingMovementMethod()); // 启用滚动

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

        // 创建适配器
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataList);

        // 设置适配器
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = (String) parent.getItemAtPosition(position);
//            log2textview("点击了" + parent.getItemAtPosition(position));
//            log2textview("点击了" + dataList.get(position));
//            log2textview("点击了" + dataList.get((int) id));
//            Gson gson = new Gson();
//            Type type = new TypeToken<Map<String, Object>>() {}.getType();
//            Map<String, Object> parsedMap = gson.fromJson(selectedItem, type);
//            System.out.println("处理ID：" + parsedMap.get("id"));
            if(selectedItem.contains("XOSS")){
                if(trailsHelper.flag_running)
                    Toast.makeText(this, "正在下载轨迹，请稍后!", Toast.LENGTH_SHORT).show();
                else if(gpxEleHelper.flag_running)
                    Toast.makeText(this, "正在转换海拔，请稍后!", Toast.LENGTH_SHORT).show();
                else{
                    String idstr = selectedItem.split("@")[0].split("#")[1];
                    Toast.makeText(this, "点击了：" + idstr, Toast.LENGTH_SHORT).show();
                    log2handler("选择"+selectedItem);
                    Message msg = handler.obtainMessage();
                    msg.obj = "download_xoss_"+idstr;
                    handler.sendMessage(msg);
                }
            }else
                Toast.makeText(this, "暂时不支持iGP!", Toast.LENGTH_SHORT).show();
        });


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
        gpxEleHelper = new GpxEleHelper(handler=handler);
        trailsHelper = new TrailsHelper(getApplicationContext(), handler=handler);

        // 个人信息
        xoss_username = findViewById(R.id.xoss_username);
        xoss_password = findViewById(R.id.xoss_password);
        igps_username = findViewById(R.id.igps_username);
        igps_password = findViewById(R.id.igps_password);
        xoss_username.setText(preferences.getParam("xoss_username", "").toString());
        xoss_password.setText(preferences.getParam("xoss_password", "").toString());
        igps_username.setText(preferences.getParam("igps_username", "").toString());
        igps_password.setText(preferences.getParam("igps_password", "").toString());
    }

    @Override
    public void onClick(View v) {
//        Toast.makeText(getApplicationContext(), xoss_username.getText()+":"+xoss_password.getText(), Toast.LENGTH_LONG).show();
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

    }

}