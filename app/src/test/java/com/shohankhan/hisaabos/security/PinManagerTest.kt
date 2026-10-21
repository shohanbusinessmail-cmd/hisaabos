package com.shohankhan.hisaabos.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinManagerTest {

    private val salt = PinManager.newSalt()

    @Test
    fun `pin is stored only as a salted verifier`() {
        val hash = PinManager.hashToBase64("1234", salt)
        assertTrue(hash.isNotBlank())
        assertFalse(hash.contains("1234"))
        assertTrue(PinManager.verify("1234", PinManager.saltToBase64(salt), hash, 50_000))
        assertFalse(PinManager.verify("4321", PinManager.saltToBase64(salt), hash, 50_000))
    }

    @Test
    fun `same pin with a different salt produces a different verifier`() {
        val otherSalt = PinManager.newSalt()
        assertNotEquals(
            PinManager.hashToBase64("1234", salt),
            PinManager.hashToBase64("1234", otherSalt),
        )
    }

    @Test
    fun `bangla digits normalise to the same pin`() {
        assertEquals("123456", PinManager.normalize("১২৩৪৫৬"))
        val hash = PinManager.hashToBase64("123456", salt)
        assertTrue(PinManager.verify("১২৩৪৫৬", PinManager.saltToBase64(salt), hash, 50_000))
    }

    @Test
    fun `pin length rules are enforced`() {
        assertFalse(PinManager.isValidPin("123"))
        assertTrue(PinManager.isValidPin("1234"))
        assertTrue(PinManager.isValidPin("12345678"))
        assertFalse(PinManager.isValidPin("123456789"))
        assertFalse(PinManager.isValidPin("12a4"))
    }

    @Test
    fun `malformed stored verifier never matches`() {
        assertFalse(PinManager.verify("1234", "not-base64!", "also-bad", 1_000))
        assertNull(PinManager.saltFromBase64("not-base64!"))
        assertEquals(16, salt.size)
    }
}
