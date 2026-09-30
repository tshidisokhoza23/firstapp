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

            Text("TJ")
                .font(.system(size: size * 0.42, weight: .black, design: .rounded))
                .foregroundColor(.white)
        }
    }
}

#Preview {
    AppLogo(size: 90)
}
