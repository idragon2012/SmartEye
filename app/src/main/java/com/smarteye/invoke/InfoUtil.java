package com.smarteye.invoke;

import android.os.Build;

import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class InfoUtil {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public static String transInfo(String baiduInfo) throws JSONException {
        JSONObject all = new JSONObject(baiduInfo);
        JSONArray result = all.getJSONArray("result");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < result.length(); i++) {
            JSONObject detail = result.getJSONObject(i);
            if (detail.getDouble("score") > 0.5) {
                JSONObject baikeInfo = null;
                try {
                    baikeInfo = detail.getJSONObject("baike_info");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                sb.append(detail.getString("keyword"))
                        .append(":")
                        .append(detail.getString("score"))
                        .append("\n")
                        .append("ChatGPT：" + ChatGPTUtil.chat(detail.getString("keyword")))
                        .append("\n")
                        .append(baikeInfo == null ? "" : "百科信息：" + baikeInfo.optString("description"))
                        .append("\n")
                        .append(baikeInfo == null ? "" : "百科链接：" + baikeInfo.optString("baike_url"))
                        .append("\n")
                ;
            }
        }
        if (sb.length() < 1) {
            sb.append("暂无有效识别");
        }
        return sb.toString();
    }
}
