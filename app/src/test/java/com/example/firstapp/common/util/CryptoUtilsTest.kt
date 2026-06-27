package com.example.firstapp.common.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import android.util.Base64
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CryptoUtilsTest {

    @Test
    fun testEncryptionDecryption() {
        val pair = CryptoUtils.generateKeyPair()
        val originalText = "Hello E2EE!"
        
        val encrypted = CryptoUtils.encrypt(originalText, pair.first)
        
        val privateBytes = Base64.decode(pair.second, Base64.DEFAULT)
        val keySpec = PKCS8EncodedKeySpec(privateBytes)
        val kf = KeyFactory.getInstance("RSA")
        val privateKey = kf.generatePrivate(keySpec)
        
        val decrypted = CryptoUtils.decrypt(encrypted, privateKey)
        
        assertEquals(originalText, decrypted)
    }
}
