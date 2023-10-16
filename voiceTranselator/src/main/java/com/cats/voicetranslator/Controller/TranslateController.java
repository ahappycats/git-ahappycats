package com.cats.voicetranslator.Controller;

import com.cats.voicetranslator.textTranslator.TextTranslator;
import com.cats.voicetranslator.voiceVox.VoiceVoxModel;
import com.cats.voicetranslator.voicetotext.VoiceToText;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class TranslateController {
    private VoiceToText voiceToText = new VoiceToText();
    private TextTranslator textTranslator = new TextTranslator();
    private VoiceVoxModel voiceVoxModel = new VoiceVoxModel();


    @PostMapping("/translate_and_speak")
    public TranslationResult translateAndSpeak(@RequestParam("audio") MultipartFile audioFile) throws IOException {
        // 1. Convert audio file to text
        byte[] audioData = audioFile.getBytes();
        String originalText = voiceToText.processExternalAudio(audioData);

        // 2. Translate the text
        String translatedText = textTranslator.translateKoreanToJapanese(originalText);

        // 3. Convert the translated text to speech and save the audio file
        byte[] translatedAudioData = voiceVoxModel.textToSpeech(translatedText);
        String audioPath = "D:\\voice\\output.wav";
        Path path = Paths.get(audioPath);
        Files.write(path, translatedAudioData);

        // 4. Return the translated text and the path to the audio file
        return new TranslationResult(translatedText, audioPath);
    }

    class TranslationRequest {
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static class TranslationResult {
        private String translatedText;
        private String audioPath;

        public TranslationResult(String translatedText, String audioPath) {
            this.translatedText = translatedText;
            this.audioPath = audioPath;
        }

        public String getTranslatedText() {
            return translatedText;
        }

        public void setTranslatedText(String translatedText) {
            this.translatedText = translatedText;
        }

        public String getAudioPath() {
            return audioPath;
        }

        public void setAudioPath(String audioPath) {
            this.audioPath = audioPath;
        }
    }
}

