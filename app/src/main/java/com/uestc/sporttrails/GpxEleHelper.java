package com.uestc.sporttrails;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GpxEleHelper {
    public static int fetch_stride = 50;
    private static final String TAG = "GpxElevationProcessor";

    public static Handler handler;
    public GpxEleHelper(Handler handler) {
        this.handler = handler;
    }
    public void processGpxFile(File gpxFile) {
        new ProcessGpxTask().execute(gpxFile);
    }

    private class ProcessGpxTask extends AsyncTask<File, Void, String> {

        @Override
        protected String doInBackground(File... files) {
            File gpxFile = files[0];
            try {
                long startTime = System.currentTimeMillis(); // 获取开始时间

                // Step 1: Parse GPX file and extract coordinates
                List<LatLng> coordinates = parseGpxFile(gpxFile);

                // Step 2: Fetch elevations using OpenTopoData API
                List<Double> elevations = fetchElevations(coordinates);

                // Step 3: Interpolate elevations for all points
                List<Double> interpolatedElevations = interpolateElevations(elevations, coordinates.size());

                // Step 4: Insert elevations back into the GPX file
                insertElevationsIntoGpx(gpxFile, interpolatedElevations);

                long endTime = System.currentTimeMillis(); // 获取结束时间
                long elapsedTime = (endTime - startTime)/1000; // 计算运行时间（秒）
                Log.d("RunTime", "程序运行时间：" + elapsedTime + " 秒");

                Message msg = handler.obtainMessage();
                msg.obj = "为GPX添加ELE成功@耗时"+elapsedTime+"秒";
                handler.sendMessage(msg);

                return "GPX file processed successfully!";
            } catch (Exception e) {
                Log.e(TAG, "Error processing GPX file", e);
                Message msg = handler.obtainMessage();
                msg.obj = "为GPX添加ELE失败";
                handler.sendMessage(msg);
                return "Error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, result);
        }
    }

    // Step 1: Parse GPX file to extract <trkpt> elements
    private List<LatLng> parseGpxFile(File gpxFile) throws Exception {
        List<LatLng> coordinates = new ArrayList<>();
        InputStream inputStream = new FileInputStream(gpxFile);

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(inputStream, null);

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && "trkpt".equals(parser.getName())) {
                String latStr = parser.getAttributeValue(null, "lat");
                String lonStr = parser.getAttributeValue(null, "lon");
                double lat = Double.parseDouble(latStr);
                double lon = Double.parseDouble(lonStr);
                coordinates.add(new LatLng(lat, lon));
            }
            eventType = parser.next();
        }

        inputStream.close();
        return coordinates;
    }
