import Foundation
import Security

public enum CryptoUtils {

    public static func generateKeyPair() -> (publicKey: String, privateKey: String)? {
        let attributes: [String: Any] = [
            kSecAttrKeyType as String: kSecAttrKeyTypeRSA,
            kSecAttrKeySizeInBits as String: 2048,
            kSecPrivateKeyAttrs as String: [
                kSecAttrIsPermanent as String: false
            ]
        ]

        var error: Unmanaged<CFError>?
        guard let privateKey = SecKeyCreateRandomKey(attributes as CFDictionary, &error),
              let publicKey = SecKeyCopyPublicKey(privateKey) else {
            return nil
        }

        guard let privateKeyData = SecKeyCopyExternalRepresentation(privateKey, &error) as Data?,
              let publicKeyData = SecKeyCopyExternalRepresentation(publicKey, &error) as Data? else {
            return nil
        }

        return (
            publicKey: publicKeyData.base64EncodedString(),
            privateKey: privateKeyData.base64EncodedString()
        )
    }

    public static func encrypt(text: String, publicKeyBase64: String) -> String? {
        guard let publicData = Data(base64Encoded: publicKeyBase64),
              let textData = text.data(using: .utf8) else {
            return nil
        }

        let attributes: [String: Any] = [
            kSecAttrKeyType as String: kSecAttrKeyTypeRSA,
            kSecAttrKeyClass as String: kSecAttrKeyClassPublic,
            kSecAttrKeySizeInBits as String: 2048
        ]

        var error: Unmanaged<CFError>?
        guard let publicKey = SecKeyCreateWithData(publicData as CFData, attributes as CFDictionary, &error) else {
            return nil
        }

        let algorithm: SecKeyAlgorithm = .rsaEncryptionPKCS1

        guard SecKeyIsAlgorithmSupported(publicKey, .encrypt, algorithm) else {
            return nil
        }

        guard let encryptedData = SecKeyCreateEncryptedData(publicKey, algorithm, textData as CFData, &error) as Data? else {
            return nil
        }

        return encryptedData.base64EncodedString()
    }

    public static func decrypt(encryptedBase64: String, privateKeyBase64: String) -> String? {
        guard let privateData = Data(base64Encoded: privateKeyBase64),
              let encryptedData = Data(base64Encoded: encryptedBase64) else {
            return nil
        }

        let attributes: [String: Any] = [
            kSecAttrKeyType as String: kSecAttrKeyTypeRSA,
            kSecAttrKeyClass as String: kSecAttrKeyClassPrivate,
            kSecAttrKeySizeInBits as String: 2048
        ]

        var error: Unmanaged<CFError>?
        guard let privateKey = SecKeyCreateWithData(privateData as CFData, attributes as CFDictionary, &error) else {
            return nil
        }

        let algorithm: SecKeyAlgorithm = .rsaEncryptionPKCS1

        guard SecKeyIsAlgorithmSupported(privateKey, .decrypt, algorithm) else {
            return nil
        }

        guard let decryptedData = SecKeyCreateDecryptedData(privateKey, algorithm, encryptedData as CFData, &error) as Data? else {
            return nil
        }

        return String(data: decryptedData, encoding: .utf8)
    }
}
