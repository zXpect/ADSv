package com.project.ads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.project.ads.helpers.LegalTextParser;
import com.project.ads.helpers.LegalTextParser.Block;
import com.project.ads.helpers.LegalTextParser.Type;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/** Formato de los textos legales de res/raw y los dos archivos reales. */
public class LegalTextParserTest {

    @Test
    public void readsEachKindOfBlock() {
        List<Block> blocks = LegalTextParser.parse(
                "Intro del documento.\n\n## 1. Naturaleza\n1.1. ADS es un prototipo.\n10.2. Otra cláusula.\n- Una viñeta\r\n");
        assertEquals(5, blocks.size());
        assertEquals(Type.PARAGRAPH, blocks.get(0).type);
        assertEquals(Type.SECTION, blocks.get(1).type);
        assertEquals("1. Naturaleza", blocks.get(1).text);
        assertEquals(Type.CLAUSE, blocks.get(2).type);
        assertEquals("1.1.", blocks.get(2).number);
        assertEquals("ADS es un prototipo.", blocks.get(2).text);
        assertEquals("10.2.", blocks.get(3).number);
        assertEquals(Type.BULLET, blocks.get(4).type);
        assertEquals("Una viñeta", blocks.get(4).text);
    }

    @Test
    public void numbersInsideAParagraphAreNotClauses() {
        // «Ley 1581 de 2012.» o una cifra al inicio sin punto final no son cláusulas
        assertEquals(Type.PARAGRAPH, LegalTextParser.parse("2012. Algo").get(0).type);
        assertEquals(Type.PARAGRAPH, LegalTextParser.parse("1.1 sin punto final").get(0).type);
    }

    @Test
    public void realTermsHaveTwentySections() throws IOException {
        List<Block> blocks = LegalTextParser.parse(raw("legal_terms.txt"));
        long sections = blocks.stream().filter(b -> b.type == Type.SECTION).count();
        assertEquals(20, sections);
        assertTrue(blocks.stream().anyMatch(b -> "20.3.".equals(b.number)));
    }

    @Test
    public void realPrivacyNoticeHasItsSections() throws IOException {
        List<Block> blocks = LegalTextParser.parse(raw("legal_privacy.txt"));
        assertTrue(blocks.stream().anyMatch(b -> b.type == Type.SECTION && b.text.equals("Derechos del Titular")));
        assertEquals(12, blocks.stream().filter(b -> b.type == Type.BULLET).count());
        // Sin referencias a los anexos del documento de grado
        assertTrue(blocks.stream().noneMatch(b -> b.text.contains("Anexo")));
    }

    private static String raw(String name) throws IOException {
        return new String(Files.readAllBytes(Paths.get("src/main/res/raw", name)), StandardCharsets.UTF_8);
    }
}
