import SwiftUI

public extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }

    static let appPrimary = Color(red: 0.00, green: 0.27, blue: 0.74) // #0045BC
    static let appAccent = Color(red: 0.00, green: 0.27, blue: 0.74) // #0045BC
    static let appSecondary = Color(red: 0.10, green: 0.46, blue: 0.82)
    static let appBackgroundLight = Color(red: 0.96, green: 0.96, blue: 0.97)
    static let appBackgroundDarkTop = Color(red: 0.07, green: 0.07, blue: 0.07)
    static let appBackgroundDarkBottom = Color(red: 0.12, green: 0.12, blue: 0.12)
    static let appBottomNavBg = Color(red: 0.92, green: 0.95, blue: 0.98) // #EBF1FB
    static let appBottomNavPill = Color(red: 0.82, green: 0.88, blue: 0.98) // #D0E1FB
    static let logoBlueDark = Color(red: 0.00, green: 0.27, blue: 0.74)
    static let boyishPurple = Color(hex: "7C4DFF")
}

public struct AppColors {
    public static let primary = Color.appPrimary
    public static let secondary = Color.appSecondary
    public static let accent = Color.appAccent
    public static let backgroundDarkTop = Color.appBackgroundDarkTop
    public static let backgroundDarkBottom = Color.appBackgroundDarkBottom
    public static let backgroundLight = Color.appBackgroundLight
    public static let logoBlueDark = Color.logoBlueDark
    public static let boyishPurple = Color.boyishPurple
}
