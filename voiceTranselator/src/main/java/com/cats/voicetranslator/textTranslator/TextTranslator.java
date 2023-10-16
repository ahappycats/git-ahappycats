package com.cats.voicetranslator.textTranslator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class TextTranslator {
    private static final String API_URL = "https://api-free.deepl.com/v2/translate";
    private static final String API_KEY = "";  // 여기에 DeepL API 키를 넣으세요.

    public String translateKoreanToJapanese(String text) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "DeepL-Auth-Key " + API_KEY);
            connection.setRequestProperty("User-Agent", "YourApp/1.2.3");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String requestBody = String.format("{\"text\": [\"%s\"], \"target_lang\": \"JA\"}", text);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBody.getBytes("UTF-8"));
                os.flush();
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line).append('\n');
                }
            }

            JSONObject json = new JSONObject(response.toString());
            JSONArray translations = json.getJSONArray("translations");
            String translatedText = translations.getJSONObject(0).getString("text");

            return "번역된 텍스트: " + translatedText;

        } catch (IOException e) {
            e.printStackTrace();
            return "번역 중 오류 발생";
        }
    }
}
