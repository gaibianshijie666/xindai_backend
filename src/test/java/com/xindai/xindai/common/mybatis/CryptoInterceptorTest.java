package com.xindai.xindai.common.mybatis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CryptoInterceptor encryption/decryption logic.
 */
class CryptoInterceptorTest {

    private static final String TEST_KEY = "eGluZGFpX2Rldl9hZXMyNTZfc2VjcmV0X2tleV8zMjA="; // Base64 of "xindai_dev_aes256_secret_key_320" (32 bytes)

    private CryptoInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new CryptoInterceptor(TEST_KEY);
    }

    // ---- Test entity with @CryptoField annotations ----

    static class TestUser {
        @CryptoField
        String phone;

        @CryptoField
        String idCard;

        String realName; // not annotated - should not be affected

        Integer status; // non-String field should be ignored

        TestUser() {}

        TestUser(String phone, String idCard, String realName, Integer status) {
            this.phone = phone;
            this.idCard = idCard;
            this.realName = realName;
            this.status = status;
        }
    }

    @Test
    @DisplayName("Encrypt and decrypt round-trip for phone number")
    void testEncryptDecryptPhone() {
        TestUser user = new TestUser("13800138000", null, "Zhang San", 1);

        // Encrypt
        interceptor.encryptParameters(user);
        assertNotEquals("13800138000", user.phone);
        assertTrue(user.phone.length() > "13800138000".length(), "Encrypted value should be longer (Base64)");
        assertEquals("Zhang San", user.realName, "Non-annotated field should not be affected");
        assertNull(user.idCard, "Null annotated field should remain null");

        // Decrypt
        interceptor.decryptResults(user);
        assertEquals("13800138000", user.phone, "Decrypted phone should match original");
        assertEquals("Zhang San", user.realName, "Non-annotated field should still not be affected");
        assertNull(user.idCard);
    }

    @Test
    @DisplayName("Encrypt and decrypt round-trip for ID card number")
    void testEncryptDecryptIdCard() {
        TestUser user = new TestUser(null, "110101199003076789", "Li Si", 0);

        interceptor.encryptParameters(user);
        assertNull(user.phone);
        assertNotEquals("110101199003076789", user.idCard);
        assertTrue(user.idCard.length() > "110101199003076789".length());
        assertEquals("Li Si", user.realName);

        interceptor.decryptResults(user);
        assertNull(user.phone);
        assertEquals("110101199003076789", user.idCard);
        assertEquals("Li Si", user.realName);
    }

    @Test
    @DisplayName("Null values are skipped gracefully")
    void testNullValueHandling() {
        TestUser user = new TestUser(null, null, null, null);

        assertDoesNotThrow(() -> interceptor.encryptParameters(user));
        assertDoesNotThrow(() -> interceptor.decryptResults(user));

        assertNull(user.phone);
        assertNull(user.idCard);
        assertNull(user.realName);
        assertNull(user.status);
    }

    @Test
    @DisplayName("Non-annotated fields are never affected")
    void testNonAnnotatedFieldsUnaffected() {
        TestUser user = new TestUser("13800138000", "110101199003076789", "Wang Wu", 1);

        interceptor.encryptParameters(user);
        assertEquals("Wang Wu", user.realName, "realName should not be encrypted");
        assertEquals(1, user.status, "status should not be encrypted");

        interceptor.decryptResults(user);
        assertEquals("Wang Wu", user.realName);
        assertEquals(1, user.status);
    }

    @Test
    @DisplayName("Encrypted value is different from plaintext")
    void testEncryptedValueDiffers() {
        TestUser user = new TestUser("13800138000", "110101199003076789", "Test", 1);

        interceptor.encryptParameters(user);
        assertNotEquals("13800138000", user.phone);
        assertNotEquals("110101199003076789", user.idCard);
    }

    @Test
    @DisplayName("Same plaintext produces same ciphertext (AES ECB mode is deterministic)")
    void testDeterministicEncryption() {
        TestUser user1 = new TestUser("13800138000", null, null, null);
        TestUser user2 = new TestUser("13800138000", null, null, null);

        interceptor.encryptParameters(user1);
        interceptor.encryptParameters(user2);

        // Hutool AES with default ECB mode is deterministic
        assertEquals(user1.phone, user2.phone, "AES ECB mode produces deterministic ciphertext");

        // Both decrypt to the same plaintext
        interceptor.decryptResults(user1);
        interceptor.decryptResults(user2);
        assertEquals(user1.phone, user2.phone);
        assertEquals("13800138000", user1.phone);
    }

    @Test
    @DisplayName("Empty string is handled gracefully (skipped)")
    void testEmptyString() {
        TestUser user = new TestUser("", "", "Test", 1);

        interceptor.encryptParameters(user);
        // Empty strings should be skipped (length > 0 check in processFields)
        assertEquals("", user.phone);
        assertEquals("", user.idCard);
    }

    @Test
    @DisplayName("Constructor rejects invalid key length")
    void testInvalidKeyLength() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[8]);
        assertThrows(IllegalArgumentException.class, () -> new CryptoInterceptor(shortKey));
    }

    @Test
    @DisplayName("16-byte key is accepted (AES-128)")
    void testAes128KeyAccepted() {
        String key128 = Base64.getEncoder().encodeToString(new byte[16]);
        assertDoesNotThrow(() -> new CryptoInterceptor(key128));
    }

    @Test
    @DisplayName("24-byte key is accepted (AES-192)")
    void testAes192KeyAccepted() {
        String key192 = Base64.getEncoder().encodeToString(new byte[24]);
        assertDoesNotThrow(() -> new CryptoInterceptor(key192));
    }

    @Test
    @DisplayName("32-byte key is accepted (AES-256)")
    void testAes256KeyAccepted() {
        String key256 = Base64.getEncoder().encodeToString(new byte[32]);
        assertDoesNotThrow(() -> new CryptoInterceptor(key256));
    }

    @Test
    @DisplayName("Encrypt parameters wrapped in Map (MyBatis-Plus style)")
    void testEncryptParametersInMap() {
        TestUser user = new TestUser("13800138000", "110101199003076789", "Test", 1);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("et", user);

        interceptor.encryptParameters(paramMap);

        assertNotEquals("13800138000", user.phone);
        assertNotEquals("110101199003076789", user.idCard);
        assertEquals("Test", user.realName);

        interceptor.decryptResults(user);

        assertEquals("13800138000", user.phone);
        assertEquals("110101199003076789", user.idCard);
    }

    @Test
    @DisplayName("Decrypt results in a collection")
    void testDecryptResultsInCollection() {
        TestUser user1 = new TestUser("13800138000", null, null, null);
        TestUser user2 = new TestUser("13900139000", null, null, null);

        interceptor.encryptParameters(user1);
        interceptor.encryptParameters(user2);

        List<TestUser> users = List.of(user1, user2);
        interceptor.decryptResults(users);

        assertEquals("13800138000", user1.phone);
        assertEquals("13900139000", user2.phone);
    }

    @Test
    @DisplayName("Decrypt non-encrypted value returns original value (graceful)")
    void testDecryptNonEncryptedValue() {
        // If a value is not encrypted (e.g., legacy data), decryptValue should return it as-is
        String result = interceptor.decryptValue("plaintext-value");
        assertEquals("plaintext-value", result, "Non-encrypted value should be returned unchanged");
    }
}
