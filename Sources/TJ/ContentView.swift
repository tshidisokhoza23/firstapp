import SwiftUI

struct ContentView: View {
    var body: some View {
        AppNavigation()
    }
}

#Preview {
    ContentView()
        .environmentObject(GlobalCallState())
}
