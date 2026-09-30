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

    static let appBackgroundDarkTop = Color(hex: "0F172A")
    static let appBackgroundDarkBottom = Color(hex: "1E293B")
    static let appBackgroundLight = Color(hex: "F8FAFC")
    static let logoBlueDark = Color(hex: "1A237E")
    static let boyishPurple = Color(hex: "7C4DFF")
    static let appAccent = Color(hex: "00D1FF")
    static let appPrimary = Color(hex: "1A237E")
    static let appSecondary = Color(hex: "00D1FF")
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
