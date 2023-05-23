package com.smarteye.invoke;

import android.os.Build;

import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONObject;

public class ChatGPTUtil {

    public static final String authorization = "";

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public static String chat(String content) {
        String url = "https://api.openai-proxy.com/v1/chat/completions";
        try {
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", content);

            JSONArray messages = new JSONArray();
            messages.put(message);

            JSONObject request = new JSONObject();
            request.put("model", "gpt-3.5-turbo");
            request.put("messages", messages);

            String result = OkHttpClientUtil.doPostJsonWithAuth(url, request.toString(), authorization);

            System.out.println(result);
            JSONObject jsonObject = new JSONObject(result);
            JSONArray choices = jsonObject.getJSONArray("choices");
            JSONObject choice =choices.getJSONObject(0);
            return choice.getJSONObject("message").getString("content");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
