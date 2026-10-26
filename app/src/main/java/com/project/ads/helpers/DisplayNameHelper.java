package com.project.ads.helpers;

/**
 * Formato de nombres de personas para mostrarlos en pantalla («breider lopez» →
 * «Breider Lopez»). Solo cambia lo que se ve; el dato guardado en Firebase no se toca.
 */
public final class DisplayNameHelper {

    private DisplayNameHelper() {}

    /** Mayúscula inicial en cada palabra y espacios simples; el resto de letras se respeta. */
    public static String format(String name) {
        if (name == null) return null;
        String[] words = name.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.appendCodePoint(Character.toUpperCase(word.codePointAt(0)));
            out.append(word, Character.charCount(word.codePointAt(0)), word.length());
        }
        return out.toString();
    }
}
