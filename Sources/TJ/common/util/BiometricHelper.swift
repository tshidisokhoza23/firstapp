import Foundation
import LocalAuthentication

public final class BiometricHelper {

    public static func isBiometricAvailable() -> Bool {
        let context = LAContext()
        var error: NSError?
        return context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error)
    }

    public static func biometricType() -> LABiometryType {
        let context = LAContext()
        var error: NSError?
        _ = context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error)
        return context.biometryType
    }

    public static func showBiometricPrompt(
        title: String = "Biometric Authentication",
        reason: String = "Log in using Face ID or Touch ID",
        onSuccess: @escaping () -> Void,
        onError: @escaping (_ errorCode: Int, _ errorMessage: String) -> Void,
        onFailed: @escaping () -> Void
    ) {
        let context = LAContext()
        context.localizedCancelTitle = "Cancel"

        var error: NSError?
        guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) else {
            let code = error?.code ?? LAError.biometryNotAvailable.rawValue
            let msg = error?.localizedDescription ?? "Biometrics not available"
            DispatchQueue.main.async {
                onError(code, msg)
            }
            return
        }

        context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: reason) { success, authError in
            DispatchQueue.main.async {
                if success {
                    onSuccess()
                } else if let authError = authError as? LAError {
                    if authError.code == .authenticationFailed {
                        onFailed()
                    } else {
                        onError(authError.errorCode, authError.localizedDescription)
                    }
                } else {
                    onFailed()
                }
            }
        }
    }
}
