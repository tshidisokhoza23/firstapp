import SwiftUI

struct AppColors {
    // Unified Brand Palette
    static let brandPrimary = Color(red: 0.0, green: 0.23, blue: 0.63) // #003BA1 Logo Blue Dark
    static let brandAccent = Color(red: 0.0, green: 0.82, blue: 1.0)    // #00D1FF Logo Blue Light

    // Base Background Gradients (Dark and Light)
    static let darkBackgroundTop = Color(red: 0.016, green: 0.082, blue: 0.157) // #041528
    static let darkBackgroundBottom = Color.black                              // #000000
    static let lightBackground = Color(red: 0.97, green: 0.98, blue: 0.98)      // #F8F9FA

    // Neutrals
    static let greyLight = Color(red: 0.94, green: 0.95, blue: 0.96)           // #F0F2F5
    static let greyMedium = Color(red: 0.56, green: 0.56, blue: 0.58)          // #8E8E93
    static let greyDark = Color(red: 0.11, green: 0.11, blue: 0.12)            // #1C1C1E
    static let surfaceColor = Color(red: 0.086, green: 0.094, blue: 0.11)       // #16181C
    static let dividerColor = Color(red: 0.18, green: 0.20, blue: 0.21)        // #2F3336

    static func backgroundLinearGradient(isDark: Bool) -> LinearGradient {
        if isDark {
            return LinearGradient(
                colors: [darkBackgroundTop, darkBackgroundBottom],
                startPoint: .top,
                endPoint: .bottom
            )
        } else {
            return LinearGradient(
                colors: [Color.white, lightBackground],
                startPoint: .top,
                endPoint: .bottom
            )
        }
    }
}
