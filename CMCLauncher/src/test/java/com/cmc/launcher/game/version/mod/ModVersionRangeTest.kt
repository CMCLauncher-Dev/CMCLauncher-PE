package com.cmc.launcher.game.version.mod

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModVersionRangeTest {

    @Test
    fun acceptsPlainRanges() {
        assertTrue(ModVersionRange.matches("1.20.1", ">=1.20 <1.21"))
        assertFalse(ModVersionRange.matches("1.21", ">=1.20 <1.21"))
        assertTrue(ModVersionRange.matches("2.5.0", "[2.0,3.0)"))
        assertFalse(ModVersionRange.matches("3.0", "[2.0,3.0)"))
    }

    @Test
    fun acceptsCaretAndTilde() {
        assertTrue(ModVersionRange.matches("1.4.2", "^1.2.0"))
        assertFalse(ModVersionRange.matches("2.0.0", "^1.2.0"))
        assertTrue(ModVersionRange.matches("1.2.9", "~1.2.3"))
        assertFalse(ModVersionRange.matches("1.3.0", "~1.2.3"))
    }

    @Test
    fun acceptsAlternatives() {
        assertTrue(ModVersionRange.matches("1.19.2", "1.18.x || >=1.19 <1.20"))
        assertFalse(ModVersionRange.matches("1.20.0", "1.18.x || >=1.21"))
    }
}