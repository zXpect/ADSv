package com.project.ads.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lee los textos legales de res/raw (legal_terms.txt, legal_privacy.txt) para la
 * pantalla nativa de Términos y privacidad. Formato, una línea por bloque:
 * "## Título" sección, "1.1. Texto" cláusula numerada, "- Texto" viñeta y
 * cualquier otra línea, párrafo. Las líneas vacías se ignoran.
 */
public final class LegalTextParser {

    public enum Type { SECTION, CLAUSE, BULLET, PARAGRAPH }

    public static final class Block {
        public final Type type;
        /** Número de la cláusula ("1.1."); vacío en los demás tipos. */
        public final String number;
        public final String text;

        Block(Type type, String number, String text) {
            this.type = type;
            this.number = number;
            this.text = text;
        }
    }

    private static final Pattern CLAUSE = Pattern.compile("^(\\d+(?:\\.\\d+)+\\.)\\s+(.+)$");

    private LegalTextParser() {}

    public static List<Block> parse(String source) {
        List<Block> blocks = new ArrayList<>();
        if (source == null) return blocks;
        for (String raw : source.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("## ")) {
                blocks.add(new Block(Type.SECTION, "", line.substring(3).trim()));
            } else if (line.startsWith("- ")) {
                blocks.add(new Block(Type.BULLET, "", line.substring(2).trim()));
            } else {
                Matcher clause = CLAUSE.matcher(line);
                if (clause.matches()) {
                    blocks.add(new Block(Type.CLAUSE, clause.group(1), clause.group(2).trim()));
                } else {
                    blocks.add(new Block(Type.PARAGRAPH, "", line));
                }
            }
        }
        return blocks;
    }
}
