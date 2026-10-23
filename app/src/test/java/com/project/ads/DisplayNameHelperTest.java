package com.project.ads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.project.ads.helpers.DisplayNameHelper;

import org.junit.Test;

public class DisplayNameHelperTest {

    @Test
    public void capitalizesEachWord() {
        assertEquals("Breider Lopez", DisplayNameHelper.format("breider Lopez"));
        assertEquals("Ángela Núñez", DisplayNameHelper.format("ángela núñez"));
    }

    @Test
    public void keepsTheRestOfTheLetters() {
        assertEquals("McDonald LÓPEZ", DisplayNameHelper.format("mcDonald LÓPEZ"));
    }

    @Test
    public void collapsesSpaces() {
        assertEquals("Samuel Ruiz", DisplayNameHelper.format("  samuel   ruiz "));
    }

    @Test
    public void handlesEmptyAndNull() {
        assertEquals("", DisplayNameHelper.format("   "));
        assertNull(DisplayNameHelper.format(null));
    }
}
