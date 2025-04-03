package com.uestc.sporttrails;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider;
import com.alibaba.sdk.android.oss.model.ObjectMetadata;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Cipher;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class TrailsHelper {
    public static String current_RideId="";
    public static int current_distance=0;
//    # trkpt/km: 222.43845252051582

    public static String gpx_name="sports_trail.gpx";
    private static String token_xoss="";
    private static String token_igp="";
    private static OkHttpClient client;
    private Context context;
    public static Handler handler;
    UtilPreferences preferences;
    public TrailsHelper(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;
    }

    private static String xoss_username="", xoss_password="", igps_username="", igps_password="";
    public void refresh_identity(){
        preferences = new UtilPreferences(context);
        xoss_username = (String) preferences.getParam("xoss_username", "");
        xoss_password = (String) preferences.getParam("xoss_password", "");
        igps_username = (String) preferences.getParam("igps_username", "");
        igps_password = (String) preferences.getParam("igps_password", "");
    }

    public static File getAppPath() throws IOException {
//        File gpxDir = new File(context.getFilesDir(), "SportsTrails");// 获取应用内部存储的 files 目录
        // 获取公共下载目录
        File publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File gpxDir = new File(publicDir, "SportsTrails");

        // 确保目录存在（关键修复点）
        if (!gpxDir.exists() && !gpxDir.mkdirs()) {
            throw new IOException("无法创建目录: " + gpxDir.getAbsolutePath());
        }

        if (!gpxDir.exists() || !gpxDir.isDirectory()) {
//            Log.e("OSS", "目录不存在或不是有效目录");
//            Message msg = handler.obtainMessage();
//            msg = handler.obtainMessage();
//            msg.obj = "目录不存在或不是有效目录！";
//            handler.sendMessage(msg);
        }else{
//            Log.e("OSS", "目录存在或是有效目录");
//            Message msg = handler.obtainMessage();
//            msg = handler.obtainMessage();
//            msg.obj = "目录存在或是有效目录！";
//            handler.sendMessage(msg);
        }
        return gpxDir;
    }

    static {
        // 创建 OkHttpClient 实例，使用 CookieJar 管理会话状态
        client = new OkHttpClient.Builder()
                .cookieJar(new CookieJar() {
                    private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        cookieStore.put(url.host(), cookies);
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        List<Cookie> cookies = cookieStore.get(url.host());
                        return cookies != null ? cookies : new java.util.ArrayList<>();
                    }
                })
                .build();
    }

    /**
     * 发送 GET 请求
     * @param url 请求的 URL
     * @param headers 请求头参数
     * @return 响应结果
     * @throws IOException 网络请求异常
     */
    public static String get(String url, Map<String, String> headers) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url);

        // 添加请求头
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        Request request = requestBuilder.build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            }
            return null;
        }
    }

    /**
     * 发送 POST 请求
     * @param url 请求的 URL
     * @param headers 请求头参数
     * @param body 请求体参数
     * @return 响应结果
     * @throws IOException 网络请求异常
     */
    public static String post(String url, Map<String, String> headers, String body) throws IOException {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(body, JSON);

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(requestBody);

        // 添加请求头
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        Request request = requestBuilder.build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            }
            return null;
        }
    }


    public String loginIGPS() throws IOException, JSONException {
//        MediaType JSON = MediaType.get("application/json; charset=utf-8");
//        RequestBody requestBody = RequestBody.create(body, JSON);
        refresh_identity();
        RequestBody requestBody = new FormBody.Builder()
                .add("username", igps_username)
                .add("password", igps_password)
                .build();
        Request.Builder requestBuilder = new Request.Builder()
                .url("https://my.igpsport.com/Auth/Login")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .addHeader("Referer", "https://my.igpsport.com/")
                .post(requestBody);

        Request request = requestBuilder.build();
        Log.i("IGP-LOGIN", request.toString());
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                Log.i("IGP", response.toString());
//                return response.body().string();

                ResponseBody responseBody = response.body();
                JSONObject result = new JSONObject(responseBody.string());

//                token_igp = result.getString("token");
                HttpUrl httpUrl = response.request().url();
                List<Cookie> cookies = client.cookieJar().loadForRequest(httpUrl);
                for (Cookie cookie : cookies) {
                    if ("loginToken".equals(cookie.name())) {
                        token_igp = cookie.value();
                    }
                }
            }
//            return null;
        }

