package info.xiequan.androidbootstraps.util.network;


import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import info.xiequan.androidbootstraps.exception.MyHttpException;
import info.xiequan.androidbootstraps.util.AppConfig;


/**
 * Created by spark on 5/7/14.
 */
public class HttpUtil {
    /**
     * 判断是否有网络
     *
     * @param context
     * @return boolean 是否存在网络
     */
    public static boolean haveConnection(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return false;
        } else if (!networkInfo.isAvailable()) {
            return false;
        }
        return true;
    }

    /**
     * 判断网络类型
     *
     * @param context
     * @return String wifi or mobile
     */
    public static String connectionType(Activity context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        String result = "";
        int type = networkInfo.getType();
        if (type == ConnectivityManager.TYPE_WIFI) {
            result = "wifi";
        } else if (type == ConnectivityManager.TYPE_MOBILE) {
            result = "mobile";
        }
        return result;
    }

    /**
     * 通过HTTP协议post文件,参数
     *
     * @param actionUrl
     *            上传URL地址
     * @param params
     *            Map<String,String>参数, 没有参数的话传null
     * @param files
     *            Map<String,File>参数,没有参数的话传null
     * @return String 返回页面的内容
     * @throws java.io.IOException
     */
    public static String post(String actionUrl, Map<String, String> params, Map<String, File> files) throws IOException {

        String BOUNDARY = java.util.UUID.randomUUID().toString();
        String PREFIX = "--", LINEND = "\r\n";
        String MULTIPART_FROM_DATA = "multipart/form-data";
        String CHARSET = "UTF-8";

        URL uri = new URL(actionUrl);
        HttpURLConnection conn = (HttpURLConnection) uri.openConnection();
        conn.setConnectTimeout(5 * 1000);
        conn.setDoInput(true);// 允许输入
        conn.setDoOutput(true);// 允许输出
        conn.setUseCaches(false); // 不允许使用缓存
        conn.setRequestMethod("POST");
        conn.setRequestProperty("connection", "keep-alive");
        conn.setRequestProperty("Charsert", "UTF-8");
        conn.setRequestProperty("Content-Type", MULTIPART_FROM_DATA + ";boundary=" + BOUNDARY);
        // 首先组拼文本类型的参数
        StringBuilder sb = new StringBuilder();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                sb.append(PREFIX);
                sb.append(BOUNDARY);
                sb.append(LINEND);
                sb.append("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"" + LINEND);
                sb.append("Content-Type: text/plain; charset=" + CHARSET + LINEND);
                sb.append("Content-Transfer-Encoding: 8bit" + LINEND);
                sb.append(LINEND);
                sb.append(entry.getValue());
                sb.append(LINEND);
            }
        }
        DataOutputStream outStream = new DataOutputStream(conn.getOutputStream());
        outStream.write(sb.toString().getBytes("UTF-8"));

        // 发送文件数据
        if (files != null) {
            for (Map.Entry<String, File> file : files.entrySet()) {
                StringBuilder sb1 = new StringBuilder();
                sb1.append(PREFIX);
                sb1.append(BOUNDARY);
                sb1.append(LINEND);
                sb1.append("Content-Disposition: form-data; name=\"" + file.getKey() + "\"; filename=\"" + file.getValue().getPath() + "\"" + LINEND);
                sb1.append("Content-Type: application/octet-stream; charset=" + CHARSET + LINEND);
                sb1.append(LINEND);
                outStream.write(sb1.toString().getBytes("UTF-8"));
                InputStream is = new FileInputStream(file.getValue());
                byte[] buffer = new byte[1024 * 50];
                int len = 0;
                while ((len = is.read(buffer)) != -1) {
                    outStream.write(buffer, 0, len);
                }

                is.close();
                outStream.write(LINEND.getBytes());
            }
        }

        // 请求结束标志
        byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINEND).getBytes();
        outStream.write(end_data);
        outStream.flush();

        // 得到响应码
        int res = conn.getResponseCode();
        StringBuilder sb2 = new StringBuilder();
        InputStream in = null;
        if (res == 200) {
            in = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String ch = null;
            while ((ch = br.readLine()) != null) {
                sb2.append(ch);
            }
        }
        return in == null ? "" : sb2.toString();
    }

    public static String post(String path, File file) {

        Log.d("path", path);
        HttpURLConnection con;
        String boundary = java.util.UUID.randomUUID().toString();
        try {
            URL url = new URL(path);

            con = (HttpURLConnection) url.openConnection();

            con.setConnectTimeout(5000);

			/* 允许Input、Output，不使用Cache */

            con.setDoInput(true);

            con.setDoOutput(true);

            con.setUseCaches(false);

			/* 设置传送的method=POST */

            con.setRequestMethod("POST");

			/* setRequestProperty */

            con.setRequestProperty("Connection", "Keep-Alive");

            con.setRequestProperty("Charset", "UTF-8");

            con.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

			/* 设置DataOutputStream */

            DataOutputStream ds = new DataOutputStream(con.getOutputStream());

            FileInputStream fStream = new FileInputStream(file);

			/* 设置每次写入1024bytes */

            int bufferSize = 1024;

            byte[] buffer = new byte[bufferSize];

            int length = -1;

			/* 从文件读取数据至缓冲区 */

            while ((length = fStream.read(buffer)) != -1)

            {

				/* 将资料写入DataOutputStream中 */

                ds.write(buffer, 0, length);

            }

            fStream.close();

            ds.flush();

            ds.close();

            int res = con.getResponseCode();
            StringBuilder sb2 = new StringBuilder();
            InputStream in = null;
            if (res == 200) {
                in = con.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                String ch = null;
                while ((ch = br.readLine()) != null) {
                    sb2.append(ch);
                }
            }
            Log.d("content", sb2.toString());
            return in == null ? "" : sb2.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static String post(String url, Map<String, String> map) throws MyHttpException {
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            if (map != null) {
                for (Entry<String, String> set : map.entrySet()) {
                    params.add(new BasicNameValuePair(set.getKey(), set.getValue()));
                }
            }
            HttpClient httpClient = new DefaultHttpClient();
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000);
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 5000);
            HttpPost httpPost = new HttpPost(new URI(url));
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            HttpResponse httpResponse = httpClient.execute(httpPost);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                String content = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
                return content.replaceAll("&nbsp;", "");
            }
        } catch (URISyntaxException ex) {
            Log.e("URISyntaxException", ex.getMessage());
        } catch (ClientProtocolException ex) {
            Log.e("ClientProtocolException", ex.getMessage());
        } catch (IOException ex) {
            // Log.e("IOException", ex.getMessage());
            throw new MyHttpException("500", "网络异常请稍后再试");
        }
        return "";
    }

    public static String postJSON(String url, Map<String, String> map) throws MyHttpException {

        try {
            JSONObject obj = new JSONObject();
            if (map != null) {
                for (Entry<String, String> set : map.entrySet()) {
                    try {
                        obj.put(set.getKey(), set.getValue());
                        // if (BaseApplication.IS_DEBUG)
                        // Log.d("param :" + set.getKey(), set.getValue());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            HttpClient httpClient = new DefaultHttpClient();
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000);
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 5000);
            HttpPost httpPost = new HttpPost(new URI(url));
            httpPost.setEntity(new StringEntity(obj.toString(), "UTF-8"));
            httpPost.addHeader("Content-Type", "application/json;charset=UTF-8");
            httpPost.addHeader("User-Agent", "imgfornote");
            HttpResponse httpResponse = httpClient.execute(httpPost);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                String content = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
                if (AppConfig.IS_DEBUG)
                    Log.d("json", content);
                return content.replaceAll("&nbsp;", "");
            } else {
                if (AppConfig.IS_DEBUG) {
                    String content = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
                    Log.d("error", content);
                }
            }
        } catch (URISyntaxException ex) {
            Log.e("URISyntaxException", ex.getMessage());
        } catch (ClientProtocolException ex) {
            Log.e("ClientProtocolException", ex.getMessage());
        } catch (IOException ex) {
            // Log.e("IOException", ex.getMessage());
            throw new MyHttpException("500", "网络异常请稍后再试");
        }
        return "";
    }

    public static String postJSONGoods(String url, Map<String, ?> map) throws MyHttpException {

        try {
            JSONObject json = new JSONObject();
            if (map != null) {
                for (Entry<String, ?> set : map.entrySet()) {
                    try {
                        if(set.getKey()=="goods"){
                            JSONArray array = new JSONArray(String.valueOf(set.getValue()));
                            json.put(set.getKey(), array);
                        }else if(set.getKey()=="invoice"){
                            JSONObject object = new JSONObject(String.valueOf(set.getValue()));
                            json.put(set.getKey(), object);
                        }else{
                            json.put(set.getKey(), set.getValue());
                        }
                        // if (BaseApplication.IS_DEBUG)
                        // Log.d("param :" + set.getKey(), set.getValue());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            HttpClient httpClient = new DefaultHttpClient();
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000);
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 5000);
            HttpPost httpPost = new HttpPost(new URI(url));
            httpPost.setEntity(new StringEntity(json.toString(), "UTF-8"));
            httpPost.addHeader("Content-Type", "application/json;charset=UTF-8");
            httpPost.addHeader("User-Agent", "imgfornote");
            HttpResponse httpResponse = httpClient.execute(httpPost);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                String content = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
                if (AppConfig.IS_DEBUG)
                    Log.d("json", content);
                return content.replaceAll("&nbsp;", "");
            } else {
                if (AppConfig.IS_DEBUG) {
                    String content = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
                    Log.d("error", content);
                }
            }
        } catch (URISyntaxException ex) {
            Log.e("URISyntaxException", ex.getMessage());
        } catch (ClientProtocolException ex) {
            Log.e("ClientProtocolException", ex.getMessage());
        } catch (IOException ex) {
            // Log.e("IOException", ex.getMessage());
            throw new MyHttpException("500", "网络异常请稍后再试");
        }
        return "";
    }

    public static String get(String url, Map<String, String> map) throws MyHttpException {
        try {
            url += "?";
            if (map != null) {
                for (Entry<String, String> set : map.entrySet()) {
                    url += set.getKey() + "=" + set.getValue() + "&";
                }
            }
            HttpClient httpClient = new DefaultHttpClient();
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 20000);
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 20000);
            HttpGet httpGet = new HttpGet(new URI(url));
            HttpResponse httpResponse = httpClient.execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                String content = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
                if (AppConfig.IS_DEBUG)
                    Log.d("json", content);
                return content.replaceAll("&nbsp;", "");
            }
        } catch (URISyntaxException ex) {
            Log.e("URISyntaxException", ex.getMessage());
        } catch (ClientProtocolException ex) {
            Log.e("ClientProtocolException", ex.getMessage());
        } catch (IOException ex) {
            // Log.e("IOException", ex.getMessage());
            throw new MyHttpException("500", "网络异常请稍后再试");
        }
        return "";
    }

    public static String put(String url, Map<String, String> map) throws MyHttpException {
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            if (map != null) {
                for (Entry<String, String> set : map.entrySet()) {
                    params.add(new BasicNameValuePair(set.getKey(), set.getValue()));
                }
            }
            HttpClient httpClient = new DefaultHttpClient();
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000);
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 5000);
            HttpPut httpPut = new HttpPut(new URI(url));
            httpPut.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            HttpResponse httpResponse = httpClient.execute(httpPut);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                String content = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
                return content.replaceAll("&nbsp;", "");
            }
        } catch (URISyntaxException ex) {
            Log.e("URISyntaxException", ex.getMessage());
        } catch (ClientProtocolException ex) {
            Log.e("ClientProtocolException", ex.getMessage());
        } catch (IOException ex) {
            // Log.e("IOException", ex.getMessage());
            throw new MyHttpException("500", "网络异常请稍后再试");
        }
        return "";
    }

    public static String delete(String url, Map<String, String> map) throws MyHttpException {
        try {
            url += "?";
            if (map != null) {
                for (Entry<String, String> set : map.entrySet()) {
                    url += set.getKey() + "=" + set.getValue() + "&";
                }
            }
            HttpClient httpClient = new DefaultHttpClient();
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000);
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 5000);
            HttpDelete httpDelete = new HttpDelete(new URI(url));
            HttpResponse httpResponse = httpClient.execute(httpDelete);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                String content = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
                return content.replaceAll("&nbsp;", "");
            }
        } catch (URISyntaxException ex) {
            Log.e("URISyntaxException", ex.getMessage());
        } catch (ClientProtocolException ex) {
            Log.e("ClientProtocolException", ex.getMessage());
        } catch (IOException ex) {
            // Log.e("IOException", ex.getMessage());
            throw new MyHttpException("500", "网络异常请稍后再试");
        }
        return "";
    }
}
