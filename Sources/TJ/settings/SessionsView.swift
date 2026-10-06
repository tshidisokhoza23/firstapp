import SwiftUI

struct SessionsView: View {
    @Environment(\.dismiss) var dismiss
    @State private var sessions: [DeviceSession] = DeviceSession.samples
    @State private var selectedSessionToTerminate: DeviceSession? = nil
    @State private var showingTerminateAllDialog: Bool = false

    var body: some View {
        NavigationView {
            List {
                if let current = sessions.first(where: { $0.isCurrent }) {
                    Section(header: Text("Current Session")) {
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                Text(current.deviceName)
                                    .font(.headline)
                                Spacer()
                                Text("Active Now")
                                    .font(.caption)
                                    .fontWeight(.bold)
                                    .foregroundColor(.green)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(Color.green.opacity(0.15))
                                    .cornerRadius(8)
                            }
                            Text("\(current.osVersion) • \(current.appVersion)")
                                .font(.subheadline)
                                .foregroundColor(AppColors.greyMedium)
                            Text("\(current.location) (\(current.ipAddress))")
                                .font(.caption)
                                .foregroundColor(AppColors.greyMedium)
                        }
                        .padding(.vertical, 6)
                    }
                }

                if sessions.count > 1 {
                    Section {
                        Button(action: { showingTerminateAllDialog = true }) {
                            HStack {
                                Image(systemName: "xmark.shield.fill")
                                    .foregroundColor(.red)
                                Text("Terminate All Other Sessions")
                                    .foregroundColor(.red)
                                    .fontWeight(.semibold)
                            }
                        }
                    }
                }

                Section(header: Text("Active Sessions")) {
                    ForEach(sessions.filter { !$0.isCurrent }) { session in
                        Button(action: { selectedSessionToTerminate = session }) {
                            VStack(alignment: .leading, spacing: 6) {
                                HStack {
                                    Text(session.deviceName)
                                        .font(.headline)
                                        .foregroundColor(.primary)
                                    Spacer()
                                    Text(session.lastActiveText)
                                        .font(.caption)
                                        .foregroundColor(AppColors.greyMedium)
                                }
                                Text("\(session.osVersion) • \(session.appVersion)")
                                    .font(.subheadline)
                                    .foregroundColor(AppColors.greyMedium)
                                Text("\(session.location) (\(session.ipAddress))")
                                    .font(.caption)
                                    .foregroundColor(AppColors.greyMedium)
                            }
                            .padding(.vertical, 4)
                        }
                    }
                }
            }
            .navigationTitle("Sessions")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
            .confirmationDialog("Terminate Session?", isPresented: Binding(
                get: { selectedSessionToTerminate != nil },
                set: { if !$0 { selectedSessionToTerminate = nil } }
            ), titleVisibility: .visible) {
                if let target = selectedSessionToTerminate {
                    Button("Terminate \(target.deviceName)", role: .destructive) {
                        sessions.removeAll(where: { $0.id == target.id })
                        selectedSessionToTerminate = nil
                    }
                }
                Button("Cancel", role: .cancel) { selectedSessionToTerminate = nil }
            } message: {
                if let target = selectedSessionToTerminate {
                    Text("Are you sure you want to log out from \(target.deviceName) (\(target.location))?")
                }
            }
            .alert("Terminate All Other Sessions?", isPresented: $showingTerminateAllDialog) {
                Button("Terminate", role: .destructive) {
                    sessions.removeAll(where: { !$0.isCurrent })
                }
                Button("Cancel", role: .cancel) { }
            } message: {
                Text("You will be logged out of all other devices except this one.")
            }
        }
    }
}
