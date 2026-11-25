package com.campusride;

import com.campusride.util.PasswordHasher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordHasher Security and PBKDF2 Verification Tests")
public class PasswordHasherTest {

    @Test
    @DisplayName("Password hashing produces valid iterations:salt:hash format")
    public void testHashFormat() {
        String password = "SecureStudentPass#2026";
        String hash = PasswordHasher.hashPassword(password);

        assertNotNull(hash);
        String[] parts = hash.split(":");
        assertEquals(3, parts.length);
        assertEquals("1000", parts[0]);
        assertEquals(32, parts[1].length()); // 16 bytes = 32 hex chars
        assertEquals(64, parts[2].length()); // 256 bits = 32 bytes = 64 hex chars
    }

    @Test
    @DisplayName("Verify matching password returns true")
    public void testVerifyCorrectPassword() {
        String password = "Password@123";
        String hash = PasswordHasher.hashPassword(password);

        assertTrue(PasswordHasher.verifyPassword(password, hash));
    }

    @Test
    @DisplayName("Verify incorrect password returns false")
    public void testVerifyIncorrectPassword() {
        String password = "Password@123";
        String hash = PasswordHasher.hashPassword(password);

        assertFalse(PasswordHasher.verifyPassword("WrongPassword", hash));
        assertFalse(PasswordHasher.verifyPassword("", hash));
        assertFalse(PasswordHasher.verifyPassword(null, hash));
    }

    @Test
    @DisplayName("Salts ensure identical passwords generate distinct hash strings")
    public void testSaltUniqueness() {
        String password = "CommonPassword";
        String hash1 = PasswordHasher.hashPassword(password);
        String hash2 = PasswordHasher.hashPassword(password);

        assertNotEquals(hash1, hash2);
        assertTrue(PasswordHasher.verifyPassword(password, hash1));
        assertTrue(PasswordHasher.verifyPassword(password, hash2));
    }
}
