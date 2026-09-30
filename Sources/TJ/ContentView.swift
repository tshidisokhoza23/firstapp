import SwiftUI

struct ContentView: View {
    var body: some View {
        MainRootView()
    }
}

#Preview {
    ContentView()
        .environmentObject(GlobalCallState())
}
