package com.cats.voicetranslator.voicetotext;

import com.cats.voicetranslator.textTranslator.TextTranslator;
import com.cats.voicetranslator.voiceVox.VoiceVoxModel;
import org.json.JSONObject;

import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class VoiceToText {
    private static final String API_URL = "https://api.openai.com/v1/audio/transcriptions";
    private static final String API_KEY = "";  // Please reset this key and keep it private.
    private static volatile boolean isRecording = false;
    private static ByteArrayOutputStream out;

    private TextTranslator translator = new TextTranslator();

    public void speechToText() {
        Frame frame = new Frame();
        frame.setSize(400, 400);
        frame.setVisible(true);

        frame.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && !isRecording) {
                    // Run startRecording in a new thread
                    new Thread(VoiceToText::startRecording).start();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && isRecording) {
                    stopRecordingAndSend();
                }
            }
        });
    }

    private static void startRecording() {
        System.out.println("start recording");
        isRecording = true;
        out = new ByteArrayOutputStream();
        AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        try {
            TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            byte[] buffer = new byte[4096];

            while (isRecording) {
                if (microphone.available() > 0) { // Check if data is available
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    out.write(buffer, 0, bytesRead);
                }
            }

            microphone.close();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public String processExternalAudio(byte[] audioData) {
        try {
            // 1. Convert audio data to wav format if necessary
            byte[] wavAudioBytes = convertToWav(audioData);

            // 2. Send to OpenAI and get the transcription
            String responseFromOpenAI = sendToOpenAI(wavAudioBytes);

            // 3. Parse response and optionally perform further actions
            JSONObject jsonResponse = new JSONObject(responseFromOpenAI);
            String extractedText = jsonResponse.getString("transcription");  // Please modify according to the actual response structure!

            // 4. Return the transcribed text or some result
            return extractedText;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error processing audio";
        }
    }

    private byte[] convertToWav(byte[] audioBytes) {
        try {
            InputStream byteInputStream = new ByteArrayInputStream(audioBytes);
            AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
            AudioInputStream stream = new AudioInputStream(byteInputStream, format, audioBytes.length);

            ByteArrayOutputStream wavOut = new ByteArrayOutputStream();
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, wavOut);

            return wavOut.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void stopRecordingAndSend() {
        System.out.println("stop recording");
        isRecording = false;
        byte[] rawAudioBytes = out.toByteArray();
        byte[] wavAudioBytes = convertToWav(rawAudioBytes);  // Convert raw audio bytes to WAV

        // OpenAI에서 받은 응답
        String responseFromOpenAI = sendToOpenAI(wavAudioBytes);
        System.out.println("Received from OpenAI: " + responseFromOpenAI);

        // OpenAI 응답을 파싱하여 텍스트를 추출합니다. (이 부분은 OpenAI 응답 형식에 따라 달라질 수 있습니다.)
        JSONObject jsonResponse = new JSONObject(responseFromOpenAI);
        String extractedText = jsonResponse.getString("text");  // 이것은 실제 JSON 키를 기반으로 수정해야 합니다.

        // 추출된 텍스트를 일본어로 번역
        String translatedText = translator.translateKoreanToJapanese(extractedText);
        System.out.println("Translated to Japanese: " + translatedText);

        // 일본어 텍스트를 음성으로 변환
        VoiceVoxModel voiceVoxModel = new VoiceVoxModel();
        byte[] audioData = voiceVoxModel.textToSpeech(translatedText);

        // WAV 파일로 저장
        try (FileOutputStream fos = new FileOutputStream("D:\\voice\\output.wav")) {
            fos.write(audioData);
        }
        catch (IOException e) {
            e.printStackTrace();
        }


    }


    private String sendToOpenAI(byte[] audioBytes) {
        String boundary = Long.toHexString(System.currentTimeMillis());
        String LINE_FEED = "\r\n";

        try {
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true);

                // Model parameter
                writer.append("--" + boundary).append(LINE_FEED);
                writer.append("Content-Disposition: form-data; name=\"model\"").append(LINE_FEED);
                writer.append("Content-Type: text/plain; charset=UTF-8").append(LINE_FEED);
                writer.append(LINE_FEED);
                writer.append("whisper-1").append(LINE_FEED);
                writer.flush();

                // Audio file
                writer.append("--" + boundary).append(LINE_FEED);
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"").append(LINE_FEED);
                writer.append("Content-Type: audio/wav").append(LINE_FEED);
                writer.append(LINE_FEED);
                writer.flush();

                os.write(audioBytes);
                os.flush();

                writer.append(LINE_FEED);
                writer.flush();

                // End boundary
                writer.append("--" + boundary + "--").append(LINE_FEED);
                writer.close();
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line).append('\n');
                }
            }

            return response.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "Error while sending data to OpenAI";
        }
    }

    public static void main(String[] args) {
        VoiceToText voiceToText = new VoiceToText();
        voiceToText.speechToText();
    }
}

