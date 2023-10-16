package com.cats.voicetranslator.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/translator")
    public String translator() {
        return "Translator";
    }
}
