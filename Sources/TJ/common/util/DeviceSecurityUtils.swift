import Foundation
import UIKit

public final class DeviceSecurityUtils {

    public static func isDeviceJailbroken() -> Bool {
        #if targetEnvironment(simulator)
        return false
        #else
        // 1. Check common jailbreak files & binaries
        let jailbreakPaths = [
            "/Applications/Cydia.app",
            "/Library/MobileSubstrate/MobileSubstrate.dylib",
            "/bin/bash",
            "/usr/sbin/sshd",
            "/etc/apt",
            "/private/var/lib/apt",
            "/usr/bin/ssh",
            "/Applications/Sileo.app",
            "/var/binpack"
        ]

        for path in jailbreakPaths {
            if FileManager.default.fileExists(atPath: path) {
                return true
            }
        }

        // 2. Test sandbox violation (ability to write outside application sandbox)
        let testString = "JailbreakTest"
        let testFilePath = "/private/jailbreak_test_\(UUID().uuidString).txt"
        do {
            try testString.write(toFile: testFilePath, atomically: true, encoding: .utf8)
            try? FileManager.default.removeItem(atPath: testFilePath)
            return true
        } catch {
            // Write prohibited = restricted sandbox = secure non-jailbroken device
        }

        // 3. Test opening Cydia URL scheme
        if let cydiaURL = URL(string: "cydia://package/com.example.package") {
            if UIApplication.shared.canOpenURL(cydiaURL) {
                return true
            }
        }

        return false
        #endif
    }
}
