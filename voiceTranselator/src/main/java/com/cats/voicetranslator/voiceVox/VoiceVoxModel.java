package com.cats.voicetranslator.voiceVox;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class VoiceVoxModel {
    private static final String VOICEVOX_API_URL = "http://localhost:50021";

    public byte[] textToSpeech(String text) {
        try {
            // 음성 합성용 쿼리 생성
            String queryJson = generateAudioQuery(text);
            if (queryJson == null) {
                return null;
            }

            // 실제 음성 합성 실행
            return synthesizeAudio(queryJson);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String generateAudioQuery(String text) throws IOException {
        URL url = new URL(VOICEVOX_API_URL + "/audio_query?text=" + URLEncoder.encode(text, "UTF-8") + "&speaker=2");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.flush();
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private byte[] synthesizeAudio(String queryJson) throws IOException {
        URL url = new URL(VOICEVOX_API_URL + "/synthesis?speaker=2");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(queryJson.getBytes("UTF-8"));
            os.flush();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream is = connection.getInputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }

        return output.toByteArray();
    }
}