//        String query_url = "https://my.igpsport.com/Activity/ActivityList";
        String query_url = "https://prod.zh.igpsport.com/service/web-gateway/web-analyze/activity/queryMyActivity?pageNo=1&pageSize=20&reqType=0&sort=1";
        request = new Request.Builder()
                .url(query_url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .addHeader("Referer", "https://my.igpsport.com/")
                .addHeader("Authorization", "Bearer " + token_igp)
                .build();
        Log.i("IGP-GET", request.toString());
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                Log.i("IGP", response.toString());
                Log.i("IGP-body", response.body().toString());
                Log.i("IGP-msg", response.message());
                Log.i("IGP-code", String.valueOf(response.code()));
//                Log.i("IGP", String.valueOf(response.trailers()));

//                MediaType JSON = MediaType.get("application/json; charset=utf-8");
//                RequestBody requestBody = RequestBody.create(response.body(), JSON);
                return response.body().string();
            }
            return null;
        }
    }


    private static final String XOSS_PUBLIC_KEY =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDmuQkBbijudDAJgfffDeeIButq" +
            "WHZvUwcRuvWdg89393FSdz3IJUHc0rgI/S3WuU8N0VePJLmVAZtCOK4qe4FY/eKm" +
            "WpJmn7JfXB4HTMWjPVoyRZmSYjW4L8GrWmh51Qj7DwpTADadF3aq04o+s1b8LXJa" +
            "8r6+TIqqL5WUHtRqmQIDAQAB";

    // RSA Encryption
    public static String encryptPassword(String password) throws Exception {
        byte[] keyBytes = Base64.decode(XOSS_PUBLIC_KEY, Base64.DEFAULT);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(spec);

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(password.getBytes());
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
    }

    public boolean download_latest_gpxfile(String idStr){
        Gson gson = new Gson();
        // 将 JSON 字符串转换为 JsonObject
        JsonObject jsonObject = gson.fromJson(str_rtn, JsonObject.class);
        JsonArray jsonArray = jsonObject.getAsJsonObject("data").getAsJsonArray("data");
        JsonObject element = null;
        for(int i=0;i< jsonArray.size();i++){
            element = jsonArray.get(i).getAsJsonObject();
            if(element.get("id").getAsString()==idStr)break;
        }
        System.out.println("Downloading:"+element.toString());
        String RideId = element.get("id").getAsString();
        String uuid = element.get("uuid").getAsString();
        current_RideId = RideId;
        current_distance = (int) element.get("distance").getAsDouble();
        // 使用 File 构造文件路径（避免字符串拼接错误）
        File outputFile = null;
        try {
            outputFile = new File(getAppPath(), gpx_name);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(outputFile.exists()){
            Message msg = handler.obtainMessage();
            msg.obj = "文件覆盖保存:"+outputFile.getName();
            handler.sendMessage(msg);
            outputFile.delete();
//                            return;
        }

//                        https://www.imxingzhe.com/api/v1/pgworkout/185790375/gpx/
        String fileUrl = "https://www.imxingzhe.com/api/v1/pgworkout/" + RideId + "/gpx/";
        System.out.println("downloading: " + fileUrl);
        Request fileRequest = new Request.Builder()
                .url(fileUrl)
                .build();
        Response fileResponse = null;
        try {
            fileResponse = client.newCall(fileRequest).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (fileResponse.code() == 200) {
            System.out.println("下载到: " + outputFile);
            try {
                writeFile(outputFile, fileResponse.body().bytes());
                System.out.println("文件写入成功: " + outputFile);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("文件写入失败: " + outputFile);
            }
        } else {
            System.out.println("文件请求失败: " + fileResponse.code() + outputFile);
        }
//                I/System.out: 下载到: /storage/emulated/0/Download/SportsTrails/185387836.gpx
//                I/System.out: 文件保存成功: /storage/emulated/0/Download/SportsTrails/185387836.gpx
        return false;
    }

    String str_rtn=null;
    public String loginXOSS() throws Exception {
//        MediaType JSON = MediaType.get("application/json; charset=utf-8");
//        RequestBody requestBody = RequestBody.create(body, JSON);
        refresh_identity();
        String safe_password = encryptPassword(xoss_password);
        System.out.println("safe_password:"+safe_password);
        RequestBody requestBody = new FormBody.Builder()
                .add("account", xoss_username)
                .add("password", safe_password)
                .build();
        Request.Builder requestBuilder = new Request.Builder()
                .url("https://www.imxingzhe.com/api/v1/user/login/")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .addHeader("Referer", "https://www.imxingzhe.com/")
                .post(requestBody);

        Request request = requestBuilder.build();
        Log.i("XOSS-LOGIN", request.toString());
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                Log.i("XOSS", response.toString());
//                return response.body().string();

                // Extract login token from cookies
                for (Cookie cookie : Cookie.parseAll(request.url(), response.headers())) {
                    if ("loginToken".equals(cookie.name())) {
                        token_xoss = cookie.value();
                        Log.d("XOSS:", token_xoss);
                        break;
                    }
                }
            }
//            return null;
        }

        request = new Request.Builder()
                .url("https://www.imxingzhe.com/api/v1/pgworkout/?offset=0&limit=20&sport=3&year=&month=")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .addHeader("Referer", "https://www.imxingzhe.com/")
                .build();
//        Log.i("XOSS-GET", request.toString());
        str_rtn=null;
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
//                Log.i("XOSS", response.toString());
//                Log.i("XOSS-body", response.body().toString());
//                Log.i("XOSS-msg", response.message());
//                Log.i("XOSS-code", String.valueOf(response.code()));
                str_rtn = response.body().string();
//                Log.i("XOSS", str_rtn);

//                MediaType JSON = MediaType.get("application/json; charset=utf-8");
//                RequestBody requestBody = RequestBody.create(response.body(), JSON);

            }
            return str_rtn;
        }
    }

    private void writeFile(File filename, byte[] data) throws IOException {
        if(filename.exists()){
            Message msg = handler.obtainMessage();
            msg.obj = "文件已存在:"+filename.getName();
            handler.sendMessage(msg);
            return;
        }
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(data);
            Message msg = handler.obtainMessage();
            msg.obj = "文件下载成功:"+filename.getName();
            handler.sendMessage(msg);
        }
    }

    // 修改创建 OSS 头部的方法
    public Map<String, String> createOSSHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "*/*");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9");
        headers.put("Origin", "https://app.zh.igpsport.com");
        headers.put("Referer", "https://app.zh.igpsport.com/");
        headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36");
        headers.put("x-oss-user-agent", "aliyun-sdk-js/6.22.0 Chrome 134.0.0.0 on OS X 10.15.7 64-bit");
        return headers;
    }

    public static String getStsToken(String loginToken) {
        String url = "https://prod.zh.igpsport.com/service/mobile/api/AliyunService/GetOssTokenForApp";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + loginToken)
                .build();
