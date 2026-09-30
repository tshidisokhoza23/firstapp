package com.tpsk.tj.common.util

import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

object CryptoUtils {
    private const val RSA_ALGORITHM = "RSA/ECB/PKCS1Padding"

    fun generateKeyPair(): Pair<String, String> {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val kp = kpg.genKeyPair()
        val public = Base64.encodeToString(kp.public.encoded, Base64.DEFAULT)
        val private = Base64.encodeToString(kp.private.encoded, Base64.DEFAULT)
        return Pair(public, private)
    }

    fun encrypt(text: String, publicKeyStr: String): String {
        val publicBytes = Base64.decode(publicKeyStr, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(publicBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKey = keyFactory.generatePublic(keySpec)
        
        val cipher = Cipher.getInstance(RSA_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val bytes = cipher.doFinal(text.toByteArray())
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    fun decrypt(encryptedText: String, privateKey: PrivateKey): String {
        val cipher = Cipher.getInstance(RSA_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        val bytes = Base64.decode(encryptedText, Base64.DEFAULT)
        return String(cipher.doFinal(bytes))
    }
}
