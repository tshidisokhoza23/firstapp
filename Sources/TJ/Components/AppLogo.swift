import SwiftUI

public struct AppLogo: View {
    public var size: CGFloat

    public init(size: CGFloat = 90) {
        self.size = size
    }

    public var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: size * 0.23, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [Color.logoBlueDark, Color.appAccent],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: size, height: size)
                .shadow(color: Color.appAccent.opacity(0.3), radius: size * 0.08, x: 0, y: size * 0.04)

            Text("TJ")
                .font(.system(size: size * 0.42, weight: .black, design: .rounded))
                .foregroundColor(.white)
        }
    }
}

public struct AppLogoWithText: View {
    public var size: CGFloat

    public init(size: CGFloat = 32) {
        self.size = size
    }

    public var body: some View {
        HStack(spacing: 10) {
            AppLogo(size: size)
            Text("TJ")
                .font(.system(size: size * 0.65, weight: .heavy, design: .rounded))
                .foregroundColor(.primary)
        }
    }
}

#Preview {
    AppLogo(size: 90)
}