//        Log.i("STS", request.toString());
        try (Response response = client.newCall(request).execute()) {
            Log.i("STS", response.toString());
            if (response.isSuccessful() && response.body() != null) {
                String jsonData = response.body().string();
                return  jsonData;
            }
        } catch (IOException e) {
            System.out.println("请求失败: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("JSON 解析失败: " + e.getMessage());
        }
        return null;
    }

    // OSS Upload
    public void upload2oss(String loginToken, File file, String ossName) {
//        String accessKeyId = "STS.NXTMKzY5N3kRYY65XjPW4K73a";
//        String accessKeySecret = "5NAJoBQD2Ek9yHBn2WpSSscViACFKyujTKy2UcNTBrqn";
//        String securityToken = "CAISwwJ1q6Ft5B2yfSjIr5vhBvHOtOpvhKm5W3+H0Vg/XNgYpPKYgzz2IH1NdHRsBeEWsvo3mmFZ7/YblqFyZKd+fWv+UZO4HwrZRFnzDbDasumZsJYm6vT8a0XxZjf/2MjNGZabKPrWZvaqbX3diyZ32sGUXD6+XlujQ/br4NwdGbZxZASjaidcD9p7PxZrrNRgVUHcLvGwKBXn8AGyZQhK2lck0zgvtPjv+KDGtEqC1m+d4/QOuoH8LqKja8RRJ5plW7+3prcrL/qcjHIBu0QUrPkn3fUUoC20t9WcEkRX5A6dL+3X/9tgIQl0fKEmHLReq/zxhUG4KJM5/qaAKHYlVYk9O0y3z7B4z3V/y82DgncopxuK2mVMozF+DOOBlykOT7w1Wg1klbTfBFG+B1LSpL8USMuFvGSuSGPF78C9VZRGj/IdpxqAAYrfIi5UNNFZa/zfkbhgAJ9CRTSrLbiLdjaENkuHQL9bga0Pd7qXyRa78qhZhZZhlOZIbCygSFK9foORoa6+9Ydi6y1MMNPPM1QYOW85Aj7/OBS3M9a9KdD62rY2OfAmbqn7mKSgNRQGik3CwqV2s/QcNA4ExwNYDy57XdByyc0YIAA=";

        String jsonData = getStsToken(loginToken);
        Log.i("STS", jsonData);
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonData, JsonObject.class);
        JsonObject data = jsonObject.getAsJsonObject("data");

        String accessKeyId = data.get("accessKeyId").getAsString();
        String accessKeySecret = data.get("accessKeySecret").getAsString();
        String securityToken = data.get("securityToken").getAsString();

        System.out.println("Access Key ID: " + accessKeyId);
        System.out.println("Access Key Secret: " + accessKeySecret);
        System.out.println("Security Token: " + securityToken);

        OSSStsTokenCredentialProvider credentialProvider = new OSSStsTokenCredentialProvider(
                accessKeyId, accessKeySecret, securityToken);

        OSS oss = new OSSClient(context, "oss-cn-hangzhou.aliyuncs.com", credentialProvider);
