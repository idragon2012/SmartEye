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
        StringBuilder sb = new StringBuilder();
        if (all.has("error_code")) {
            sb.append("百度AI接口调用失败:").append(all.getString("error_msg"));
            return sb.toString();
        }

        if (!all.has("result")) {
            sb.append("百度AI接口调用失败:无result");
            return sb.toString();
        }

        JSONArray result = all.getJSONArray("result");

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
                        .append("\n").append("ChatGPT：").append(ChatGPTUtil.chat(detail.getString("keyword")))
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