//
//    // Step 2: Fetch elevations using OpenTopoData API
//    private List<Double> fetchElevations(List<LatLng> coordinates) throws Exception {
//        List<Double> elevations = new ArrayList<>();
//        StringBuilder locations = new StringBuilder();
//        Log.i("Coordinates:", String.valueOf(coordinates.size()));
//        int stride = coordinates.size()/96;
//        for (int i = 0; i < coordinates.size(); ) {
//            LatLng point = coordinates.get(i);
//            locations.append(point.lat).append(",").append(point.lon);
//            i+=stride;
//            if (i < coordinates.size() - 1) {
//                locations.append("|");
//            }
//        }
////        Log.i("ELE-CNT", String.valueOf(cnt));
////        Log.i("Coordinates:", locations.toString());
//        String urlString = "https://api.opentopodata.org/v1/aster30m?locations=" + locations.toString();
//        URL url = new URL(urlString);
//        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//        connection.setRequestMethod("GET");
//
//
//// 读取完整的 HTTP 响应
//        InputStream responseStream = connection.getInputStream();
//        StringBuilder jsonResponse = new StringBuilder();
//        BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));
//        String line;
//        while ((line = reader.readLine()) != null) {
//            jsonResponse.append(line);
//        }
//        reader.close();
//        responseStream.close();
////        Log.i("Res", jsonResponse.toString());
//        // Parse JSON response (use a library like Gson or JSONObject)
//        Gson gson = new Gson();
//        JsonObject jsonObject = gson.fromJson(jsonResponse.toString(), JsonObject.class);
//        System.out.println(jsonObject);
//
//        JsonArray jsonArray = jsonObject.getAsJsonArray("results");
//        for (int i = 0; i < jsonArray.size(); i++) {
//            JsonObject element = jsonArray.get(i).getAsJsonObject();
////            System.out.println(element.toString());
//            elevations.add(element.get("elevation").getAsDouble());
//        }
//
//        return elevations;
//    }


    private static OkHttpClient client;
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

    private void parseElevations(String jsonResponse, List<Double> elevations) {
        Gson gson = new Gson();
        try {
            JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);
            JsonArray results = jsonObject.getAsJsonArray("results");

            for (JsonElement element : results) {
                JsonObject result = element.getAsJsonObject();
                double elevation = result.get("elevation").getAsDouble();
                elevations.add(elevation);
            }
        } catch (Exception e) {
            Log.e("ElevationParser", "Error parsing elevations: " + e.getMessage());
        }
    }

    // Step 2: Fetch elevations using OpenTopoData API
    private List<Double> fetchElevations(List<LatLng> coordinates) throws Exception {
        List<Double> elevations = new ArrayList<>();
        StringBuilder locations = new StringBuilder();
        Log.i("Coordinates:", String.valueOf(coordinates.size()));
        int query_cnt=0;
        for (int i = 0; i < coordinates.size();) {
            LatLng point = coordinates.get(i);
            locations.append(point.lat).append(",").append(point.lon);
            i+=fetch_stride;
            if (i < coordinates.size() - 1) {
                locations.append("|");
            }else{
                break;
            }

            if (locations.length()>99 || i>coordinates.size() - 1){
                int ratio_ready = i*100/coordinates.size();
                if(query_cnt%5==0){
                    Message msg = handler.obtainMessage();
                    msg.obj = "\r>"+ratio_ready+"%";
                    handler.sendMessage(msg);
                }

                query_cnt++;
                Log.i("ELE-request", "Query#"+query_cnt+"$locations="+locations.length()+"@trkpt="+i+"/"+coordinates.size());
//        Log.i("Coordinates:", locations.toString());
                String urlString = "https://api.opentopodata.org/v1/aster30m?locations=" + locations.toString();
//                locations = new StringBuilder();//java.lang.OutOfMemoryError: Failed to allocate a 32 byte allocation with 109736 free bytes and 107KB until OOM
                locations.setLength(0);
                Request request = new Request.Builder()
                        .url(urlString)
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .build();

//                String str_rtn=null;
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
//                Log.i("XOSS", response.toString());
//                        str_rtn = response.body().string();
                        String jsonResponse = response.body().string();
                        parseElevations(jsonResponse, elevations);
                    }
                }
            }
        }

        return elevations;
    }

    // Step 3: Interpolate elevations for all points
    private List<Double> interpolateElevations(List<Double> elevations, int totalPoints) {
//        Log.i("interpolateElevations", String.valueOf(elevations.size())+","+String.valueOf(totalPoints));
        List<Double> interpolatedElevations = new ArrayList<>();
        double[] originalIndices = new double[elevations.size()];
        for (int i = 0; i < originalIndices.length; i++) {
            originalIndices[i] = i * ((double) totalPoints / elevations.size());
        }
//        Log.e("Interpolation", String.valueOf(totalPoints)+"@"+elevations.size()+"@"+originalIndices.length);
        for (int i = 0; i < totalPoints; i++) {
            double x = i;
            int j = 0;
            // 找到目标索引所在的区间
            while (j < originalIndices.length - 1 && originalIndices[j + 1] < x) {
                j++;
            }
//            Log.e("Interpolation", String.valueOf(totalPoints)+"@"+j);
            j = Math.min(j, originalIndices.length - 2);
            // 获取区间两端的点
            double x0 = originalIndices[j];
            double x1 = originalIndices[j + 1];
            double y0 = elevations.get(j);
            double y1 = elevations.get(j + 1);

            // 线性插值公式：y = y0 + (y1 - y0) * ((x - x0) / (x1 - x0))
            double y = y0 + (y1 - y0) * ((x - x0) / (x1 - x0));
            interpolatedElevations.add(y);
        }

        return interpolatedElevations;
    }

    // Step 4: Insert elevations back into the GPX file
    private void insertElevationsIntoGpx(File gpxFile, List<Double> elevations) throws Exception {
        StringBuilder gpxContent = new StringBuilder();
        InputStream inputStream = new FileInputStream(gpxFile);
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        inputStream.close();

        String originalContent = new String(buffer);
        String[] lines = originalContent.split("\n");

        int elevationIndex = 0;
        for (String line : lines) {
            gpxContent.append(line).append("\n");
            if (line.trim().startsWith("<trkpt")) {
                if (elevationIndex < elevations.size()) {
                    double elevation = elevations.get(elevationIndex);
                    gpxContent.append("    <ele>").append(elevation).append("</ele>\n");
                    elevationIndex++;
                }
            }
        }

        File gpxSave = new File(TrailsHelper.getAppPath(), TrailsHelper.current_RideId+".gpx");
        FileWriter writer = new FileWriter(gpxSave);
        writer.write(gpxContent.toString());
        writer.close();
    }

    // Helper class to store latitude and longitude
    private static class LatLng {
        double lat, lon;

        LatLng(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }
}
