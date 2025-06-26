package com.ubp.clasificador.util;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;

public class TextPreprocessor {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "a", "al", "ante", "con", "contra", "de", "del", "desde", "durante", "en",
        "entre", "hacia", "hasta", "mediante", "para", "por", "según", "sin", "sobre", "tras",
        "el", "la", "los", "las", "un", "una", "unos", "unas", "y", "o", "u", "pero",
        "más", "menos", "este", "esta", "estos", "estas", "ese", "esa", "esos", "esas",
        "mi", "tu", "su", "nuestro", "vuestro", "sus", "mis", "tus", "lo", "se", "me", "te",
        "que", "quien", "quienes", "cual", "cuales", "como", "cuando", "donde", "mientras",
        "si", "no", "bien", "mal", "muy", "tan", "tanto", "así", "además", "entonces", "luego",
        "ya", "aun", "solo", "también", "tampoco", "quizás", "tal vez", "casi", "siempre", "nunca",
        "jamás", "poco", "mucho", "todo", "nada", "nadie", "algo", "alguien", "uno", "otro",
        "misma", "mismo", "mismas", "mismos", "cada", "cierto", "cierta", "ciertos", "ciertas",
        "propio", "propia", "propios", "propias", "varios", "varias", "tal", "tales"
    ));

    public static List<String> preprocess(String text) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }

        String cleanedText = text.toLowerCase().replaceAll("[^a-zñáéíóúü\\s]", "");

        List<String> tokens = Arrays.asList(cleanedText.split("\\s+"));

        return tokens.stream()
                     .filter(token -> !token.isEmpty() && !STOP_WORDS.contains(token))
                     .collect(Collectors.toList());
    }
}