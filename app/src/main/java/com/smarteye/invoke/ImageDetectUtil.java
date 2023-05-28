package com.smarteye.invoke;

import android.graphics.Bitmap;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;

/**
 * 通用物体和场景识别
 */
public class ImageDetectUtil {

    public static final String accessToken = ""; // Baidu AI access token

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public static String advancedGeneral(Bitmap bitmap) {
        String url = "https://aip.baidubce.com/rest/2.0/image-classify/v2/advanced_general?access_token=";
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            String imgStr = Base64Util.encode(byteArrayOutputStream.toByteArray());
            String imgParam = URLEncoder.encode(imgStr, "UTF-8");

            String param = "image=" + imgParam+"&baike_num=9999";
            String result = OkHttpClientUtil.doPostXXX(url + accessToken, param);
            System.out.println(result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
}

