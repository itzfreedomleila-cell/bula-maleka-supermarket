package net.codejava.account.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests for password hashing (Module 1). */
class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void correctPassword_matches_wrongPassword_doesNot() {
        String stored = hasher.hash("Bula2026x");
        assertTrue(hasher.matches("Bula2026x", stored));
        assertFalse(hasher.matches("bula2026x", stored));
    }

    @Test
    void samePassword_givesDifferentHashes_becauseOfSalt() {
        assertNotEquals(hasher.hash("Bula2026x"), hasher.hash("Bula2026x"));
    }

    @Test
    void passwordIsNeverStoredAsPlainText() {
        assertFalse(hasher.hash("Bula2026x").contains("Bula2026x"));
    }
}