//        OSS oss = new OSSClient(context, "https://oss-cn-hangzhou.aliyuncs.com", credentialProvider);

        // 1. 创建 ObjectMetadata 并设置头信息
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setHeader("Accept", "*/*");
        metadata.setHeader("Accept-Language", "zh-CN,zh;q=0.9");
        metadata.setHeader("Origin", "https://app.zh.igpsport.com");
        metadata.setHeader("Referer", "https://app.zh.igpsport.com/");
        metadata.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36");
        metadata.setHeader("x-oss-user-agent", "aliyun-sdk-js/6.22.0 Chrome 134.0.0.0 on OS X 10.15.7 64-bit");
        Log.i("OSS:", file.getAbsolutePath());
        // 2. 创建 PutObjectRequest 并关联 Metadata
        PutObjectRequest put = new PutObjectRequest("igp-zh", ossName, file.getAbsolutePath());
        put.setMetadata(metadata); // 关键步骤：设置头信息

        Log.i("OSS:", "上传至OSS");
        // 3. 执行上传
        oss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
            @Override
            public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                Log.d("OSS", "上传成功. ETag: " + result.getETag());
            }

            @Override
            public void onFailure(PutObjectRequest request,
                                  ClientException clientEx,
                                  ServiceException serviceEx) {
                String errorInfo = "";
                if (clientEx != null) {
                    errorInfo += "客户端错误: " + clientEx.getMessage();
                }
                if (serviceEx != null) {
                    errorInfo += "服务端错误: " + serviceEx.getErrorCode();
                }
                Log.e("OSS", "上传失败: " + errorInfo);
            }
        });
    }

    // iGPSport Notification
    public void notifyIGPS(String fileName, String ossName) throws IOException {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        String json = "{\"fileName\":\"" + fileName + "\",\"ossName\":\"" + ossName + "\"}";
        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
                .url("https://prod.zh.igpsport.com/service/web-gateway/web-analyze/activity/uploadByOss")
                .post(body)
                .addHeader("Authorization", "Bearer " + token_igp)
                .addHeader("Content-Type", "application/json")
                .addHeader("Referer", "https://app.zh.igpsport.com/")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        }
    }

    // Main upload flow
    public void uploadGPXFiles() {
        try {
            File gpxFile = new File(getAppPath(), current_RideId + ".gpx");
//            File gpxFile = new File(getAppPath(), gpx_name);
            if(! gpxFile.exists()){
                Message msg = handler.obtainMessage();
                msg.obj = "GPX文件还未生成成功";
                handler.sendMessage(msg);
            }
            // 1. Login to iGPSport
            loginIGPS();
            Log.i("OSS", "登录至IGPS成功");

            // 2. Process files
//            File gpxDir = getAppPath();
//            Log.i("OSS", gpxDir.getAbsolutePath());

//            File[] gpxFiles = gpxDir.listFiles();
//            Log.i("OSS", "Files="+gpxFiles.length);

//            for (File gpxFile : gpxDir.listFiles()) {

            String ossName = "1456042-" + UUID.randomUUID().toString();
            Log.i("OSS", gpxFile.getName());
            Log.i("OSS", ossName);

            // 3. Upload to OSS
            upload2oss(token_igp, gpxFile, ossName);
            Log.i("OSS", "上传至OSS成功");
            Message msg = handler.obtainMessage();
            msg.obj = "上传至OSS成功！";
            handler.sendMessage(msg);

            // 4. Notify server
            notifyIGPS(gpxFile.getName(), ossName);
            Log.i("OSS", "上传至IGPS成功");

            msg = handler.obtainMessage();
            msg.obj = "上传至IGPSPORTS成功！";
            handler.sendMessage(msg);
//                /data/user/0/com.uestc.sporttrails/files/SportsTrails/185387836.gpx
//            break;
//          }
        } catch (Exception e) {
            Log.i("OSS", "上传至OSS失败");
            Message msg = handler.obtainMessage();
            msg = handler.obtainMessage();
            msg.obj = "上传至IGPSPORTS失败！";
            handler.sendMessage(msg);
            e.printStackTrace();
        }
    }

}    