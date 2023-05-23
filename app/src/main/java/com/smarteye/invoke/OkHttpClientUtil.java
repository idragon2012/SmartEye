package com.smarteye.invoke;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * okhttp请求
 *
 * @author
 */
public class OkHttpClientUtil {
    static OkHttpClient client = new OkHttpClient();

    /**
     * post请求xml入参
     *
     * @param url
     * @param data
     * @return
     * @throws IOException
     */
    public static String doPostXml(String url, String data) throws IOException {
        MediaType mediaType = MediaType.parse("application/octet-stream");
        RequestBody body = RequestBody.create(mediaType, data);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    /**
     * post请求json入参
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static String doPostJson(String url, String data) throws IOException {
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, data);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    /**
     * post请求json入参
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static String doPostJsonWithAuth(String url, String data, String authorization) throws IOException {
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, data);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", authorization)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    /**
     * post请求 x-www-form-urlencoded 入参
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static String doPostXXX(String url, String data) throws IOException {
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, data);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    /**
     * get请求
     *
     * @param url
     * @return
     */
    public static String httpGet(String url) {
        Response response = null;
        String result = "";
        Request request = new Request.Builder().url(url).build();
        try {
            response = client.newCall(request).execute();
            result = response.body().string();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}


